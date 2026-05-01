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

class GatewayPositionRiskContractAcceptanceTest : FunSpec({

    val positionRiskJson = """
        [
          {
            "instrumentId":"AAPL",
            "assetClass":"EQUITY",
            "marketValue":"150000.00",
            "delta":"0.850000",
            "gamma":"0.012000",
            "vega":"45.300000",
            "varContribution":"3200.00",
            "esContribution":"4100.00",
            "percentageOfTotal":"64.00"
          }
        ]
    """.trimIndent()

    test("gateway routing to risk-orchestrator — GET /api/v1/risk/positions/{bookId} with position risk data — returns 200 with expected fields") {
        val backend = BackendStubServer {
            get("/api/v1/risk/positions/equity-growth") {
                call.respond(Json.parseToJsonElement(positionRiskJson).jsonArray)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/risk/positions/equity-growth")

                response.status shouldBe HttpStatusCode.OK
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
                body.size shouldBe 1

                val item = body[0].jsonObject
                item.containsKey("instrumentId") shouldBe true
                item.containsKey("assetClass") shouldBe true
                item.containsKey("marketValue") shouldBe true
                item.containsKey("delta") shouldBe true
                item.containsKey("gamma") shouldBe true
                item.containsKey("vega") shouldBe true
                item.containsKey("varContribution") shouldBe true
                item.containsKey("esContribution") shouldBe true
                item.containsKey("percentageOfTotal") shouldBe true

                item["instrumentId"]?.jsonPrimitive?.content shouldBe "AAPL"
                item["assetClass"]?.jsonPrimitive?.content shouldBe "EQUITY"
                item["marketValue"]?.jsonPrimitive?.content shouldBe "150000.00"
                item["varContribution"]?.jsonPrimitive?.content shouldBe "3200.00"
                item["esContribution"]?.jsonPrimitive?.content shouldBe "4100.00"
                item["percentageOfTotal"]?.jsonPrimitive?.content shouldBe "64.00"

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/risk/positions/equity-growth" }
                recorded.method shouldBe "GET"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to risk-orchestrator — GET /api/v1/risk/positions/{bookId} with no position risk data — returns 404") {
        val backend = BackendStubServer {
            get("/api/v1/risk/positions/empty-portfolio") {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/risk/positions/empty-portfolio")

                response.status shouldBe HttpStatusCode.NotFound
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }
})
