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
import io.ktor.server.routing.get
import io.ktor.server.testing.*
import kotlinx.serialization.json.*

class GatewayMarketRegimeContractAcceptanceTest : FunSpec({

    val currentRegimeJson = """
        {
          "regime":"ELEVATED_VOL",
          "previousRegime":"NORMAL",
          "transitionedAt":"2025-01-15T10:00:00Z",
          "confidence":"0.92",
          "degradedInputs":false,
          "effectiveCalculationType":"MONTE_CARLO",
          "effectiveConfidenceLevel":"CL_99",
          "effectiveTimeHorizonDays":1,
          "effectiveCorrelationMethod":"EWMA"
        }
    """.trimIndent()

    val regimeHistoryJson = """
        {
          "transitions":[
            {"regime":"NORMAL","previousRegime":"ELEVATED_VOL","transitionedAt":"2025-01-14T09:00:00Z"},
            {"regime":"ELEVATED_VOL","previousRegime":"NORMAL","transitionedAt":"2025-01-15T10:00:00Z"}
          ]
        }
    """.trimIndent()

    test("GET /api/v1/risk/regime/current returns the upstream payload as JSON") {
        val backend = BackendStubServer {
            get("/api/v1/risk/regime/current") {
                call.respond(Json.parseToJsonElement(currentRegimeJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/risk/regime/current")

                response.status shouldBe HttpStatusCode.OK
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["regime"]?.jsonPrimitive?.content shouldBe "ELEVATED_VOL"
                body["previousRegime"]?.jsonPrimitive?.content shouldBe "NORMAL"
                body["effectiveCalculationType"]?.jsonPrimitive?.content shouldBe "MONTE_CARLO"

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/risk/regime/current" }
                recorded.method shouldBe "GET"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("GET /api/v1/risk/regime/history defaults to limit=50 when no query param is provided") {
        val backend = BackendStubServer {
            get("/api/v1/risk/regime/history") {
                call.respond(Json.parseToJsonElement(regimeHistoryJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/risk/regime/history")

                response.status shouldBe HttpStatusCode.OK
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["transitions"]?.jsonArray?.size shouldBe 2

                // Verify the gateway coerces the default limit and forwards it
                val recorded = backend.recordedRequests.single { it.path == "/api/v1/risk/regime/history" }
                recorded.query["limit"] shouldBe listOf("50")
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("GET /api/v1/risk/regime/history with limit=10 forwards the coerced limit to the upstream") {
        val backend = BackendStubServer {
            get("/api/v1/risk/regime/history") {
                call.respond(Json.parseToJsonElement(regimeHistoryJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/risk/regime/history?limit=10")

                response.status shouldBe HttpStatusCode.OK

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/risk/regime/history" }
                recorded.query["limit"] shouldBe listOf("10")
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("GET /api/v1/risk/regime/history coerces an out-of-range limit to 500 (upper bound)") {
        val backend = BackendStubServer {
            get("/api/v1/risk/regime/history") {
                call.respond(Json.parseToJsonElement(regimeHistoryJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/risk/regime/history?limit=9999")

                response.status shouldBe HttpStatusCode.OK

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/risk/regime/history" }
                recorded.query["limit"] shouldBe listOf("500")
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("GET /api/v1/risk/regime/history coerces a non-numeric limit to the default of 50") {
        val backend = BackendStubServer {
            get("/api/v1/risk/regime/history") {
                call.respond(Json.parseToJsonElement(regimeHistoryJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/risk/regime/history?limit=abc")

                response.status shouldBe HttpStatusCode.OK

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/risk/regime/history" }
                recorded.query["limit"] shouldBe listOf("50")
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }
})
