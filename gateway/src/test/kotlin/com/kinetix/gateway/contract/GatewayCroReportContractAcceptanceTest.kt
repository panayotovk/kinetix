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
import io.ktor.server.routing.post
import io.ktor.server.testing.*
import kotlinx.serialization.json.*

class GatewayCroReportContractAcceptanceTest : FunSpec({

    val reportJson = """
        {
          "level":"FIRM","entityId":"FIRM","entityName":"FIRM",
          "parentId":null,"varValue":"2000000.00",
          "expectedShortfall":"2500000.00","pnlToday":null,
          "limitUtilisation":"40.00","marginalVar":null,"incrementalVar":null,
          "topContributors":[
            {"entityId":"div-equities","entityName":"Equities",
             "varContribution":"1200000.00","pctOfTotal":"60.00"}
          ],
          "childCount":2,"isPartial":false,"missingBooks":[]
        }
    """.trimIndent()

    test("gateway routing to CRO report endpoint — POST /api/v1/risk/reports/cro returns a report — returns 200 with report body proxied from risk-orchestrator") {
        val backend = BackendStubServer {
            post("/api/v1/risk/reports/cro") {
                call.respond(Json.parseToJsonElement(reportJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.post("/api/v1/risk/reports/cro")

                response.status shouldBe HttpStatusCode.OK

                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["level"]?.jsonPrimitive?.content shouldBe "FIRM"
                body["varValue"]?.jsonPrimitive?.content shouldBe "2000000.00"
                body["limitUtilisation"]?.jsonPrimitive?.content shouldBe "40.00"

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/risk/reports/cro" }
                recorded.method shouldBe "POST"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to CRO report endpoint — POST /api/v1/risk/reports/cro and risk-orchestrator returns 503 — returns 503 to the client") {
        val backend = BackendStubServer {
            post("/api/v1/risk/reports/cro") {
                call.respond(HttpStatusCode.ServiceUnavailable)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.post("/api/v1/risk/reports/cro")

                response.status shouldBe HttpStatusCode.ServiceUnavailable
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }
})
