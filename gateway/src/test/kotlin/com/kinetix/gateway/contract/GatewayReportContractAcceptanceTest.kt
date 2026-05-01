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
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.testing.*
import kotlinx.serialization.json.*

class GatewayReportContractAcceptanceTest : FunSpec({

    val templateListJson = """
        [
          {
            "templateId":"tpl-risk-summary",
            "name":"Risk Summary",
            "templateType":"RISK_SUMMARY",
            "ownerUserId":"SYSTEM",
            "description":"Per-book VaR and Greeks",
            "source":"risk_positions_flat"
          }
        ]
    """.trimIndent()

    val outputJson = """
        {
          "outputId":"out-abc",
          "templateId":"tpl-risk-summary",
          "generatedAt":"2025-01-15T10:00:00Z",
          "outputFormat":"JSON",
          "rowCount":2
        }
    """.trimIndent()

    test("gateway routing to report endpoints — GET /api/v1/reports/templates — returns 200 with list of templates") {
        val backend = BackendStubServer {
            get("/api/v1/reports/templates") {
                call.respond(Json.parseToJsonElement(templateListJson).jsonArray)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/reports/templates")

                response.status shouldBe HttpStatusCode.OK
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
                body.size shouldBe 1
                body[0].jsonObject["templateId"]?.jsonPrimitive?.content shouldBe "tpl-risk-summary"

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/reports/templates" }
                recorded.method shouldBe "GET"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to report endpoints — POST /api/v1/reports/generate with valid request — returns 200 with report output") {
        val backend = BackendStubServer {
            post("/api/v1/reports/generate") {
                call.respond(Json.parseToJsonElement(outputJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.post("/api/v1/reports/generate") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"templateId":"tpl-risk-summary","bookId":"BOOK-1","format":"JSON"}""")
                }

                response.status shouldBe HttpStatusCode.OK
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["outputId"]?.jsonPrimitive?.content shouldBe "out-abc"

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/reports/generate" }
                recorded.method shouldBe "POST"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to report endpoints — GET /api/v1/reports/{outputId} for existing output — returns 200 with the output") {
        val backend = BackendStubServer {
            get("/api/v1/reports/out-abc") {
                call.respond(Json.parseToJsonElement(outputJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/reports/out-abc")

                response.status shouldBe HttpStatusCode.OK
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["outputId"]?.jsonPrimitive?.content shouldBe "out-abc"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to report endpoints — GET /api/v1/reports/{outputId} for missing output — returns 404") {
        val backend = BackendStubServer {
            get("/api/v1/reports/missing") {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/reports/missing")

                response.status shouldBe HttpStatusCode.NotFound
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to report endpoints — GET /api/v1/reports/{outputId}/csv for existing output — returns 200 with CSV text") {
        val backend = BackendStubServer {
            get("/api/v1/reports/out-abc/csv") {
                call.respondText("book_id,instrument_id\nBOOK-1,AAPL", ContentType.Text.Plain)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/reports/out-abc/csv")

                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldBe "book_id,instrument_id\nBOOK-1,AAPL"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to report endpoints — GET /api/v1/reports/{outputId}/csv for missing output — returns 404") {
        val backend = BackendStubServer {
            get("/api/v1/reports/missing/csv") {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/reports/missing/csv")

                response.status shouldBe HttpStatusCode.NotFound
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }
})
