package com.kinetix.risk.routes

import com.kinetix.risk.model.FactorReturn
import com.kinetix.risk.persistence.DatabaseTestSetup
import com.kinetix.risk.persistence.ExposedFactorReturnRepository
import com.kinetix.risk.persistence.FactorReturnsTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDate

class FactorReturnRoutesAcceptanceTest : FunSpec({

    val db = DatabaseTestSetup.startAndMigrate()
    val repository = ExposedFactorReturnRepository(db)

    beforeEach {
        newSuspendedTransaction(db = db) { FactorReturnsTable.deleteAll() }
    }

    fun testApp(block: suspend ApplicationTestBuilder.() -> Unit) {
        testApplication {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            routing {
                factorReturnRoutes(repository)
            }
            block()
        }
    }

    val sampleReturn = FactorReturn(
        factorName = "EQUITY_BETA",
        asOfDate = LocalDate.of(2026, 3, 24),
        returnValue = 0.012,
        source = "market_data",
    )

    test("GET /api/v1/factor-returns/{name}/{date} returns the factor return") {
        repository.save(sampleReturn)

        testApp {
            val response = client.get("/api/v1/factor-returns/EQUITY_BETA/2026-03-24")

            response.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["factorName"]?.jsonPrimitive?.content shouldBe "EQUITY_BETA"
            body["asOfDate"]?.jsonPrimitive?.content shouldBe "2026-03-24"
            body["returnValue"]?.jsonPrimitive?.double shouldBe 0.012
            body["source"]?.jsonPrimitive?.content shouldBe "market_data"
        }
    }

    test("GET /api/v1/factor-returns/{name}/{date} returns 404 when not found") {
        testApp {
            val response = client.get("/api/v1/factor-returns/EQUITY_BETA/2026-03-24")

            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    test("GET /api/v1/factor-returns/{name}/{date} returns 400 for invalid date format") {
        testApp {
            val response = client.get("/api/v1/factor-returns/EQUITY_BETA/not-a-date")

            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    test("GET /api/v1/factor-returns/{name} with date range returns ordered results") {
        val returns = listOf(
            FactorReturn("EQUITY_BETA", LocalDate.of(2026, 3, 22), 0.010, "feed"),
            FactorReturn("EQUITY_BETA", LocalDate.of(2026, 3, 23), 0.011, "feed"),
            FactorReturn("EQUITY_BETA", LocalDate.of(2026, 3, 24), 0.012, "feed"),
        )
        repository.saveBatch(returns)

        testApp {
            val response = client.get("/api/v1/factor-returns/EQUITY_BETA?from=2026-03-22&to=2026-03-24")

            response.status shouldBe HttpStatusCode.OK
            val arr = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            arr.size shouldBe 3
            arr[0].jsonObject["asOfDate"]?.jsonPrimitive?.content shouldBe "2026-03-22"
            arr[2].jsonObject["returnValue"]?.jsonPrimitive?.double shouldBe 0.012
        }
    }

    test("GET /api/v1/factor-returns/{name} returns 400 when from or to is missing") {
        testApp {
            val response = client.get("/api/v1/factor-returns/EQUITY_BETA?from=2026-03-22")

            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    test("PUT /api/v1/factor-returns upserts a factor return") {
        testApp {
            val response = client.put("/api/v1/factor-returns") {
                contentType(ContentType.Application.Json)
                setBody("""{"factorName":"EQUITY_BETA","asOfDate":"2026-03-24","returnValue":0.012,"source":"manual"}""")
            }

            response.status shouldBe HttpStatusCode.NoContent
            val saved = repository.findByFactorAndDate("EQUITY_BETA", LocalDate.of(2026, 3, 24))
            saved shouldBe FactorReturn(
                factorName = "EQUITY_BETA",
                asOfDate = LocalDate.of(2026, 3, 24),
                returnValue = 0.012,
                source = "manual",
            )
        }
    }
})
