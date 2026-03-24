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

class GatewayCroReportContractAcceptanceTest : BehaviorSpec({

    val riskClient = mockk<RiskServiceClient>()

    beforeEach { clearMocks(riskClient) }

    given("gateway routing to CRO report endpoint") {

        `when`("POST /api/v1/risk/reports/cro returns a report") {
            then("returns 200 with report body proxied from risk-orchestrator") {
                val reportNode = Json.parseToJsonElement(
                    """
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
                ).jsonObject

                coEvery { riskClient.triggerCroReport() } returns reportNode

                testApplication {
                    application { module(riskClient) }
                    val response = client.post("/api/v1/risk/reports/cro")

                    response.status shouldBe HttpStatusCode.OK

                    val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                    body["level"]?.jsonPrimitive?.content shouldBe "FIRM"
                    body["varValue"]?.jsonPrimitive?.content shouldBe "2000000.00"
                    body["limitUtilisation"]?.jsonPrimitive?.content shouldBe "40.00"
                }
            }
        }

        `when`("POST /api/v1/risk/reports/cro and risk-orchestrator returns 503") {
            then("returns 503 to the client") {
                coEvery { riskClient.triggerCroReport() } returns null

                testApplication {
                    application { module(riskClient) }
                    val response = client.post("/api/v1/risk/reports/cro")

                    response.status shouldBe HttpStatusCode.ServiceUnavailable
                }
            }
        }
    }
})
