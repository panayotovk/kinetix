package com.kinetix.gateway.routes

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
import io.mockk.mockk
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val saCcrResponse = buildJsonObject {
    put("nettingSetId", "CP-GS-SA-CCR")
    put("counterpartyId", "CP-GS")
    put("replacementCost", 100_000.0)
    put("pfeAddon", 525_000.0)
    put("multiplier", 1.0)
    put("ead", 875_000.0)
    put("alpha", 1.4)
}

class SaCcrRoutesTest : FunSpec({

    val riskClient = mockk<RiskServiceClient>()

    beforeEach {
        clearMocks(riskClient)
        coEvery { riskClient.calculateVaR(any()) } returns null
        coEvery { riskClient.getLatestVaR(any()) } returns null
    }

    test("GET /api/v1/counterparty/{id}/sa-ccr proxies to risk-orchestrator and returns SA-CCR result") {
        coEvery { riskClient.getCounterpartySaCcr("CP-GS", 0.0) } returns saCcrResponse

        testApplication {
            application { module(riskClient) }
            val response = client.get("/api/v1/counterparty/CP-GS/sa-ccr")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText().contains("ead") shouldBe true
        }
    }

    test("GET /api/v1/counterparty/{id}/sa-ccr returns 404 when counterparty not found") {
        coEvery { riskClient.getCounterpartySaCcr("CP-MISSING", 0.0) } returns null

        testApplication {
            application { module(riskClient) }
            val response = client.get("/api/v1/counterparty/CP-MISSING/sa-ccr")
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    test("GET /api/v1/counterparty/{id}/sa-ccr passes collateral query param through") {
        coEvery { riskClient.getCounterpartySaCcr("CP-GS", 500_000.0) } returns saCcrResponse

        testApplication {
            application { module(riskClient) }
            val response = client.get("/api/v1/counterparty/CP-GS/sa-ccr?collateral=500000.0")
            response.status shouldBe HttpStatusCode.OK
        }
    }
})
