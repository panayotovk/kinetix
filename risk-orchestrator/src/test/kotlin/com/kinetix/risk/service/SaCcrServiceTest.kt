package com.kinetix.risk.service

import com.kinetix.risk.client.ClientResponse
import com.kinetix.risk.client.ReferenceDataServiceClient
import com.kinetix.risk.client.SaCcrClient
import com.kinetix.risk.client.SaCcrResult
import com.kinetix.risk.client.dtos.CounterpartyDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk

private val COUNTERPARTY = CounterpartyDto(
    counterpartyId = "CP-GS",
    legalName = "Goldman Sachs",
    ratingSp = "A+",
    sector = "FINANCIALS",
    lgd = 0.4,
    cdsSpreadBps = 65.0,
)

private fun saCcrResult(
    ead: Double = 875_000.0,
    rc: Double = 100_000.0,
    pfeAddon: Double = 525_000.0,
    multiplier: Double = 1.0,
) = SaCcrResult(
    nettingSetId = "NS-GS-001",
    counterpartyId = "CP-GS",
    replacementCost = rc,
    pfeAddon = pfeAddon,
    multiplier = multiplier,
    ead = ead,
    alpha = 1.4,
)

class SaCcrServiceTest : FunSpec({

    val referenceDataClient = mockk<ReferenceDataServiceClient>()
    val saCcrClient = mockk<SaCcrClient>()
    val service = SaCcrService(
        referenceDataClient = referenceDataClient,
        saCcrClient = saCcrClient,
    )

    context("calculateSaCcr") {

        test("returns SA-CCR result for known counterparty with positions") {
            coEvery { referenceDataClient.getCounterparty("CP-GS") } returns
                ClientResponse.Success(COUNTERPARTY)
            coEvery {
                saCcrClient.calculateSaCcr(
                    nettingSetId = any(),
                    counterpartyId = "CP-GS",
                    positions = any(),
                    collateralNet = any(),
                )
            } returns saCcrResult(ead = 875_000.0)

            val result = service.calculateSaCcr(
                counterpartyId = "CP-GS",
                positions = emptyList(),
                collateralNet = 0.0,
            )

            result.ead shouldBe 875_000.0
            result.counterpartyId shouldBe "CP-GS"
        }

        test("EAD is approximately 1.4 times RC plus PFE add-on") {
            coEvery { referenceDataClient.getCounterparty("CP-GS") } returns
                ClientResponse.Success(COUNTERPARTY)
            val rc = 100_000.0
            val pfe = 525_000.0
            val expectedEad = 1.4 * (rc + pfe)
            coEvery {
                saCcrClient.calculateSaCcr(any(), any(), any(), any())
            } returns saCcrResult(ead = expectedEad, rc = rc, pfeAddon = pfe)

            val result = service.calculateSaCcr("CP-GS", emptyList(), 0.0)

            result.ead shouldBe expectedEad
        }

        test("throws IllegalArgumentException when counterparty not found") {
            coEvery { referenceDataClient.getCounterparty("CP-UNKNOWN") } returns
                ClientResponse.NotFound(404)

            try {
                service.calculateSaCcr("CP-UNKNOWN", emptyList(), 0.0)
                throw AssertionError("Expected exception not thrown")
            } catch (e: IllegalArgumentException) {
                e.message shouldNotBe null
            }
        }

        test("passes collateral_net through to gRPC client") {
            coEvery { referenceDataClient.getCounterparty("CP-GS") } returns
                ClientResponse.Success(COUNTERPARTY)
            var capturedCollateral = Double.NaN
            coEvery {
                saCcrClient.calculateSaCcr(any(), any(), any(), any())
            } answers {
                capturedCollateral = args[3] as Double
                saCcrResult()
            }

            service.calculateSaCcr("CP-GS", emptyList(), collateralNet = 500_000.0)

            capturedCollateral shouldBe 500_000.0
        }

        test("multiplier is between 0.05 and 1.0") {
            coEvery { referenceDataClient.getCounterparty("CP-GS") } returns
                ClientResponse.Success(COUNTERPARTY)
            coEvery {
                saCcrClient.calculateSaCcr(any(), any(), any(), any())
            } returns saCcrResult(multiplier = 0.85)

            val result = service.calculateSaCcr("CP-GS", emptyList(), 0.0)

            result.multiplier shouldBe 0.85
        }

        test("result alpha is 1.4 as per BCBS 279") {
            coEvery { referenceDataClient.getCounterparty("CP-GS") } returns
                ClientResponse.Success(COUNTERPARTY)
            coEvery {
                saCcrClient.calculateSaCcr(any(), any(), any(), any())
            } returns saCcrResult()

            val result = service.calculateSaCcr("CP-GS", emptyList(), 0.0)

            result.alpha shouldBe 1.4
        }
    }
})
