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

class GatewayMarginContractAcceptanceTest : FunSpec({

    val sampleEstimateJson = """
        {
          "initialMargin":"12500.00",
          "variationMargin":"350.00",
          "totalMargin":"12850.00",
          "currency":"USD"
        }
    """.trimIndent()

    test("GET /api/v1/books/{bookId}/margin proxies the upstream margin estimate as JSON") {
        val backend = BackendStubServer {
            get("/api/v1/books/BOOK-001/margin") {
                call.respond(Json.parseToJsonElement(sampleEstimateJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/books/BOOK-001/margin")

                response.status shouldBe HttpStatusCode.OK
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["initialMargin"]?.jsonPrimitive?.content shouldBe "12500.00"
                body["variationMargin"]?.jsonPrimitive?.content shouldBe "350.00"
                body["totalMargin"]?.jsonPrimitive?.content shouldBe "12850.00"
                body["currency"]?.jsonPrimitive?.content shouldBe "USD"

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/books/BOOK-001/margin" }
                recorded.method shouldBe "GET"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("GET /api/v1/books/{bookId}/margin forwards the previousMTM query parameter to the upstream client") {
        val backend = BackendStubServer {
            get("/api/v1/books/BOOK-001/margin") {
                call.respond(Json.parseToJsonElement(sampleEstimateJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/books/BOOK-001/margin?previousMTM=9876.50")

                response.status shouldBe HttpStatusCode.OK

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/books/BOOK-001/margin" }
                recorded.query["previousMTM"] shouldBe listOf("9876.50")
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("GET /api/v1/books/{bookId}/margin returns 404 when the upstream reports the book is unknown") {
        val backend = BackendStubServer {
            get("/api/v1/books/UNKNOWN/margin") {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/books/UNKNOWN/margin")

                response.status shouldBe HttpStatusCode.NotFound
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("GET /api/v1/books/{bookId}/margin surfaces an upstream 503 as 503") {
        val backend = BackendStubServer {
            get("/api/v1/books/BOOK-001/margin") {
                call.respond(HttpStatusCode.ServiceUnavailable)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/books/BOOK-001/margin")

                response.status shouldBe HttpStatusCode.ServiceUnavailable
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }
})
