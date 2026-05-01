package com.kinetix.regulatory.stress

import com.kinetix.regulatory.client.RiskOrchestratorClient
import com.kinetix.regulatory.module
import com.kinetix.regulatory.persistence.DatabaseTestSetup
import com.kinetix.regulatory.persistence.ExposedFrtbCalculationRepository
import com.kinetix.regulatory.persistence.ExposedStressScenarioRepository
import com.kinetix.regulatory.persistence.StressScenariosTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.mockk
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class StressScenarioGovernanceAcceptanceTest : FunSpec({

    val db = DatabaseTestSetup.startAndMigrate()
    val frtbRepo = ExposedFrtbCalculationRepository(db)
    val riskClient = mockk<RiskOrchestratorClient>()
    val stressScenarioRepo = ExposedStressScenarioRepository(db)

    beforeEach {
        newSuspendedTransaction(db = db) { StressScenariosTable.deleteAll() }
    }

    test("a new scenario — POST /api/v1/stress-scenarios — returns 201 with DRAFT status") {
        testApplication {
            application {
                module(frtbRepo, riskClient, stressScenarioRepository = stressScenarioRepo)
            }
            val response = client.post("/api/v1/stress-scenarios") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "name": "Equity Crash",
                        "description": "Global equity -30%",
                        "shocks": "{\"EQ\":-0.30}",
                        "createdBy": "analyst@kinetix.com"
                    }
                """.trimIndent())
            }
            response.status shouldBe HttpStatusCode.Created
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["status"]?.jsonPrimitive?.content shouldBe "DRAFT"
            body["name"]?.jsonPrimitive?.content shouldBe "Equity Crash"
            body["createdBy"]?.jsonPrimitive?.content shouldBe "analyst@kinetix.com"
            body.containsKey("id") shouldBe true
            body.containsKey("createdAt") shouldBe true
        }
    }

    test("a DRAFT scenario — PATCH /{id}/submit — status becomes PENDING_APPROVAL") {
        testApplication {
            application {
                module(frtbRepo, riskClient, stressScenarioRepository = stressScenarioRepo)
            }
            val createResponse = client.post("/api/v1/stress-scenarios") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "name": "FX Shock",
                        "description": "USD/EUR +15%",
                        "shocks": "{\"FX\":0.15}",
                        "createdBy": "analyst@kinetix.com"
                    }
                """.trimIndent())
            }
            val id = Json.parseToJsonElement(createResponse.bodyAsText())
                .jsonObject["id"]!!.jsonPrimitive.content

            val submitResponse = client.patch("/api/v1/stress-scenarios/$id/submit")
            submitResponse.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(submitResponse.bodyAsText()).jsonObject
            body["status"]?.jsonPrimitive?.content shouldBe "PENDING_APPROVAL"
        }
    }

    test("a PENDING_APPROVAL scenario — PATCH /{id}/approve — status becomes APPROVED with approvedBy") {
        testApplication {
            application {
                module(frtbRepo, riskClient, stressScenarioRepository = stressScenarioRepo)
            }
            val createResponse = client.post("/api/v1/stress-scenarios") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "name": "Rate Hike",
                        "description": "Parallel +100bps",
                        "shocks": "{\"IR\":0.01}",
                        "createdBy": "analyst@kinetix.com"
                    }
                """.trimIndent())
            }
            val id = Json.parseToJsonElement(createResponse.bodyAsText())
                .jsonObject["id"]!!.jsonPrimitive.content

            client.patch("/api/v1/stress-scenarios/$id/submit")

            val approveResponse = client.patch("/api/v1/stress-scenarios/$id/approve") {
                contentType(ContentType.Application.Json)
                setBody("""{"approvedBy":"manager@kinetix.com"}""")
            }
            approveResponse.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(approveResponse.bodyAsText()).jsonObject
            body["status"]?.jsonPrimitive?.content shouldBe "APPROVED"
            body["approvedBy"]?.jsonPrimitive?.content shouldBe "manager@kinetix.com"
            body.containsKey("approvedAt") shouldBe true
        }
    }

    test("an APPROVED scenario — PATCH /{id}/retire — status becomes RETIRED") {
        testApplication {
            application {
                module(frtbRepo, riskClient, stressScenarioRepository = stressScenarioRepo)
            }
            val createResponse = client.post("/api/v1/stress-scenarios") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "name": "Commodity Spike",
                        "description": "Oil +50%",
                        "shocks": "{\"COMM\":0.50}",
                        "createdBy": "analyst@kinetix.com"
                    }
                """.trimIndent())
            }
            val id = Json.parseToJsonElement(createResponse.bodyAsText())
                .jsonObject["id"]!!.jsonPrimitive.content

            client.patch("/api/v1/stress-scenarios/$id/submit")
            client.patch("/api/v1/stress-scenarios/$id/approve") {
                contentType(ContentType.Application.Json)
                setBody("""{"approvedBy":"manager@kinetix.com"}""")
            }

            val retireResponse = client.patch("/api/v1/stress-scenarios/$id/retire")
            retireResponse.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(retireResponse.bodyAsText()).jsonObject
            body["status"]?.jsonPrimitive?.content shouldBe "RETIRED"
        }
    }

    test("a scenario submitted by the same user who attempts to approve it — PATCH /{id}/approve with same user as creator — returns 400 for four-eyes violation") {
        testApplication {
            application {
                module(frtbRepo, riskClient, stressScenarioRepository = stressScenarioRepo)
            }
            val createResponse = client.post("/api/v1/stress-scenarios") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "name": "Self-Approval Test",
                        "description": "Must not be approved by creator",
                        "shocks": "{\"EQ\":-0.10}",
                        "createdBy": "analyst@kinetix.com"
                    }
                """.trimIndent())
            }
            val id = Json.parseToJsonElement(createResponse.bodyAsText())
                .jsonObject["id"]!!.jsonPrimitive.content

            client.patch("/api/v1/stress-scenarios/$id/submit")

            val approveResponse = client.patch("/api/v1/stress-scenarios/$id/approve") {
                contentType(ContentType.Application.Json)
                setBody("""{"approvedBy":"analyst@kinetix.com"}""")
            }
            approveResponse.status shouldBe HttpStatusCode.BadRequest
        }
    }

    test("a DRAFT scenario for invalid transition — PATCH /{id}/approve directly — returns 400 for invalid state transition") {
        testApplication {
            application {
                module(frtbRepo, riskClient, stressScenarioRepository = stressScenarioRepo)
            }
            val createResponse = client.post("/api/v1/stress-scenarios") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "name": "Invalid Transition",
                        "description": "Should fail",
                        "shocks": "{}",
                        "createdBy": "analyst@kinetix.com"
                    }
                """.trimIndent())
            }
            val id = Json.parseToJsonElement(createResponse.bodyAsText())
                .jsonObject["id"]!!.jsonPrimitive.content

            val approveResponse = client.patch("/api/v1/stress-scenarios/$id/approve") {
                contentType(ContentType.Application.Json)
                setBody("""{"approvedBy":"manager@kinetix.com"}""")
            }
            approveResponse.status shouldBe HttpStatusCode.BadRequest
        }
    }
})
