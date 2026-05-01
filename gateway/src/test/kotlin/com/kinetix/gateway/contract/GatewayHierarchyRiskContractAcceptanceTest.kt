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

class GatewayHierarchyRiskContractAcceptanceTest : FunSpec({

    val firmNodeJson = """
        {
          "level":"FIRM","entityId":"FIRM","entityName":"FIRM",
          "parentId":null,"varValue":"2500000.00",
          "expectedShortfall":"3125000.00","pnlToday":null,
          "limitUtilisation":null,"marginalVar":null,"incrementalVar":null,
          "topContributors":[
            {"entityId":"div-equities","entityName":"Equities",
             "varContribution":"1500000.00","pctOfTotal":"60.00"}
          ],
          "childCount":2,"isPartial":false,"missingBooks":[]
        }
    """.trimIndent()

    test("gateway routing to hierarchy risk endpoints — GET /api/v1/risk/hierarchy/FIRM/FIRM with a resolved node — returns 200 with node shape proxied from risk-orchestrator") {
        val backend = BackendStubServer {
            get("/api/v1/risk/hierarchy/FIRM/FIRM") {
                call.respond(Json.parseToJsonElement(firmNodeJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/risk/hierarchy/FIRM/FIRM")

                response.status shouldBe HttpStatusCode.OK

                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["level"]?.jsonPrimitive?.content shouldBe "FIRM"
                body["entityId"]?.jsonPrimitive?.content shouldBe "FIRM"
                body.containsKey("varValue") shouldBe true
                body.containsKey("topContributors") shouldBe true
                body.containsKey("isPartial") shouldBe true
                body.containsKey("missingBooks") shouldBe true
                body["childCount"]?.jsonPrimitive?.int shouldBe 2

                val contributors = body["topContributors"]!!.jsonArray
                contributors.size shouldBe 1
                contributors[0].jsonObject["entityId"]?.jsonPrimitive?.content shouldBe "div-equities"

                val recorded = backend.recordedRequests.single { it.path == "/api/v1/risk/hierarchy/FIRM/FIRM" }
                recorded.method shouldBe "GET"
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to hierarchy risk endpoints — GET /api/v1/risk/hierarchy/DESK/desk-rates when entity is not found — returns 404") {
        val backend = BackendStubServer {
            get("/api/v1/risk/hierarchy/DESK/desk-rates") {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/risk/hierarchy/DESK/desk-rates")
                response.status shouldBe HttpStatusCode.NotFound
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }

    test("gateway routing to hierarchy risk endpoints — GET /api/v1/risk/hierarchy/DIVISION/div-equities returns partial result — isPartial flag and missingBooks are present in response") {
        val partialNodeJson = """
            {
              "level":"DIVISION","entityId":"div-equities","entityName":"Equities",
              "parentId":"FIRM","varValue":"800000.00",
              "expectedShortfall":null,"pnlToday":null,
              "limitUtilisation":null,"marginalVar":null,"incrementalVar":null,
              "topContributors":[],"childCount":3,
              "isPartial":true,"missingBooks":["book-x","book-y"]
            }
        """.trimIndent()
        val backend = BackendStubServer {
            get("/api/v1/risk/hierarchy/DIVISION/div-equities") {
                call.respond(Json.parseToJsonElement(partialNodeJson).jsonObject)
            }
        }
        val httpClient = HttpClient(CIO) { install(ClientContentNegotiation) { json() } }
        try {
            val riskClient = HttpRiskServiceClient(httpClient, backend.baseUrl)

            testApplication {
                application { module(riskClient) }
                val response = client.get("/api/v1/risk/hierarchy/DIVISION/div-equities")

                response.status shouldBe HttpStatusCode.OK

                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["isPartial"]?.jsonPrimitive?.boolean shouldBe true
                body["missingBooks"]?.jsonArray?.size shouldBe 2
            }
        } finally {
            httpClient.close()
            backend.close()
        }
    }
})
