package com.kinetix.gateway.routes

import com.kinetix.gateway.client.*
import com.kinetix.gateway.module
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.serialization.json.*

private val sampleScenario = StressScenarioItem(
    id = "scenario-1",
    name = "GFC Replay",
    description = "2008 crisis replay",
    shocks = """{"volShocks":{"EQUITY":3.0},"priceShocks":{"EQUITY":0.6}}""",
    status = "DRAFT",
    createdBy = "analyst@kinetix.com",
    approvedBy = null,
    approvedAt = null,
    createdAt = "2026-03-03T08:00:00Z",
)

class StressScenarioRoutesTest : FunSpec({

    val regulatoryClient = mockk<RegulatoryServiceClient>()

    beforeEach {
        clearMocks(regulatoryClient)
    }

    test("GET /api/v1/stress-scenarios returns all scenarios") {
        coEvery { regulatoryClient.listScenarios() } returns listOf(sampleScenario)

        testApplication {
            application { module(regulatoryClient) }
            val response = client.get("/api/v1/stress-scenarios")
            response.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            body.size shouldBe 1
            body[0].jsonObject["id"]?.jsonPrimitive?.content shouldBe "scenario-1"
            body[0].jsonObject["name"]?.jsonPrimitive?.content shouldBe "GFC Replay"
            body[0].jsonObject["status"]?.jsonPrimitive?.content shouldBe "DRAFT"
        }
    }

    test("GET /api/v1/stress-scenarios/approved returns approved scenarios") {
        val approved = sampleScenario.copy(status = "APPROVED", approvedBy = "head@kinetix.com")
        coEvery { regulatoryClient.listApprovedScenarios() } returns listOf(approved)

        testApplication {
            application { module(regulatoryClient) }
            val response = client.get("/api/v1/stress-scenarios/approved")
            response.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            body.size shouldBe 1
            body[0].jsonObject["status"]?.jsonPrimitive?.content shouldBe "APPROVED"
        }
    }

    test("POST /api/v1/stress-scenarios creates scenario") {
        coEvery { regulatoryClient.createScenario(any()) } returns sampleScenario

        testApplication {
            application { module(regulatoryClient) }
            val response = client.post("/api/v1/stress-scenarios") {
                contentType(ContentType.Application.Json)
                setBody("""{"name":"GFC Replay","description":"2008 crisis replay","shocks":"{}","createdBy":"analyst@kinetix.com"}""")
            }
            response.status shouldBe HttpStatusCode.Created
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["id"]?.jsonPrimitive?.content shouldBe "scenario-1"
            body["name"]?.jsonPrimitive?.content shouldBe "GFC Replay"
        }
    }

    test("PATCH /api/v1/stress-scenarios/{id}/submit submits for approval") {
        val submitted = sampleScenario.copy(status = "PENDING_APPROVAL")
        coEvery { regulatoryClient.submitForApproval("scenario-1") } returns submitted

        testApplication {
            application { module(regulatoryClient) }
            val response = client.patch("/api/v1/stress-scenarios/scenario-1/submit")
            response.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["status"]?.jsonPrimitive?.content shouldBe "PENDING_APPROVAL"
        }
    }

    test("PATCH /api/v1/stress-scenarios/{id}/approve approves scenario") {
        val approved = sampleScenario.copy(status = "APPROVED", approvedBy = "head@kinetix.com")
        coEvery { regulatoryClient.approve("scenario-1", any()) } returns approved

        testApplication {
            application { module(regulatoryClient) }
            val response = client.patch("/api/v1/stress-scenarios/scenario-1/approve") {
                contentType(ContentType.Application.Json)
                setBody("""{"approvedBy":"head@kinetix.com"}""")
            }
            response.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["status"]?.jsonPrimitive?.content shouldBe "APPROVED"
            body["approvedBy"]?.jsonPrimitive?.content shouldBe "head@kinetix.com"
        }
    }

    test("PATCH /api/v1/stress-scenarios/{id}/retire retires scenario") {
        val retired = sampleScenario.copy(status = "RETIRED")
        coEvery { regulatoryClient.retire("scenario-1") } returns retired

        testApplication {
            application { module(regulatoryClient) }
            val response = client.patch("/api/v1/stress-scenarios/scenario-1/retire")
            response.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["status"]?.jsonPrimitive?.content shouldBe "RETIRED"
        }
    }
})
