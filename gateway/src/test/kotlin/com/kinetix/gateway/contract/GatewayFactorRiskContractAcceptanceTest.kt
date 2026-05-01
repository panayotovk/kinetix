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

class GatewayFactorRiskContractAcceptanceTest : FunSpec({

    val sampleSnapshotJson = """
        {
          "bookId":"BOOK-1",
          "calculatedAt":"2026-03-24T10:00:00Z",
          "totalVar":50000.0,
          "systematicVar":38000.0,
          "idiosyncraticVar":12000.0,
          "rSquared":0.576,
          "concentrationWarning":false,
          "factors":[
            {"factorType":"EQUITY_BETA","varContribution":30000.0,"pctOfTotal":0.60,"loading":1.2,"loadingMethod":"OLS_REGRESSION"}
          ]
        }
    """.trimIndent()

    test("gateway routing to factor risk endpoints — GET /api/v1/books/{bookId}/factor-risk/latest with existing snapshot — returns 200 with the factor decomposition snapshot") {
        val backend = BackendStubServer {
            get("/api/v1/books/BOOK-1/factor-risk/latest") {
                call.respond(Json.parseToJsonElement(sampleSnapshotJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/books/BOOK-1/factor-risk/latest")

                response.status shouldBe HttpStatusCode.OK

                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["bookId"]?.jsonPrimitive?.content shouldBe "BOOK-1"
                body.containsKey("totalVar") shouldBe true
                body.containsKey("systematicVar") shouldBe true
                body.containsKey("idiosyncraticVar") shouldBe true
                body.containsKey("rSquared") shouldBe true
                body.containsKey("concentrationWarning") shouldBe true
                body.containsKey("factors") shouldBe true

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/books/BOOK-1/factor-risk/latest" }
                recorded.method shouldBe "GET"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to factor risk endpoints — GET /api/v1/books/{bookId}/factor-risk/latest when no snapshot exists — returns 404") {
        val backend = BackendStubServer {
            get("/api/v1/books/UNKNOWN/factor-risk/latest") {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/books/UNKNOWN/factor-risk/latest")

                response.status shouldBe HttpStatusCode.NotFound
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to factor risk endpoints — GET /api/v1/books/{bookId}/factor-risk returns history — returns 200 with array of snapshots") {
        val olderSnapshotJson = """{"bookId":"BOOK-1","totalVar":40000.0,"calculatedAt":"2026-03-23T10:00:00Z"}"""
        val historyJson = """[$sampleSnapshotJson,$olderSnapshotJson]"""
        val backend = BackendStubServer {
            get("/api/v1/books/BOOK-1/factor-risk") {
                call.respond(Json.parseToJsonElement(historyJson).jsonArray)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/books/BOOK-1/factor-risk")

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
