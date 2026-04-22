package com.kinetix.gateway.contract

import com.kinetix.gateway.client.RiskServiceClient
import com.kinetix.gateway.module
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.*

class GatewayMarketRegimeContractAcceptanceTest : FunSpec({

    val riskClient = mockk<RiskServiceClient>()

    beforeEach { clearMocks(riskClient) }

    val currentRegimeResponse = buildJsonObject {
        put("regime", "ELEVATED_VOL")
        put("previousRegime", "NORMAL")
        put("transitionedAt", "2025-01-15T10:00:00Z")
        put("confidence", "0.92")
        put("degradedInputs", false)
        put("effectiveCalculationType", "MONTE_CARLO")
        put("effectiveConfidenceLevel", "CL_99")
        put("effectiveTimeHorizonDays", 1)
        put("effectiveCorrelationMethod", "EWMA")
    }

    val regimeHistoryResponse = buildJsonObject {
        putJsonArray("transitions") {
            addJsonObject {
                put("regime", "NORMAL")
                put("previousRegime", "ELEVATED_VOL")
                put("transitionedAt", "2025-01-14T09:00:00Z")
            }
            addJsonObject {
                put("regime", "ELEVATED_VOL")
                put("previousRegime", "NORMAL")
                put("transitionedAt", "2025-01-15T10:00:00Z")
            }
        }
    }

    test("GET /api/v1/risk/regime/current returns the upstream payload as JSON") {
        coEvery { riskClient.getCurrentRegime() } returns currentRegimeResponse

        testApplication {
            application { module(riskClient) }
            val response = client.get("/api/v1/risk/regime/current")

            response.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["regime"]?.jsonPrimitive?.content shouldBe "ELEVATED_VOL"
            body["previousRegime"]?.jsonPrimitive?.content shouldBe "NORMAL"
            body["effectiveCalculationType"]?.jsonPrimitive?.content shouldBe "MONTE_CARLO"
        }
    }

    test("GET /api/v1/risk/regime/history defaults to limit=50 when no query param is provided") {
        coEvery { riskClient.getRegimeHistory(50) } returns regimeHistoryResponse

        testApplication {
            application { module(riskClient) }
            val response = client.get("/api/v1/risk/regime/history")

            response.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["transitions"]?.jsonArray?.size shouldBe 2
            coVerify(exactly = 1) { riskClient.getRegimeHistory(50) }
        }
    }

    test("GET /api/v1/risk/regime/history with limit=10 forwards the coerced limit to the client") {
        coEvery { riskClient.getRegimeHistory(10) } returns regimeHistoryResponse

        testApplication {
            application { module(riskClient) }
            val response = client.get("/api/v1/risk/regime/history?limit=10")

            response.status shouldBe HttpStatusCode.OK
            coVerify(exactly = 1) { riskClient.getRegimeHistory(10) }
        }
    }

    test("GET /api/v1/risk/regime/history coerces an out-of-range limit to 500 (upper bound)") {
        coEvery { riskClient.getRegimeHistory(500) } returns regimeHistoryResponse

        testApplication {
            application { module(riskClient) }
            val response = client.get("/api/v1/risk/regime/history?limit=9999")

            response.status shouldBe HttpStatusCode.OK
            coVerify(exactly = 1) { riskClient.getRegimeHistory(500) }
        }
    }

    test("GET /api/v1/risk/regime/history coerces a non-numeric limit to the default of 50") {
        coEvery { riskClient.getRegimeHistory(50) } returns regimeHistoryResponse

        testApplication {
            application { module(riskClient) }
            val response = client.get("/api/v1/risk/regime/history?limit=abc")

            response.status shouldBe HttpStatusCode.OK
            coVerify(exactly = 1) { riskClient.getRegimeHistory(50) }
        }
    }
})
