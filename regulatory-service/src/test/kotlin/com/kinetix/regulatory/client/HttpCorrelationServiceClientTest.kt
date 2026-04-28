package com.kinetix.regulatory.client

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

class HttpCorrelationServiceClientTest : FunSpec({

    fun mockClient(handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpClient =
        HttpClient(MockEngine { request -> handler(request) }) {
            install(ContentNegotiation) { json() }
        }

    test("happy path: returns the matrix when the upstream serves a valid payload") {
        val httpClient = mockClient {
            respond(
                content = """
                    {"labels":["EQUITY","FX"],"values":[1.0,0.3,0.3,1.0],
                     "windowDays":252,"asOfDate":"2026-04-22","method":"PEARSON"}
                """.trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpCorrelationServiceClient(httpClient, "http://correlation:8080")

        val matrix = client.fetchLatestMatrix(listOf("EQUITY", "FX"))

        matrix shouldBe CorrelationMatrix(
            labels = listOf("EQUITY", "FX"),
            values = listOf(1.0, 0.3, 0.3, 1.0),
        )
    }

    test("happy path: encodes asset classes as a comma-separated labels query parameter") {
        var capturedUrl: String? = null
        val httpClient = mockClient { request ->
            capturedUrl = request.url.toString()
            respond(
                content = """{"labels":["EQUITY"],"values":[1.0],"windowDays":252,"asOfDate":"2026-04-22","method":"PEARSON"}""",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpCorrelationServiceClient(httpClient, "http://correlation:8080")

        client.fetchLatestMatrix(listOf("EQUITY", "FX", "RATES"))

        capturedUrl shouldBe "http://correlation:8080/api/v1/correlations/latest?labels=EQUITY,FX,RATES&window=252"
    }

    test("404 returns null instead of failing") {
        val httpClient = mockClient {
            respond(content = "", status = HttpStatusCode.NotFound)
        }
        val client = HttpCorrelationServiceClient(httpClient, "http://correlation:8080")

        val matrix = client.fetchLatestMatrix(listOf("EQUITY"))

        matrix shouldBe null
    }

    test("500 surfaces as an exception — failures are not swallowed") {
        val httpClient = mockClient {
            respond(
                content = """{"error":"upstream failure"}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpCorrelationServiceClient(httpClient, "http://correlation:8080")

        shouldThrow<Throwable> {
            client.fetchLatestMatrix(listOf("EQUITY"))
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
        val client = HttpCorrelationServiceClient(httpClient, "http://correlation:8080")

        shouldThrow<HttpRequestTimeoutException> {
            client.fetchLatestMatrix(listOf("EQUITY"))
        }
    }

    test("an empty matrix from upstream is returned as-is") {
        val httpClient = mockClient {
            respond(
                content = """{"labels":[],"values":[],"windowDays":252,"asOfDate":"2026-04-22","method":"PEARSON"}""",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpCorrelationServiceClient(httpClient, "http://correlation:8080")

        val matrix = client.fetchLatestMatrix(emptyList())!!

        matrix.labels.shouldContainExactly()
        matrix.values.shouldContainExactly()
    }
})
