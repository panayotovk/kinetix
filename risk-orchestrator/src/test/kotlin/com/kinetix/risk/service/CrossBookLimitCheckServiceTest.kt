package com.kinetix.risk.service

import com.kinetix.common.model.AssetClass
import com.kinetix.common.model.BookId
import com.kinetix.risk.client.ClientResponse
import com.kinetix.risk.client.LimitServiceClient
import com.kinetix.risk.client.dtos.LimitDefinitionDto
import com.kinetix.risk.model.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.Instant

private fun crossBookResult(
    groupId: String = "desk-alpha",
    varValue: Double = 50_000.0,
) = CrossBookValuationResult(
    portfolioGroupId = groupId,
    bookIds = listOf(BookId("book-1"), BookId("book-2")),
    calculationType = CalculationType.PARAMETRIC,
    confidenceLevel = ConfidenceLevel.CL_95,
    varValue = varValue,
    expectedShortfall = varValue * 1.25,
    componentBreakdown = listOf(ComponentBreakdown(AssetClass.EQUITY, varValue, 100.0)),
    bookContributions = emptyList(),
    totalStandaloneVar = varValue * 1.2,
    diversificationBenefit = varValue * 0.2,
    calculatedAt = Instant.now(),
)

private fun varLimit(
    entityId: String = "desk-alpha",
    limitValue: String = "100000",
    level: String = "DESK",
    active: Boolean = true,
) = LimitDefinitionDto(
    id = "lim-1",
    level = level,
    entityId = entityId,
    limitType = "VAR",
    limitValue = limitValue,
    active = active,
)

class CrossBookLimitCheckServiceTest : FunSpec({

    val limitClient = mockk<LimitServiceClient>()
    val service = CrossBookLimitCheckService(limitClient, warningThresholdPct = 0.8)

    beforeEach { clearMocks(limitClient) }

    test("returns OK when aggregated VaR is below limit") {
        coEvery { limitClient.getLimits() } returns ClientResponse.Success(
            listOf(varLimit(limitValue = "100000")),
        )

        val results = service.checkLimits(crossBookResult(varValue = 50_000.0))
        results shouldHaveSize 1
        results[0].status shouldBe "OK"
    }

    test("returns WARNING when aggregated VaR approaches limit") {
        coEvery { limitClient.getLimits() } returns ClientResponse.Success(
            listOf(varLimit(limitValue = "100000")),
        )

        val results = service.checkLimits(crossBookResult(varValue = 85_000.0))
        results shouldHaveSize 1
        results[0].status shouldBe "WARNING"
        results[0].groupId shouldBe "desk-alpha"
    }

    test("returns BREACHED when aggregated VaR exceeds limit") {
        coEvery { limitClient.getLimits() } returns ClientResponse.Success(
            listOf(varLimit(limitValue = "100000")),
        )

        val results = service.checkLimits(crossBookResult(varValue = 120_000.0))
        results shouldHaveSize 1
        results[0].status shouldBe "BREACHED"
        results[0].message shouldBe "Aggregated VaR 120000.00 exceeds DESK limit 100000"
    }

    test("ignores limits for other entities") {
        coEvery { limitClient.getLimits() } returns ClientResponse.Success(
            listOf(varLimit(entityId = "other-desk", limitValue = "10000")),
        )

        val results = service.checkLimits(crossBookResult(varValue = 50_000.0))
        results.shouldBeEmpty()
    }

    test("checks FIRM-level limits regardless of group ID") {
        coEvery { limitClient.getLimits() } returns ClientResponse.Success(
            listOf(varLimit(entityId = "FIRM", level = "FIRM", limitValue = "40000")),
        )

        val results = service.checkLimits(crossBookResult(varValue = 50_000.0))
        results shouldHaveSize 1
        results[0].status shouldBe "BREACHED"
        results[0].limitLevel shouldBe "FIRM"
    }

    test("returns empty list when no limits configured") {
        coEvery { limitClient.getLimits() } returns ClientResponse.Success(emptyList())

        val results = service.checkLimits(crossBookResult(varValue = 50_000.0))
        results.shouldBeEmpty()
    }

    test("skips inactive limits") {
        coEvery { limitClient.getLimits() } returns ClientResponse.Success(
            listOf(varLimit(limitValue = "10000", active = false)),
        )

        val results = service.checkLimits(crossBookResult(varValue = 50_000.0))
        results.shouldBeEmpty()
    }
})
