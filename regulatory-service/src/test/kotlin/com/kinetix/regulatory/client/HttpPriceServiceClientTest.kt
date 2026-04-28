package com.kinetix.regulatory.client

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

class HttpPriceServiceClientTest : FunSpec({

    fun mockClient(handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpClient =
        HttpClient(MockEngine { request -> handler(request) }) {
            install(ContentNegotiation) { json() }
        }

    test("happy path: maps PricePointDto to DailyClosePrice with the date portion of the timestamp") {
        val httpClient = mockClient {
            respond(
                content = """
                    [
                        {"instrumentId":"AAPL","price":{"amount":"168.00","currency":"USD"},"timestamp":"2026-04-21T16:00:00Z","source":"EXCHANGE"},
                        {"instrumentId":"AAPL","price":{"amount":"170.50","currency":"USD"},"timestamp":"2026-04-22T16:00:00Z","source":"EXCHANGE"}
                    ]
                """.trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpPriceServiceClient(httpClient, "http://price:8080")

        val prices = client.fetchDailyClosePrices("AAPL", "2026-04-21", "2026-04-22")

        prices shouldHaveSize 2
        prices[0].instrumentId shouldBe "AAPL"
        prices[0].date shouldBe "2026-04-21"
        prices[0].closePrice shouldBe 168.00
        prices[1].date shouldBe "2026-04-22"
        prices[1].closePrice shouldBe 170.50
    }

    test("happy path: encodes instrumentId, from and to into the history endpoint URL") {
        var capturedUrl: String? = null
        val httpClient = mockClient { request ->
            capturedUrl = request.url.toString()
            respond(
                content = "[]",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpPriceServiceClient(httpClient, "http://price:8080")

        client.fetchDailyClosePrices("AAPL", "2026-04-21", "2026-04-22")

        capturedUrl shouldBe "http://price:8080/api/v1/prices/AAPL/history?from=2026-04-21&to=2026-04-22&interval=1d"
    }

    test("happy path: returns an empty list when upstream has no history for the window") {
        val httpClient = mockClient {
            respond(
                content = "[]",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpPriceServiceClient(httpClient, "http://price:8080")

        val prices = client.fetchDailyClosePrices("UNKNOWN", "2026-04-21", "2026-04-22")

        prices.shouldBeEmpty()
    }

    test("404 from upstream surfaces as an exception — failures are not silently mapped to empty") {
        val httpClient = mockClient {
            respond(content = "", status = HttpStatusCode.NotFound)
        }
        val client = HttpPriceServiceClient(httpClient, "http://price:8080")

        shouldThrow<Throwable> {
            client.fetchDailyClosePrices("UNKNOWN", "2026-04-21", "2026-04-22")
        }
    }

    test("500 from upstream surfaces as an exception") {
        val httpClient = mockClient {
            respond(
                content = """{"error":"upstream failure"}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpPriceServiceClient(httpClient, "http://price:8080")

        shouldThrow<Throwable> {
            client.fetchDailyClosePrices("AAPL", "2026-04-21", "2026-04-22")
        }
    }

    test("a request that times out propagates the timeout exception to the caller") {
        val httpClient = HttpClient(MockEngine { _ ->
            kotlinx.coroutines.delay(2_000)
            respond("[]")
        }) {
            install(ContentNegotiation) { json() }
            install(HttpTimeout) {
                requestTimeoutMillis = 50
            }
        }
        val client = HttpPriceServiceClient(httpClient, "http://price:8080")

        shouldThrow<HttpRequestTimeoutException> {
            client.fetchDailyClosePrices("AAPL", "2026-04-21", "2026-04-22")
        }
    }
})
