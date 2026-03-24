package com.kinetix.gateway.contract

import com.kinetix.gateway.client.RiskServiceClient
import com.kinetix.gateway.module
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.serialization.json.*

class GatewayHierarchyRiskContractAcceptanceTest : BehaviorSpec({

    val riskClient = mockk<RiskServiceClient>()

    beforeEach { clearMocks(riskClient) }

    given("gateway routing to hierarchy risk endpoints") {

        `when`("GET /api/v1/risk/hierarchy/FIRM/FIRM with a resolved node") {
            then("returns 200 with node shape proxied from risk-orchestrator") {
                val firmNode = Json.parseToJsonElement(
                    """
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
                ).jsonObject

                coEvery { riskClient.getHierarchyRisk("FIRM", "FIRM") } returns firmNode

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
                }
            }
        }

        `when`("GET /api/v1/risk/hierarchy/DESK/desk-rates when entity is not found") {
            then("returns 404") {
                coEvery { riskClient.getHierarchyRisk("DESK", "desk-rates") } returns null

                testApplication {
                    application { module(riskClient) }
                    val response = client.get("/api/v1/risk/hierarchy/DESK/desk-rates")
                    response.status shouldBe HttpStatusCode.NotFound
                }
            }
        }

        `when`("GET /api/v1/risk/hierarchy/DIVISION/div-equities returns partial result") {
            then("isPartial flag and missingBooks are present in response") {
                val partialNode = Json.parseToJsonElement(
                    """
                    {
                      "level":"DIVISION","entityId":"div-equities","entityName":"Equities",
                      "parentId":"FIRM","varValue":"800000.00",
                      "expectedShortfall":null,"pnlToday":null,
                      "limitUtilisation":null,"marginalVar":null,"incrementalVar":null,
                      "topContributors":[],"childCount":3,
                      "isPartial":true,"missingBooks":["book-x","book-y"]
                    }
                    """.trimIndent()
                ).jsonObject

                coEvery { riskClient.getHierarchyRisk("DIVISION", "div-equities") } returns partialNode

                testApplication {
                    application { module(riskClient) }
                    val response = client.get("/api/v1/risk/hierarchy/DIVISION/div-equities")

                    response.status shouldBe HttpStatusCode.OK

                    val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                    body["isPartial"]?.jsonPrimitive?.boolean shouldBe true
                    body["missingBooks"]?.jsonArray?.size shouldBe 2
                }
            }
        }
    }
})
