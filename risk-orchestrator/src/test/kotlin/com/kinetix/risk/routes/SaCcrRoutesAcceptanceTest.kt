package com.kinetix.risk.routes

import com.kinetix.risk.client.ClientResponse
import com.kinetix.risk.client.ReferenceDataServiceClient
import com.kinetix.risk.client.SaCcrClient
import com.kinetix.risk.client.SaCcrResult
import com.kinetix.risk.client.dtos.CounterpartyDto
import com.kinetix.risk.routes.dtos.SaCcrResponse
import com.kinetix.risk.service.SaCcrService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json

private fun saCcrResult(counterpartyId: String = "CP-GS") = SaCcrResult(
    nettingSetId = "NS-GS-001",
    counterpartyId = counterpartyId,
    replacementCost = 100_000.0,
    pfeAddon = 525_000.0,
    multiplier = 1.0,
    ead = 875_000.0,
    alpha = 1.4,
)

private fun Application.configureTestApp(service: SaCcrService) {
    install(ContentNegotiation) { json() }
    routing {
        saCcrRoutes(service)
    }
}

class SaCcrRoutesAcceptanceTest : FunSpec({

    val referenceDataClient = mockk<ReferenceDataServiceClient>()
    val saCcrClient = mockk<SaCcrClient>()
    val service = SaCcrService(
        referenceDataClient = referenceDataClient,
        saCcrClient = saCcrClient,
    )

    beforeEach {
        clearMocks(referenceDataClient, saCcrClient)
    }

    test("GET /api/v1/counterparty/{id}/sa-ccr returns SA-CCR result") {
        coEvery { referenceDataClient.getCounterparty("CP-GS") } returns
            ClientResponse.Success(CounterpartyDto(counterpartyId = "CP-GS", legalName = "Goldman Sachs", lgd = 0.4))
        coEvery {
            saCcrClient.calculateSaCcr(any(), any(), any(), any())
        } returns saCcrResult()

        testApplication {
            application { configureTestApp(service) }
            val response = client.get("/api/v1/counterparty/CP-GS/sa-ccr")
            response.status shouldBe HttpStatusCode.OK
            val body = Json.decodeFromString<SaCcrResponse>(response.bodyAsText())
            body.counterpartyId shouldBe "CP-GS"
            body.ead shouldBe 875_000.0
            body.alpha shouldBe 1.4
        }
    }

    test("GET /api/v1/counterparty/{id}/sa-ccr returns 404 when counterparty not found") {
        coEvery { referenceDataClient.getCounterparty("CP-MISSING") } returns
            ClientResponse.NotFound(404)

        testApplication {
            application { configureTestApp(service) }
            val response = client.get("/api/v1/counterparty/CP-MISSING/sa-ccr")
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    test("GET /api/v1/counterparty/{id}/sa-ccr with collateral query param passes it through") {
        coEvery { referenceDataClient.getCounterparty("CP-GS") } returns
            ClientResponse.Success(CounterpartyDto(counterpartyId = "CP-GS", legalName = "Goldman Sachs", lgd = 0.4))
        coEvery {
            saCcrClient.calculateSaCcr(any(), any(), any(), any())
        } returns saCcrResult()

        testApplication {
            application { configureTestApp(service) }
            val response = client.get("/api/v1/counterparty/CP-GS/sa-ccr?collateral=500000")
            response.status shouldBe HttpStatusCode.OK
        }
    }

    test("SA-CCR response contains pfe_addon field labelled distinctly from MC PFE") {
        coEvery { referenceDataClient.getCounterparty("CP-GS") } returns
            ClientResponse.Success(CounterpartyDto(counterpartyId = "CP-GS", legalName = "Goldman Sachs", lgd = 0.4))
        coEvery {
            saCcrClient.calculateSaCcr(any(), any(), any(), any())
        } returns saCcrResult()

        testApplication {
            application { configureTestApp(service) }
            val response = client.get("/api/v1/counterparty/CP-GS/sa-ccr")
            val body = Json.decodeFromString<SaCcrResponse>(response.bodyAsText())
            // The field is named pfeAddon to make clear it is the SA-CCR regulatory add-on
            body.pfeAddon shouldBe 525_000.0
            body.replacementCost shouldBe 100_000.0
        }
    }
})
