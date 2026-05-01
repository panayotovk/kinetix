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

class GatewayKrdContractAcceptanceTest : FunSpec({

    val krdResponseJson = """
        {
          "bookId":"BOOK-1",
          "instruments":[
            {
              "instrumentId":"UST-10Y",
              "totalDv01":"854.02",
              "krdBuckets":[
                {"tenorLabel":"2Y","tenorDays":730,"dv01":"45.12"},
                {"tenorLabel":"5Y","tenorDays":1825,"dv01":"120.34"},
                {"tenorLabel":"10Y","tenorDays":3650,"dv01":"680.55"},
                {"tenorLabel":"30Y","tenorDays":10950,"dv01":"8.01"}
              ]
            }
          ],
          "aggregated":[
            {"tenorLabel":"2Y","tenorDays":730,"dv01":"45.12"},
            {"tenorLabel":"10Y","tenorDays":3650,"dv01":"680.55"}
          ]
        }
    """.trimIndent()

    test("gateway routing to KRD endpoint — GET /api/v1/risk/krd/{bookId} returns 200") {
        val backend = BackendStubServer {
            get("/api/v1/risk/krd/BOOK-1") {
                call.respond(Json.parseToJsonElement(krdResponseJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/risk/krd/BOOK-1")

                response.status shouldBe HttpStatusCode.OK

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/risk/krd/BOOK-1" }
                recorded.method shouldBe "GET"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to KRD endpoint — GET /api/v1/risk/krd/{bookId} response body contains bookId matching requested book") {
        val backend = BackendStubServer {
            get("/api/v1/risk/krd/BOOK-1") {
                call.respond(Json.parseToJsonElement(krdResponseJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/risk/krd/BOOK-1")
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

                body["bookId"]?.jsonPrimitive?.content shouldBe "BOOK-1"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to KRD endpoint — GET /api/v1/risk/krd/{bookId} response body contains instruments array") {
        val backend = BackendStubServer {
            get("/api/v1/risk/krd/BOOK-1") {
                call.respond(Json.parseToJsonElement(krdResponseJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/risk/krd/BOOK-1")
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

                body.containsKey("instruments") shouldBe true
                body["instruments"]!!.jsonArray.size shouldBe 1
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to KRD endpoint — GET /api/v1/risk/krd/{bookId} response body contains aggregated buckets") {
        val backend = BackendStubServer {
            get("/api/v1/risk/krd/BOOK-1") {
                call.respond(Json.parseToJsonElement(krdResponseJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/risk/krd/BOOK-1")
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

                body.containsKey("aggregated") shouldBe true
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to KRD endpoint — GET /api/v1/risk/krd/{bookId} when upstream returns null — returns 404") {
        val backend = BackendStubServer {
            get("/api/v1/risk/krd/UNKNOWN") {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/risk/krd/UNKNOWN")

                response.status shouldBe HttpStatusCode.NotFound
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }
})
