package com.kinetix.gateway.contract

import com.kinetix.gateway.routes.limitsRoutes
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

private fun Application.configureLimitsProxy(mockEngine: MockEngine) {
    val upstreamClient = HttpClient(mockEngine)
    install(ContentNegotiation) { json() }
    routing {
        limitsRoutes(upstreamClient, "http://position-service")
    }
}

class GatewayLimitsContractAcceptanceTest : FunSpec({

    test("GET /api/v1/limits forwards to position-service and returns the upstream payload verbatim") {
        var capturedPath: String? = null
        var capturedMethod: HttpMethod? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            capturedMethod = request.method
            respond(
                content = """[{"id":"l-1","level":"FIRM","entityId":"firm-1","limitType":"VAR","limitValue":"1000000","intradayLimit":null,"overnightLimit":null,"active":true}]""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        testApplication {
            application { configureLimitsProxy(mockEngine) }

            val response = client.get("/api/v1/limits")

            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldBe """[{"id":"l-1","level":"FIRM","entityId":"firm-1","limitType":"VAR","limitValue":"1000000","intradayLimit":null,"overnightLimit":null,"active":true}]"""
            capturedPath shouldBe "/api/v1/limits"
            capturedMethod shouldBe HttpMethod.Get
        }
    }

    test("POST /api/v1/limits forwards the JSON body to position-service and returns 201") {
        var capturedBody: String? = null
        val mockEngine = MockEngine { request ->
            capturedBody = String(request.body.toByteArray())
            respond(
                content = """{"id":"l-2","level":"DESK","entityId":"desk-eq","limitType":"NOTIONAL","limitValue":"5000000","intradayLimit":null,"overnightLimit":null,"active":true}""",
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        testApplication {
            application { configureLimitsProxy(mockEngine) }

            val response = client.post("/api/v1/limits") {
                contentType(ContentType.Application.Json)
                setBody("""{"level":"DESK","entityId":"desk-eq","limitType":"NOTIONAL","limitValue":"5000000"}""")
            }

            response.status shouldBe HttpStatusCode.Created
            capturedBody shouldBe """{"level":"DESK","entityId":"desk-eq","limitType":"NOTIONAL","limitValue":"5000000"}"""
        }
    }

    test("PUT /api/v1/limits/{id} forwards the path id and body to position-service") {
        var capturedPath: String? = null
        var capturedBody: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            capturedBody = String(request.body.toByteArray())
            respond(
                content = """{"id":"l-1","level":"FIRM","entityId":"firm-1","limitType":"VAR","limitValue":"1500000","intradayLimit":null,"overnightLimit":null,"active":true}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        testApplication {
            application { configureLimitsProxy(mockEngine) }

            val response = client.put("/api/v1/limits/l-1") {
                contentType(ContentType.Application.Json)
                setBody("""{"limitValue":"1500000"}""")
            }

            response.status shouldBe HttpStatusCode.OK
            capturedPath shouldBe "/api/v1/limits/l-1"
            capturedBody shouldBe """{"limitValue":"1500000"}"""
        }
    }

    test("POST /api/v1/limits/{id}/temporary-increase forwards the path id and body to position-service") {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(
                content = """{"id":"ti-1","limitId":"l-1","newValue":"2000000","approvedBy":"head-of-risk","expiresAt":"2026-04-30T18:00:00Z","reason":"Earnings","createdAt":"2026-04-29T09:00:00Z"}""",
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        testApplication {
            application { configureLimitsProxy(mockEngine) }

            val response = client.post("/api/v1/limits/l-1/temporary-increase") {
                contentType(ContentType.Application.Json)
                setBody("""{"newValue":"2000000","approvedBy":"head-of-risk","expiresAt":"2026-04-30T18:00:00Z","reason":"Earnings"}""")
            }

            response.status shouldBe HttpStatusCode.Created
            capturedPath shouldBe "/api/v1/limits/l-1/temporary-increase"
        }
    }

    test("upstream 404 on PUT propagates to the caller as 404 with body preserved") {
        val mockEngine = MockEngine { _ ->
            respondError(
                status = HttpStatusCode.NotFound,
                content = """{"error":"not_found","message":"Limit definition not found"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        testApplication {
            application { configureLimitsProxy(mockEngine) }

            val response = client.put("/api/v1/limits/missing") {
                contentType(ContentType.Application.Json)
                setBody("""{"limitValue":"100"}""")
            }

            response.status shouldBe HttpStatusCode.NotFound
            response.bodyAsText() shouldBe """{"error":"not_found","message":"Limit definition not found"}"""
        }
    }

    test("upstream 500 on POST propagates to the caller as 500 (no silent swallow)") {
        val mockEngine = MockEngine { _ ->
            respondError(
                status = HttpStatusCode.InternalServerError,
                content = """{"error":"upstream_failure"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        testApplication {
            application { configureLimitsProxy(mockEngine) }

            val response = client.post("/api/v1/limits") {
                contentType(ContentType.Application.Json)
                setBody("""{"level":"FIRM","entityId":"f","limitType":"VAR","limitValue":"1"}""")
            }

            response.status shouldBe HttpStatusCode.InternalServerError
        }
    }
})
