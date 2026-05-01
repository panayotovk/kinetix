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
import io.ktor.server.routing.post
import io.ktor.server.testing.*
import kotlinx.serialization.json.*

class GatewayLiquidityRiskContractAcceptanceTest : FunSpec({

    val sampleResponseJson = """
        {
          "bookId":"BOOK-1",
          "portfolioLvar":316227.76,
          "dataCompleteness":0.85,
          "portfolioConcentrationStatus":"OK",
          "calculatedAt":"2026-03-24T10:00:00Z",
          "positionRisks":[
            {"instrumentId":"AAPL","tier":"HIGH_LIQUID","horizonDays":1,"advMissing":false}
          ]
        }
    """.trimIndent()

    test("gateway routing to liquidity risk endpoints — POST /api/v1/books/{bookId}/liquidity-risk with valid baseVar — returns 200 with liquidity risk response") {
        val backend = BackendStubServer {
            post("/api/v1/books/BOOK-1/liquidity-risk") {
                call.respond(Json.parseToJsonElement(sampleResponseJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.post("/api/v1/books/BOOK-1/liquidity-risk") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"baseVar": 50000.0}""")
                }

                response.status shouldBe HttpStatusCode.OK

                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["bookId"]?.jsonPrimitive?.content shouldBe "BOOK-1"
                body.containsKey("portfolioLvar") shouldBe true
                body.containsKey("dataCompleteness") shouldBe true
                body.containsKey("portfolioConcentrationStatus") shouldBe true
                body.containsKey("positionRisks") shouldBe true

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/books/BOOK-1/liquidity-risk" }
                recorded.method shouldBe "POST"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to liquidity risk endpoints — POST /api/v1/books/{bookId}/liquidity-risk when book has no positions — returns 204 No Content") {
        val backend = BackendStubServer {
            post("/api/v1/books/EMPTY-BOOK/liquidity-risk") {
                call.respond(HttpStatusCode.NoContent)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.post("/api/v1/books/EMPTY-BOOK/liquidity-risk") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"baseVar": 50000.0}""")
                }

                response.status shouldBe HttpStatusCode.NoContent
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to liquidity risk endpoints — GET /api/v1/books/{bookId}/liquidity-risk/latest with existing snapshot — returns 200 with the latest snapshot") {
        val backend = BackendStubServer {
            get("/api/v1/books/BOOK-1/liquidity-risk/latest") {
                call.respond(Json.parseToJsonElement(sampleResponseJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/books/BOOK-1/liquidity-risk/latest")

                response.status shouldBe HttpStatusCode.OK

                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["bookId"]?.jsonPrimitive?.content shouldBe "BOOK-1"
                body.containsKey("portfolioLvar") shouldBe true
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to liquidity risk endpoints — GET /api/v1/books/{bookId}/liquidity-risk/latest when no snapshot exists — returns 404") {
        val backend = BackendStubServer {
            get("/api/v1/books/UNKNOWN/liquidity-risk/latest") {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/books/UNKNOWN/liquidity-risk/latest")

                response.status shouldBe HttpStatusCode.NotFound
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to liquidity risk endpoints — GET /api/v1/books/{bookId}/liquidity-risk returns history — returns 200 with array of snapshots") {
        val historyJson = """[$sampleResponseJson,$sampleResponseJson]"""
        val backend = BackendStubServer {
            get("/api/v1/books/BOOK-1/liquidity-risk") {
                call.respond(Json.parseToJsonElement(historyJson).jsonArray)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/books/BOOK-1/liquidity-risk")

                response.status shouldBe HttpStatusCode.OK

                val arr = Json.parseToJsonElement(response.bodyAsText()).jsonArray
                arr.size shouldBe 2
                arr[0].jsonObject["bookId"]?.jsonPrimitive?.content shouldBe "BOOK-1"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }
})
