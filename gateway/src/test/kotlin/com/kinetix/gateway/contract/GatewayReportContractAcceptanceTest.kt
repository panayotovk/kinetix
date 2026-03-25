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

class GatewayReportContractAcceptanceTest : BehaviorSpec({

    val riskClient = mockk<RiskServiceClient>()

    beforeEach { clearMocks(riskClient) }

    val templateResponse = buildJsonArray {
        addJsonObject {
            put("templateId", "tpl-risk-summary")
            put("name", "Risk Summary")
            put("templateType", "RISK_SUMMARY")
            put("ownerUserId", "SYSTEM")
            put("description", "Per-book VaR and Greeks")
            put("source", "risk_positions_flat")
        }
    }

    val outputResponse = buildJsonObject {
        put("outputId", "out-abc")
        put("templateId", "tpl-risk-summary")
        put("generatedAt", "2025-01-15T10:00:00Z")
        put("outputFormat", "JSON")
        put("rowCount", 2)
    }

    given("gateway routing to report endpoints") {

        `when`("GET /api/v1/reports/templates") {
            then("returns 200 with list of templates") {
                coEvery { riskClient.listReportTemplates() } returns templateResponse

                testApplication {
                    application { module(riskClient) }
                    val response = client.get("/api/v1/reports/templates")

                    response.status shouldBe HttpStatusCode.OK
                    val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
                    body.size shouldBe 1
                    body[0].jsonObject["templateId"]?.jsonPrimitive?.content shouldBe "tpl-risk-summary"
                }
            }
        }

        `when`("POST /api/v1/reports/generate with valid request") {
            then("returns 200 with report output") {
                coEvery { riskClient.generateReport(any()) } returns outputResponse

                testApplication {
                    application { module(riskClient) }
                    val response = client.post("/api/v1/reports/generate") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"templateId":"tpl-risk-summary","bookId":"BOOK-1","format":"JSON"}""")
                    }

                    response.status shouldBe HttpStatusCode.OK
                    val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                    body["outputId"]?.jsonPrimitive?.content shouldBe "out-abc"
                }
            }
        }

        `when`("GET /api/v1/reports/{outputId} for existing output") {
            then("returns 200 with the output") {
                coEvery { riskClient.getReportOutput("out-abc") } returns outputResponse

                testApplication {
                    application { module(riskClient) }
                    val response = client.get("/api/v1/reports/out-abc")

                    response.status shouldBe HttpStatusCode.OK
                    val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                    body["outputId"]?.jsonPrimitive?.content shouldBe "out-abc"
                }
            }
        }

        `when`("GET /api/v1/reports/{outputId} for missing output") {
            then("returns 404") {
                coEvery { riskClient.getReportOutput("missing") } returns null

                testApplication {
                    application { module(riskClient) }
                    val response = client.get("/api/v1/reports/missing")

                    response.status shouldBe HttpStatusCode.NotFound
                }
            }
        }

        `when`("GET /api/v1/reports/{outputId}/csv for existing output") {
            then("returns 200 with CSV text") {
                coEvery { riskClient.getReportOutputCsv("out-abc") } returns "book_id,instrument_id\nBOOK-1,AAPL"

                testApplication {
                    application { module(riskClient) }
                    val response = client.get("/api/v1/reports/out-abc/csv")

                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldBe "book_id,instrument_id\nBOOK-1,AAPL"
                }
            }
        }

        `when`("GET /api/v1/reports/{outputId}/csv for missing output") {
            then("returns 404") {
                coEvery { riskClient.getReportOutputCsv("missing") } returns null

                testApplication {
                    application { module(riskClient) }
                    val response = client.get("/api/v1/reports/missing/csv")

                    response.status shouldBe HttpStatusCode.NotFound
                }
            }
        }
    }
})
