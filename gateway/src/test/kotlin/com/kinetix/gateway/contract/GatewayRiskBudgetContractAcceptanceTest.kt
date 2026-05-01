package com.kinetix.gateway.contract

import com.kinetix.gateway.client.HttpRiskServiceClient
import com.kinetix.gateway.module
import com.kinetix.gateway.testing.BackendStubServer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.testing.*
import kotlinx.serialization.json.*

class GatewayRiskBudgetContractAcceptanceTest : FunSpec({

    val sampleAllocationJson = """
        {
          "id":"alloc-1","entityLevel":"DESK","entityId":"desk-rates",
          "budgetType":"VAR_BUDGET","budgetPeriod":"DAILY",
          "budgetAmount":"5000000.00","effectiveFrom":"2026-01-01",
          "effectiveTo":null,"allocatedBy":"cro@firm.com","allocationNote":null
        }
    """.trimIndent()

    test("gateway routing to risk budget endpoints — GET /api/v1/risk/budgets returns 200 with array of allocations") {
        val backend = BackendStubServer {
            get("/api/v1/risk/budgets") {
                call.respond(Json.parseToJsonElement("[$sampleAllocationJson]").jsonArray)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/risk/budgets")

                response.status shouldBe HttpStatusCode.OK

                val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
                body.size shouldBe 1
                body[0].jsonObject["id"]?.jsonPrimitive?.content shouldBe "alloc-1"

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/risk/budgets" }
                recorded.method shouldBe "GET"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to risk budget endpoints — GET /api/v1/risk/budgets/{id} finds allocation — returns 200 with allocation details") {
        val backend = BackendStubServer {
            get("/api/v1/risk/budgets/alloc-1") {
                call.respond(Json.parseToJsonElement(sampleAllocationJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/risk/budgets/alloc-1")

                response.status shouldBe HttpStatusCode.OK

                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["entityLevel"]?.jsonPrimitive?.content shouldBe "DESK"
                body["budgetType"]?.jsonPrimitive?.content shouldBe "VAR_BUDGET"
                body["budgetAmount"]?.jsonPrimitive?.content shouldBe "5000000.00"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to risk budget endpoints — GET /api/v1/risk/budgets/{id} budget not found — returns 404") {
        val backend = BackendStubServer {
            get("/api/v1/risk/budgets/nonexistent") {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/risk/budgets/nonexistent")
                response.status shouldBe HttpStatusCode.NotFound
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to risk budget endpoints — POST /api/v1/risk/budgets creates a budget — returns 201 with created allocation") {
        val backend = BackendStubServer {
            post("/api/v1/risk/budgets") {
                call.respond(HttpStatusCode.Created, Json.parseToJsonElement(sampleAllocationJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.post("/api/v1/risk/budgets") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {"entityLevel":"DESK","entityId":"desk-rates","budgetType":"VAR_BUDGET",
                         "budgetPeriod":"DAILY","budgetAmount":"5000000.00",
                         "effectiveFrom":"2026-01-01","allocatedBy":"cro@firm.com"}
                        """.trimIndent()
                    )
                }

                response.status shouldBe HttpStatusCode.Created

                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body.containsKey("id") shouldBe true

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/risk/budgets" && it.method == "POST" }
                recorded.method shouldBe "POST"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to risk budget endpoints — DELETE /api/v1/risk/budgets/{id} removes an allocation — returns 204") {
        val backend = BackendStubServer {
            delete("/api/v1/risk/budgets/alloc-1") {
                call.respond(HttpStatusCode.NoContent)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.delete("/api/v1/risk/budgets/alloc-1")
                response.status shouldBe HttpStatusCode.NoContent

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/risk/budgets/alloc-1" }
                recorded.method shouldBe "DELETE"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to risk budget endpoints — DELETE /api/v1/risk/budgets/{id} when not found — returns 404") {
        val backend = BackendStubServer {
            delete("/api/v1/risk/budgets/nonexistent") {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.delete("/api/v1/risk/budgets/nonexistent")
                response.status shouldBe HttpStatusCode.NotFound
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }
})
