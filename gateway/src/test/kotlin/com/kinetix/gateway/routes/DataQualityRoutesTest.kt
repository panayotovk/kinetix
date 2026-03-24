package com.kinetix.gateway.routes

import com.kinetix.gateway.client.PositionServiceClient
import com.kinetix.gateway.client.PriceServiceClient
import com.kinetix.gateway.client.RiskServiceClient
import com.kinetix.gateway.moduleWithDataQuality
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.serialization.json.*

class DataQualityRoutesTest : FunSpec({

    val positionClient = mockk<PositionServiceClient>()
    val priceClient = mockk<PriceServiceClient>()
    val riskClient = mockk<RiskServiceClient>()

    beforeEach { clearMocks(positionClient, priceClient, riskClient) }

    test("GET /api/v1/data-quality/status returns 200 with quality checks") {
        testApplication {
            application { moduleWithDataQuality(positionClient, priceClient, riskClient) }
            val response = client.get("/api/v1/data-quality/status")
            response.status shouldBe HttpStatusCode.OK

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body.containsKey("overall") shouldBe true
            body.containsKey("checks") shouldBe true

            val checks = body["checks"]?.jsonArray
            checks!!.size shouldBe 4
        }
    }

    test("GET /api/v1/data-quality/status returns check names including FIX connectivity") {
        testApplication {
            application { moduleWithDataQuality(positionClient, priceClient, riskClient) }
            val response = client.get("/api/v1/data-quality/status")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val checks = body["checks"]!!.jsonArray

            val checkNames = checks.map { it.jsonObject["name"]?.jsonPrimitive?.content }
            checkNames shouldBe listOf(
                "Price Freshness",
                "Position Count",
                "Risk Result Completeness",
                "FIX Connectivity",
            )
        }
    }

    test("GET /api/v1/data-quality/status returns UNKNOWN overall when all checks are not implemented") {
        testApplication {
            application { moduleWithDataQuality(positionClient, priceClient, riskClient) }
            val response = client.get("/api/v1/data-quality/status")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["overall"]?.jsonPrimitive?.content shouldBe "UNKNOWN"
        }
    }

    test("each check has required fields: name, status, message, lastChecked") {
        testApplication {
            application { moduleWithDataQuality(positionClient, priceClient, riskClient) }
            val response = client.get("/api/v1/data-quality/status")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val checks = body["checks"]!!.jsonArray

            for (check in checks) {
                val obj = check.jsonObject
                obj.containsKey("name") shouldBe true
                obj.containsKey("status") shouldBe true
                obj.containsKey("message") shouldBe true
                obj.containsKey("lastChecked") shouldBe true
            }
        }
    }

    // --- FIX connectivity check tests using MockEngine ---

    fun buildMockHttpClient(sessionsJson: String, statusCode: HttpStatusCode = HttpStatusCode.OK): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(
                content = sessionsJson,
                status = statusCode,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
    }

    fun Application.configureWithMockFixClient(mockHttpClient: HttpClient) {
        install(ServerContentNegotiation) { json() }
        routing {
            dataQualityRoutes(mockHttpClient, "http://position-service")
        }
    }

    test("FIX connectivity check returns OK when all sessions are CONNECTED") {
        val mockClient = buildMockHttpClient(
            """[{"sessionId":"S1","counterparty":"Broker A","status":"CONNECTED","inboundSeqNum":100,"outboundSeqNum":50}]"""
        )

        testApplication {
            application { configureWithMockFixClient(mockClient) }
            val response = client.get("/api/v1/data-quality/status")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val fixCheck = body["checks"]!!.jsonArray
                .map { it.jsonObject }
                .first { it["name"]!!.jsonPrimitive.content == "FIX Connectivity" }

            fixCheck["status"]!!.jsonPrimitive.content shouldBe "OK"
            fixCheck["message"]!!.jsonPrimitive.content shouldContain "1 FIX session(s) connected"
        }
    }

    test("FIX connectivity check returns CRITICAL when all sessions are DISCONNECTED") {
        val mockClient = buildMockHttpClient(
            """[
                {"sessionId":"S1","counterparty":"Broker A","status":"DISCONNECTED","inboundSeqNum":0,"outboundSeqNum":0},
                {"sessionId":"S2","counterparty":"Broker B","status":"DISCONNECTED","inboundSeqNum":0,"outboundSeqNum":0}
            ]"""
        )

        testApplication {
            application { configureWithMockFixClient(mockClient) }
            val response = client.get("/api/v1/data-quality/status")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val fixCheck = body["checks"]!!.jsonArray
                .map { it.jsonObject }
                .first { it["name"]!!.jsonPrimitive.content == "FIX Connectivity" }

            fixCheck["status"]!!.jsonPrimitive.content shouldBe "CRITICAL"
            body["overall"]!!.jsonPrimitive.content shouldBe "CRITICAL"
        }
    }

    test("FIX connectivity check returns WARNING when some sessions are DISCONNECTED") {
        val mockClient = buildMockHttpClient(
            """[
                {"sessionId":"S1","counterparty":"Broker A","status":"CONNECTED","inboundSeqNum":100,"outboundSeqNum":50},
                {"sessionId":"S2","counterparty":"Broker B","status":"DISCONNECTED","inboundSeqNum":0,"outboundSeqNum":0}
            ]"""
        )

        testApplication {
            application { configureWithMockFixClient(mockClient) }
            val response = client.get("/api/v1/data-quality/status")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val fixCheck = body["checks"]!!.jsonArray
                .map { it.jsonObject }
                .first { it["name"]!!.jsonPrimitive.content == "FIX Connectivity" }

            fixCheck["status"]!!.jsonPrimitive.content shouldBe "WARNING"
        }
    }

    test("FIX connectivity check returns CRITICAL when position-service is unreachable") {
        val mockEngine = MockEngine { _ -> throw java.io.IOException("Connection refused") }
        val mockClient = HttpClient(mockEngine) { install(ContentNegotiation) { json() } }

        testApplication {
            application { configureWithMockFixClient(mockClient) }
            val response = client.get("/api/v1/data-quality/status")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val fixCheck = body["checks"]!!.jsonArray
                .map { it.jsonObject }
                .first { it["name"]!!.jsonPrimitive.content == "FIX Connectivity" }

            fixCheck["status"]!!.jsonPrimitive.content shouldBe "CRITICAL"
            body["overall"]!!.jsonPrimitive.content shouldBe "CRITICAL"
        }
    }

    test("FIX connectivity check returns WARNING when no sessions are configured") {
        val mockClient = buildMockHttpClient("[]")

        testApplication {
            application { configureWithMockFixClient(mockClient) }
            val response = client.get("/api/v1/data-quality/status")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val fixCheck = body["checks"]!!.jsonArray
                .map { it.jsonObject }
                .first { it["name"]!!.jsonPrimitive.content == "FIX Connectivity" }

            fixCheck["status"]!!.jsonPrimitive.content shouldBe "WARNING"
            fixCheck["message"]!!.jsonPrimitive.content shouldContain "No FIX sessions configured"
        }
    }
})
