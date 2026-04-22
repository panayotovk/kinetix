package com.kinetix.position

import com.kinetix.position.persistence.DatabaseTestSetup
import com.kinetix.position.persistence.ExposedLimitDefinitionRepository
import com.kinetix.position.persistence.ExposedTemporaryLimitIncreaseRepository
import com.kinetix.position.routes.LimitDefinitionResponse
import com.kinetix.position.routes.TemporaryIncreaseResponse
import com.kinetix.position.routes.limitRoutes
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

private fun Application.configureTestApp(
    limitRepo: ExposedLimitDefinitionRepository,
    temporaryIncreaseRepo: ExposedTemporaryLimitIncreaseRepository,
) {
    install(ContentNegotiation) { json() }
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "invalid_request", "message" to (cause.message ?: "Invalid request")),
            )
        }
    }
    routing {
        limitRoutes(limitRepo, temporaryIncreaseRepo)
    }
}

class LimitRoutesAcceptanceTest : FunSpec({

    val db = DatabaseTestSetup.startAndMigrate()
    val limitRepo = ExposedLimitDefinitionRepository(db)
    val temporaryIncreaseRepo = ExposedTemporaryLimitIncreaseRepository(db)

    beforeEach {
        newSuspendedTransaction(db = db) {
            exec("TRUNCATE TABLE limit_temporary_increases, limit_definitions RESTART IDENTITY CASCADE")
        }
    }

    test("POST /api/v1/limits creates a FIRM-level limit and returns 201 with the persisted definition") {
        testApplication {
            application { configureTestApp(limitRepo, temporaryIncreaseRepo) }

            val response = client.post("/api/v1/limits") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "level": "FIRM",
                        "entityId": "KINETIX",
                        "limitType": "NOTIONAL",
                        "limitValue": "1000000000",
                        "intradayLimit": "500000000",
                        "overnightLimit": "1000000000"
                    }
                    """.trimIndent()
                )
            }

            response.status shouldBe HttpStatusCode.Created
            val body = Json.decodeFromString<LimitDefinitionResponse>(response.bodyAsText())
            body.id shouldNotBe ""
            body.level shouldBe "FIRM"
            body.entityId shouldBe "KINETIX"
            body.limitType shouldBe "NOTIONAL"
            body.limitValue shouldBe "1000000000"
            body.intradayLimit shouldBe "500000000"
            body.overnightLimit shouldBe "1000000000"
            body.active shouldBe true

            // Verify persisted
            val persisted = limitRepo.findById(body.id)
            persisted shouldNotBe null
            persisted!!.entityId shouldBe "KINETIX"
        }
    }

    test("POST /api/v1/limits returns 400 when level is not a known enum value") {
        testApplication {
            application { configureTestApp(limitRepo, temporaryIncreaseRepo) }

            val response = client.post("/api/v1/limits") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "level": "GALAXY",
                        "entityId": "KINETIX",
                        "limitType": "NOTIONAL",
                        "limitValue": "1000000000"
                    }
                    """.trimIndent()
                )
            }

            response.status shouldBe HttpStatusCode.BadRequest
            response.bodyAsText() shouldContain "GALAXY"
        }
    }

    test("POST /api/v1/limits returns 400 when limitValue cannot be parsed as a number") {
        testApplication {
            application { configureTestApp(limitRepo, temporaryIncreaseRepo) }

            val response = client.post("/api/v1/limits") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "level": "DESK",
                        "entityId": "EQ-DESK",
                        "limitType": "NOTIONAL",
                        "limitValue": "abc"
                    }
                    """.trimIndent()
                )
            }

            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    test("GET /api/v1/limits returns all persisted limit definitions") {
        testApplication {
            application { configureTestApp(limitRepo, temporaryIncreaseRepo) }

            client.post("/api/v1/limits") {
                contentType(ContentType.Application.Json)
                setBody("""{"level":"FIRM","entityId":"KINETIX","limitType":"NOTIONAL","limitValue":"1000000000"}""")
            }
            client.post("/api/v1/limits") {
                contentType(ContentType.Application.Json)
                setBody("""{"level":"DESK","entityId":"EQ-DESK","limitType":"POSITION","limitValue":"500000"}""")
            }

            val response = client.get("/api/v1/limits")

            response.status shouldBe HttpStatusCode.OK
            val body = Json.decodeFromString<List<LimitDefinitionResponse>>(response.bodyAsText())
            body.size shouldBe 2
            body.map { it.entityId }.toSet() shouldBe setOf("KINETIX", "EQ-DESK")
        }
    }

    test("PUT /api/v1/limits/{id} returns 404 when the limit does not exist") {
        testApplication {
            application { configureTestApp(limitRepo, temporaryIncreaseRepo) }

            val response = client.put("/api/v1/limits/nonexistent-id") {
                contentType(ContentType.Application.Json)
                setBody("""{"limitValue":"2000000"}""")
            }

            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    test("PUT /api/v1/limits/{id} updates an existing limit and returns the new state") {
        testApplication {
            application { configureTestApp(limitRepo, temporaryIncreaseRepo) }

            val createResponse = client.post("/api/v1/limits") {
                contentType(ContentType.Application.Json)
                setBody("""{"level":"FIRM","entityId":"KINETIX","limitType":"NOTIONAL","limitValue":"1000000000"}""")
            }
            val createdId = Json.decodeFromString<LimitDefinitionResponse>(createResponse.bodyAsText()).id

            val updateResponse = client.put("/api/v1/limits/$createdId") {
                contentType(ContentType.Application.Json)
                setBody("""{"limitValue":"2000000000","active":false}""")
            }

            updateResponse.status shouldBe HttpStatusCode.OK
            val updated = Json.decodeFromString<LimitDefinitionResponse>(updateResponse.bodyAsText())
            updated.limitValue shouldBe "2000000000"
            updated.active shouldBe false
        }
    }

    test("POST /api/v1/limits/{id}/temporary-increase persists an increase with a retrievable expiry") {
        testApplication {
            application { configureTestApp(limitRepo, temporaryIncreaseRepo) }

            val createResponse = client.post("/api/v1/limits") {
                contentType(ContentType.Application.Json)
                setBody("""{"level":"DESK","entityId":"EQ-DESK","limitType":"NOTIONAL","limitValue":"500000"}""")
            }
            val limitId = Json.decodeFromString<LimitDefinitionResponse>(createResponse.bodyAsText()).id

            val expiry = "2030-01-01T00:00:00Z"
            val response = client.post("/api/v1/limits/$limitId/temporary-increase") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "newValue": "750000",
                        "approvedBy": "risk-officer-1",
                        "expiresAt": "$expiry",
                        "reason": "EOM rebalancing"
                    }
                    """.trimIndent()
                )
            }

            response.status shouldBe HttpStatusCode.Created
            val body = Json.decodeFromString<TemporaryIncreaseResponse>(response.bodyAsText())
            body.limitId shouldBe limitId
            body.newValue shouldBe "750000"
            body.approvedBy shouldBe "risk-officer-1"
            body.reason shouldBe "EOM rebalancing"
            body.expiresAt shouldContain "2030-01-01"
        }
    }

    test("POST /api/v1/limits/{id}/temporary-increase returns 404 for an unknown limit id") {
        testApplication {
            application { configureTestApp(limitRepo, temporaryIncreaseRepo) }

            val response = client.post("/api/v1/limits/nonexistent-id/temporary-increase") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "newValue": "750000",
                        "approvedBy": "risk-officer-1",
                        "expiresAt": "2030-01-01T00:00:00Z",
                        "reason": "EOM rebalancing"
                    }
                    """.trimIndent()
                )
            }

            response.status shouldBe HttpStatusCode.NotFound
        }
    }
})
