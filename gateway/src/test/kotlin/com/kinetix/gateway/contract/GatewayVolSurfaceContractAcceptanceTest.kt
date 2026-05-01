package com.kinetix.gateway.contract

import com.kinetix.gateway.client.HttpVolatilityServiceClient
import com.kinetix.gateway.moduleWithVolSurface
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

class GatewayVolSurfaceContractAcceptanceTest : FunSpec({

    // HttpVolatilityServiceClient.getSurface() calls /api/v1/volatility/{id}/surface/latest
    // HttpVolatilityServiceClient.getSurfaceDiff() calls /api/v1/volatility/{id}/surface/diff

    val surfaceJson = """
        {
          "instrumentId":"AAPL",
          "asOfDate":"2026-03-25T10:00:00Z",
          "source":"BLOOMBERG",
          "points":[
            {"strike":100.0,"maturityDays":30,"impliedVol":0.25},
            {"strike":110.0,"maturityDays":30,"impliedVol":0.22}
          ]
        }
    """.trimIndent()

    val diffJson = """
        {
          "instrumentId":"AAPL",
          "baseDate":"2026-03-25T10:00:00Z",
          "compareDate":"2026-03-24T10:00:00Z",
          "diffs":[
            {"strike":100.0,"maturityDays":30,"baseVol":0.25,"compareVol":0.24,"diff":0.01}
          ]
        }
    """.trimIndent()

    test("gateway routing to volatility-service surface endpoint — GET /api/v1/volatility/{instrumentId}/surface returns a surface — responds 200 with instrumentId, asOfDate, source, and points array") {
        val backend = BackendStubServer {
            get("/api/v1/volatility/AAPL/surface/latest") {
                call.respond(Json.parseToJsonElement(surfaceJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val volClient = HttpVolatilityServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { moduleWithVolSurface(volClient) }
                val response = client.get("/api/v1/volatility/AAPL/surface")
                response.status shouldBe HttpStatusCode.OK

                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["instrumentId"]?.jsonPrimitive?.content shouldBe "AAPL"
                body["asOfDate"]?.jsonPrimitive?.content shouldBe "2026-03-25T10:00:00Z"
                body["source"]?.jsonPrimitive?.content shouldBe "BLOOMBERG"

                val points = body["points"]!!.jsonArray
                points.size shouldBe 2
                val first = points[0].jsonObject
                first["strike"]?.jsonPrimitive?.double shouldBe 100.0
                first["maturityDays"]?.jsonPrimitive?.int shouldBe 30
                first["impliedVol"]?.jsonPrimitive?.double shouldBe 0.25

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/volatility/AAPL/surface/latest" }
                recorded.method shouldBe "GET"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to volatility-service surface endpoint — GET /api/v1/volatility/{instrumentId}/surface when no surface exists — responds 404") {
        val backend = BackendStubServer {
            get("/api/v1/volatility/UNKNOWN/surface/latest") {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val volClient = HttpVolatilityServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { moduleWithVolSurface(volClient) }
                val response = client.get("/api/v1/volatility/UNKNOWN/surface")
                response.status shouldBe HttpStatusCode.NotFound
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to volatility-service surface endpoint — GET /api/v1/volatility/{instrumentId}/surface/diff returns diffs — responds 200 with instrumentId, baseDate, compareDate, and diffs array") {
        val backend = BackendStubServer {
            get("/api/v1/volatility/AAPL/surface/diff") {
                call.respond(Json.parseToJsonElement(diffJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val volClient = HttpVolatilityServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { moduleWithVolSurface(volClient) }
                val response = client.get("/api/v1/volatility/AAPL/surface/diff?compareDate=2026-03-24T10:00:00Z")
                response.status shouldBe HttpStatusCode.OK

                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["instrumentId"]?.jsonPrimitive?.content shouldBe "AAPL"
                body.containsKey("baseDate") shouldBe true
                body.containsKey("compareDate") shouldBe true

                val diffs = body["diffs"]!!.jsonArray
                diffs.size shouldBe 1
                val d = diffs[0].jsonObject
                d["strike"]?.jsonPrimitive?.double shouldBe 100.0
                d["maturityDays"]?.jsonPrimitive?.int shouldBe 30
                d.containsKey("baseVol") shouldBe true
                d.containsKey("compareVol") shouldBe true
                d.containsKey("diff") shouldBe true

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/volatility/AAPL/surface/diff" }
                recorded.method shouldBe "GET"
                recorded.query["compareDate"] shouldBe listOf("2026-03-24T10:00:00Z")
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to volatility-service surface endpoint — GET /api/v1/volatility/{instrumentId}/surface/diff without compareDate — responds 400") {
        // Gateway-side validation rejects missing compareDate before reaching the upstream.
        val backend = BackendStubServer { }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val volClient = HttpVolatilityServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { moduleWithVolSurface(volClient) }
                val response = client.get("/api/v1/volatility/AAPL/surface/diff")
                response.status shouldBe HttpStatusCode.BadRequest

                backend.recordedRequests shouldBe emptyList()
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }
})
