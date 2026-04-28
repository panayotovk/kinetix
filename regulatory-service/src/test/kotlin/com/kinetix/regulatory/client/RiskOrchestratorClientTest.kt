package com.kinetix.regulatory.client

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

class RiskOrchestratorClientTest : FunSpec({

    fun mockClient(handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpClient =
        HttpClient(MockEngine { request -> handler(request) }) {
            install(ContentNegotiation) { json() }
        }

    test("calculateFrtb: happy path posts to /api/v1/regulatory/frtb/{bookId} and decodes the response") {
        var capturedUrl: String? = null
        var capturedMethod: HttpMethod? = null
        val httpClient = mockClient { request ->
            capturedUrl = request.url.toString()
            capturedMethod = request.method
            respond(
                content = """
                    {"bookId":"BOOK-001","sbmCharges":[],"totalSbmCharge":"100.00",
                     "grossJtd":"0","hedgeBenefit":"0","netDrc":"0","exoticNotional":"0",
                     "otherNotional":"0","totalRrao":"0","totalCapitalCharge":"100.00",
                     "calculatedAt":"2026-04-22T10:00:00Z"}
                """.trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = RiskOrchestratorClient(httpClient, "http://orchestrator:8080")

        val result = client.calculateFrtb("BOOK-001")

        capturedMethod shouldBe HttpMethod.Post
        capturedUrl shouldBe "http://orchestrator:8080/api/v1/regulatory/frtb/BOOK-001"
        result.bookId shouldBe "BOOK-001"
        result.totalCapitalCharge shouldBe "100.00"
    }

    test("calculateFrtb: 404 from upstream surfaces as a deserialization exception") {
        val httpClient = mockClient {
            respond(content = "", status = HttpStatusCode.NotFound)
        }
        val client = RiskOrchestratorClient(httpClient, "http://orchestrator:8080")

        shouldThrow<Throwable> {
            client.calculateFrtb("UNKNOWN")
        }
    }

    test("calculateFrtb: 500 from upstream surfaces as an exception — failures are not swallowed") {
        val httpClient = mockClient {
            respond(
                content = """{"error":"internal failure"}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = RiskOrchestratorClient(httpClient, "http://orchestrator:8080")

        shouldThrow<Throwable> {
            client.calculateFrtb("BOOK-001")
        }
    }

    test("calculateFrtb: a request that times out propagates the timeout exception") {
        val httpClient = HttpClient(MockEngine { _ ->
            kotlinx.coroutines.delay(2_000)
            respond("{}")
        }) {
            install(ContentNegotiation) { json() }
            install(HttpTimeout) {
                requestTimeoutMillis = 50
            }
        }
        val client = RiskOrchestratorClient(httpClient, "http://orchestrator:8080")

        shouldThrow<HttpRequestTimeoutException> {
            client.calculateFrtb("BOOK-001")
        }
    }

    test("runHistoricalReplay: serialises the instrumentReturns map into the request body") {
        var capturedBody: String? = null
        var capturedUrl: String? = null
        val httpClient = mockClient { request ->
            capturedUrl = request.url.toString()
            capturedBody = (request.body as io.ktor.http.content.TextContent).text
            respond(
                content = """
                    {"periodId":"P1","scenarioName":"S","bookId":"BOOK-001",
                     "totalPnlImpact":"-12.34","positionImpacts":[],
                     "windowStart":"2026-01-01","windowEnd":"2026-01-31",
                     "calculatedAt":"2026-04-22T10:00:00Z"}
                """.trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = RiskOrchestratorClient(httpClient, "http://orchestrator:8080")

        val result = client.runHistoricalReplay(
            bookId = "BOOK-001",
            instrumentReturns = mapOf("AAPL" to listOf(0.01, -0.02), "MSFT" to listOf(0.0)),
            windowStart = "2026-01-01",
            windowEnd = "2026-01-31",
        )

        capturedUrl shouldBe "http://orchestrator:8080/api/v1/risk/stress/BOOK-001/historical-replay"
        capturedBody!!.shouldContain("AAPL")
        capturedBody!!.shouldContain("MSFT")
        capturedBody!!.shouldContain("dailyReturns")
        result.totalPnlImpact shouldBe "-12.34"
    }

    test("runReverseStress: posts targetLoss and maxShock and returns achieved loss") {
        val httpClient = mockClient {
            respond(
                content = """
                    {"shocks":[{"instrumentId":"AAPL","shock":"-0.10"}],
                     "achievedLoss":"-1000.00","targetLoss":"-1000.00",
                     "converged":true,"calculatedAt":"2026-04-22T10:00:00Z"}
                """.trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = RiskOrchestratorClient(httpClient, "http://orchestrator:8080")

        val result = client.runReverseStress(
            bookId = "BOOK-001",
            targetLoss = -1000.0,
            maxShock = 0.5,
        )

        result.shocks shouldHaveSize 1
        result.converged shouldBe true
        result.achievedLoss shouldBe "-1000.00"
    }

    test("runStressTest: posts scenarioName and priceShocks and returns pnlImpact") {
        var capturedBody: String? = null
        val httpClient = mockClient { request ->
            capturedBody = (request.body as io.ktor.http.content.TextContent).text
            respond(
                content = """{"pnlImpact":"-50000.00"}""",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = RiskOrchestratorClient(httpClient, "http://orchestrator:8080")

        val result = client.runStressTest(
            bookId = "BOOK-001",
            scenarioName = "MARKET_CRASH",
            priceShocks = mapOf("AAPL" to -0.20, "MSFT" to -0.15),
        )

        capturedBody!!.shouldContain("MARKET_CRASH")
        capturedBody!!.shouldContain("AAPL")
        result.pnlImpact shouldBe "-50000.00"
    }
})
