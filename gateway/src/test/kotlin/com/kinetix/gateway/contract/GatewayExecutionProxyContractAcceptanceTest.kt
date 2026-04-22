package com.kinetix.gateway.contract

import com.kinetix.gateway.routes.executionProxyRoutes
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.*

private fun Application.configureExecutionProxy(mockEngine: MockEngine) {
    val upstreamClient = HttpClient(mockEngine)
    install(ContentNegotiation) { json() }
    routing {
        executionProxyRoutes(upstreamClient, "http://position-service")
    }
}

class GatewayExecutionProxyContractAcceptanceTest : FunSpec({

    test("POST /api/v1/orders forwards the request body and returns the upstream 200 payload") {
        var capturedMethod: HttpMethod? = null
        var capturedPath: String? = null
        var capturedBody: String? = null
        val mockEngine = MockEngine { request ->
            capturedMethod = request.method
            capturedPath = request.url.encodedPath
            capturedBody = String(request.body.toByteArray())
            respond(
                content = """{"orderId":"ord-1","status":"ACCEPTED"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        testApplication {
            application { configureExecutionProxy(mockEngine) }

            val response = client.post("/api/v1/orders") {
                contentType(ContentType.Application.Json)
                setBody("""{"instrumentId":"AAPL","quantity":100,"side":"BUY"}""")
            }

            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldBe """{"orderId":"ord-1","status":"ACCEPTED"}"""
            capturedMethod shouldBe HttpMethod.Post
            capturedPath shouldBe "/api/v1/orders"
            capturedBody shouldBe """{"instrumentId":"AAPL","quantity":100,"side":"BUY"}"""
        }
    }

    test("upstream 400 on pre-trade-check propagates to the caller as 400 with body preserved") {
        val mockEngine = MockEngine { _ ->
            respondError(
                status = HttpStatusCode.BadRequest,
                content = """{"error":"invalid_instrument","message":"Unknown instrumentId"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        testApplication {
            application { configureExecutionProxy(mockEngine) }

            val response = client.post("/api/v1/risk/pre-trade-check") {
                contentType(ContentType.Application.Json)
                setBody("""{"instrumentId":"UNKNOWN","quantity":100}""")
            }

            response.status shouldBe HttpStatusCode.BadRequest
            response.bodyAsText() shouldBe """{"error":"invalid_instrument","message":"Unknown instrumentId"}"""
        }
    }

    test("upstream 503 on pre-trade-check propagates to the caller as 503 (not silently swallowed)") {
        val mockEngine = MockEngine { _ ->
            respondError(
                status = HttpStatusCode.ServiceUnavailable,
                content = """{"error":"upstream_down"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        testApplication {
            application { configureExecutionProxy(mockEngine) }

            val response = client.post("/api/v1/risk/pre-trade-check") {
                contentType(ContentType.Application.Json)
                setBody("""{"instrumentId":"AAPL","quantity":100}""")
            }

            response.status shouldBe HttpStatusCode.ServiceUnavailable
            response.bodyAsText() shouldBe """{"error":"upstream_down"}"""
        }
    }

    test("GET /api/v1/execution/cost/{bookId} forwards the bookId path parameter upstream") {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(
                content = """{"bookId":"EQ-001","slippageBps":"2.5","commission":"50.00"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        testApplication {
            application { configureExecutionProxy(mockEngine) }

            val response = client.get("/api/v1/execution/cost/EQ-001")

            response.status shouldBe HttpStatusCode.OK
            capturedPath shouldBe "/api/v1/execution/cost/EQ-001"
            response.bodyAsText() shouldBe """{"bookId":"EQ-001","slippageBps":"2.5","commission":"50.00"}"""
        }
    }

    test("upstream 404 on execution cost for a missing book propagates to the caller as 404") {
        val mockEngine = MockEngine { _ ->
            respondError(
                status = HttpStatusCode.NotFound,
                content = """{"error":"book_not_found"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        testApplication {
            application { configureExecutionProxy(mockEngine) }

            val response = client.get("/api/v1/execution/cost/UNKNOWN")

            response.status shouldBe HttpStatusCode.NotFound
            response.bodyAsText() shouldBe """{"error":"book_not_found"}"""
        }
    }

    test("POST /api/v1/execution/reconciliation/{bookId}/statements forwards body and path") {
        var capturedPath: String? = null
        var capturedBody: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            capturedBody = String(request.body.toByteArray())
            respond(
                content = """{"statementId":"stmt-1","ingested":true}""",
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        testApplication {
            application { configureExecutionProxy(mockEngine) }

            val response = client.post("/api/v1/execution/reconciliation/EQ-001/statements") {
                contentType(ContentType.Application.Json)
                setBody("""{"statementDate":"2025-01-15","rows":[]}""")
            }

            response.status shouldBe HttpStatusCode.Created
            capturedPath shouldBe "/api/v1/execution/reconciliation/EQ-001/statements"
            capturedBody shouldBe """{"statementDate":"2025-01-15","rows":[]}"""
        }
    }
})
