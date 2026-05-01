package com.kinetix.referencedata.routes

import com.kinetix.common.model.CreditSpread
import com.kinetix.common.model.Desk
import com.kinetix.common.model.DeskId
import com.kinetix.common.model.Division
import com.kinetix.common.model.DivisionId
import com.kinetix.common.model.DividendYield
import com.kinetix.common.model.InstrumentId
import com.kinetix.referencedata.cache.ReferenceDataCache
import com.kinetix.referencedata.kafka.ReferenceDataPublisher
import com.kinetix.referencedata.module
import com.kinetix.referencedata.persistence.DatabaseTestSetup
import com.kinetix.referencedata.persistence.ExposedCreditSpreadRepository
import com.kinetix.referencedata.persistence.ExposedDeskRepository
import com.kinetix.referencedata.persistence.ExposedDividendYieldRepository
import com.kinetix.referencedata.persistence.ExposedDivisionRepository
import com.kinetix.referencedata.service.DeskService
import com.kinetix.referencedata.service.DivisionService
import com.kinetix.referencedata.service.ReferenceDataIngestionService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class DeskRoutesAcceptanceTest : FunSpec({

    val db = DatabaseTestSetup.startAndMigrate()
    val dividendYieldRepo = ExposedDividendYieldRepository(db)
    val creditSpreadRepo = ExposedCreditSpreadRepository(db)
    val noOpCache = object : ReferenceDataCache {
        override suspend fun putDividendYield(dividendYield: DividendYield) = Unit
        override suspend fun getDividendYield(instrumentId: InstrumentId): DividendYield? = null
        override suspend fun putCreditSpread(creditSpread: CreditSpread) = Unit
        override suspend fun getCreditSpread(instrumentId: InstrumentId): CreditSpread? = null
    }
    val noOpPublisher = object : ReferenceDataPublisher {
        override suspend fun publishDividendYield(dividendYield: DividendYield) = Unit
        override suspend fun publishCreditSpread(creditSpread: CreditSpread) = Unit
    }
    val ingestionService = ReferenceDataIngestionService(
        dividendYieldRepo, creditSpreadRepo, noOpCache, noOpPublisher,
    )
    val divisionRepo = ExposedDivisionRepository(db)
    val deskRepo = ExposedDeskRepository(db)
    val divisionService = DivisionService(divisionRepo)
    val deskService = DeskService(deskRepo, divisionRepo)

    val equitiesDivision = Division(id = DivisionId("equities"), name = "Equities")

    beforeEach {
        newSuspendedTransaction(db = db) {
            exec("TRUNCATE TABLE desks, divisions RESTART IDENTITY CASCADE")
        }
    }

    test("GET /api/v1/desks returns list of desks") {
        divisionRepo.save(equitiesDivision)
        deskRepo.save(Desk(id = DeskId("equity-growth"), name = "Equity Growth", divisionId = DivisionId("equities")))
        deskRepo.save(Desk(id = DeskId("tech-momentum"), name = "Tech Momentum", divisionId = DivisionId("equities"), deskHead = "Alice"))

        testApplication {
            application { module(dividendYieldRepo, creditSpreadRepo, ingestionService, divisionService = divisionService, deskService = deskService) }

            val response = client.get("/api/v1/desks")
            response.status shouldBe HttpStatusCode.OK

            val body: JsonArray = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            body.size shouldBe 2
            body[0].jsonObject["id"]?.jsonPrimitive?.content shouldBe "equity-growth"
            body[0].jsonObject["divisionId"]?.jsonPrimitive?.content shouldBe "equities"
            body[0].jsonObject["bookCount"]?.jsonPrimitive?.content?.toInt() shouldBe 0
        }
    }

    test("GET /api/v1/desks/{id} returns desk by ID") {
        divisionRepo.save(equitiesDivision)
        deskRepo.save(Desk(
            id = DeskId("equity-growth"),
            name = "Equity Growth",
            divisionId = DivisionId("equities"),
            deskHead = "Alice",
            description = "Growth equity strategies",
        ))

        testApplication {
            application { module(dividendYieldRepo, creditSpreadRepo, ingestionService, divisionService = divisionService, deskService = deskService) }

            val response = client.get("/api/v1/desks/equity-growth")
            response.status shouldBe HttpStatusCode.OK

            val body: JsonObject = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["id"]?.jsonPrimitive?.content shouldBe "equity-growth"
            body["name"]?.jsonPrimitive?.content shouldBe "Equity Growth"
            body["divisionId"]?.jsonPrimitive?.content shouldBe "equities"
            body["deskHead"]?.jsonPrimitive?.content shouldBe "Alice"
            body["description"]?.jsonPrimitive?.content shouldBe "Growth equity strategies"
        }
    }

    test("GET /api/v1/desks/{id} returns 404 when not found") {
        testApplication {
            application { module(dividendYieldRepo, creditSpreadRepo, ingestionService, divisionService = divisionService, deskService = deskService) }

            val response = client.get("/api/v1/desks/unknown")
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    test("POST /api/v1/desks creates a desk and returns 201") {
        divisionRepo.save(equitiesDivision)

        testApplication {
            application { module(dividendYieldRepo, creditSpreadRepo, ingestionService, divisionService = divisionService, deskService = deskService) }

            val response = client.post("/api/v1/desks") {
                contentType(ContentType.Application.Json)
                setBody("""{"id":"equity-growth","name":"Equity Growth","divisionId":"equities","deskHead":"Alice"}""")
            }
            response.status shouldBe HttpStatusCode.Created

            val body: JsonObject = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["id"]?.jsonPrimitive?.content shouldBe "equity-growth"
            body["name"]?.jsonPrimitive?.content shouldBe "Equity Growth"
            body["deskHead"]?.jsonPrimitive?.content shouldBe "Alice"
        }
    }

    test("POST /api/v1/desks returns 400 when division does not exist") {
        testApplication {
            application { module(dividendYieldRepo, creditSpreadRepo, ingestionService, divisionService = divisionService, deskService = deskService) }

            val response = client.post("/api/v1/desks") {
                contentType(ContentType.Application.Json)
                setBody("""{"id":"desk-1","name":"Desk One","divisionId":"nonexistent"}""")
            }
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    test("POST /api/v1/desks returns 400 when desk name already exists in same division") {
        divisionRepo.save(equitiesDivision)
        deskRepo.save(Desk(id = DeskId("equity-growth"), name = "Equity Growth", divisionId = DivisionId("equities")))

        testApplication {
            application { module(dividendYieldRepo, creditSpreadRepo, ingestionService, divisionService = divisionService, deskService = deskService) }

            val response = client.post("/api/v1/desks") {
                contentType(ContentType.Application.Json)
                setBody("""{"id":"equity-growth-2","name":"Equity Growth","divisionId":"equities"}""")
            }
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }
})
