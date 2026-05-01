package com.kinetix.referencedata.routes

import com.kinetix.common.model.CreditSpread
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

class DivisionRoutesAcceptanceTest : FunSpec({

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

    beforeEach {
        newSuspendedTransaction(db = db) {
            exec("TRUNCATE TABLE desks, divisions RESTART IDENTITY CASCADE")
        }
    }

    test("GET /api/v1/divisions returns list of divisions") {
        divisionRepo.save(Division(id = DivisionId("equities"), name = "Equities"))
        divisionRepo.save(Division(id = DivisionId("fixed-income"), name = "Fixed Income & Rates"))

        testApplication {
            application { module(dividendYieldRepo, creditSpreadRepo, ingestionService, divisionService = divisionService, deskService = deskService) }

            val response = client.get("/api/v1/divisions")
            response.status shouldBe HttpStatusCode.OK

            val body: JsonArray = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            body.size shouldBe 2
            body[0].jsonObject["id"]?.jsonPrimitive?.content shouldBe "equities"
            body[0].jsonObject["name"]?.jsonPrimitive?.content shouldBe "Equities"
            body[0].jsonObject["deskCount"]?.jsonPrimitive?.content?.toInt() shouldBe 0
        }
    }

    test("GET /api/v1/divisions/{id} returns division by ID") {
        divisionRepo.save(Division(id = DivisionId("equities"), name = "Equities", description = "Equity trading desks"))

        testApplication {
            application { module(dividendYieldRepo, creditSpreadRepo, ingestionService, divisionService = divisionService, deskService = deskService) }

            val response = client.get("/api/v1/divisions/equities")
            response.status shouldBe HttpStatusCode.OK

            val body: JsonObject = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["id"]?.jsonPrimitive?.content shouldBe "equities"
            body["name"]?.jsonPrimitive?.content shouldBe "Equities"
            body["description"]?.jsonPrimitive?.content shouldBe "Equity trading desks"
            body["deskCount"]?.jsonPrimitive?.content?.toInt() shouldBe 0
        }
    }

    test("GET /api/v1/divisions/{id} returns 404 when not found") {
        testApplication {
            application { module(dividendYieldRepo, creditSpreadRepo, ingestionService, divisionService = divisionService, deskService = deskService) }

            val response = client.get("/api/v1/divisions/unknown")
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    test("POST /api/v1/divisions creates a division and returns 201") {
        testApplication {
            application { module(dividendYieldRepo, creditSpreadRepo, ingestionService, divisionService = divisionService, deskService = deskService) }

            val response = client.post("/api/v1/divisions") {
                contentType(ContentType.Application.Json)
                setBody("""{"id":"equities","name":"Equities","description":"Equity trading desks"}""")
            }
            response.status shouldBe HttpStatusCode.Created

            val body: JsonObject = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["id"]?.jsonPrimitive?.content shouldBe "equities"
            body["name"]?.jsonPrimitive?.content shouldBe "Equities"
        }
    }

    test("POST /api/v1/divisions returns 400 when name already exists") {
        divisionRepo.save(Division(id = DivisionId("equities-2"), name = "Equities"))

        testApplication {
            application { module(dividendYieldRepo, creditSpreadRepo, ingestionService, divisionService = divisionService, deskService = deskService) }

            val response = client.post("/api/v1/divisions") {
                contentType(ContentType.Application.Json)
                setBody("""{"id":"equities-new","name":"Equities"}""")
            }
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    test("GET /api/v1/divisions/{divisionId}/desks returns desks in division") {
        divisionRepo.save(Division(id = DivisionId("equities"), name = "Equities"))

        testApplication {
            application { module(dividendYieldRepo, creditSpreadRepo, ingestionService, divisionService = divisionService, deskService = deskService) }

            val response = client.get("/api/v1/divisions/equities/desks")
            response.status shouldBe HttpStatusCode.OK

            val body: JsonArray = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            body.size shouldBe 0
        }
    }
})
