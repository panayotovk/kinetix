package com.kinetix.risk.service

import com.kinetix.risk.client.ClientResponse
import com.kinetix.risk.client.LimitServiceClient
import com.kinetix.risk.client.dtos.LimitDefinitionDto
import com.kinetix.risk.routes.dtos.AssetClassImpactDto
import com.kinetix.risk.routes.dtos.PositionStressImpactDto
import com.kinetix.risk.routes.dtos.StressTestResponse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class StressLimitCheckServiceTest : FunSpec({

    val limitServiceClient = mockk<LimitServiceClient>()
    val service = StressLimitCheckService(limitServiceClient)

    fun stressResponse(stressedVar: String = "500000.00", positionStressedValues: List<String> = listOf("300000.00", "200000.00")) =
        StressTestResponse(
            scenarioName = "GFC_2008",
            baseVar = "100000.00",
            stressedVar = stressedVar,
            pnlImpact = "-400000.00",
            assetClassImpacts = listOf(
                AssetClassImpactDto("EQUITY", "1000000.00", "600000.00", "-400000.00"),
            ),
            calculatedAt = "2026-03-03T08:00:00Z",
            positionImpacts = positionStressedValues.mapIndexed { i, mv ->
                PositionStressImpactDto("INST_$i", "EQUITY", "500000.00", mv, "-${500000.0 - mv.toDouble()}", "50.00")
            },
        )

    fun varLimit(value: String = "400000.00") = LimitDefinitionDto(
        id = "lim-1",
        level = "FIRM",
        entityId = "FIRM",
        limitType = "VAR",
        limitValue = value,
        active = true,
    )

    fun notionalLimit(value: String = "600000.00") = LimitDefinitionDto(
        id = "lim-2",
        level = "DESK",
        entityId = "DESK-1",
        limitType = "NOTIONAL",
        limitValue = value,
        active = true,
    )

    test("should return BREACHED when stressed VaR exceeds VAR limit") {
        coEvery { limitServiceClient.getLimits() } returns ClientResponse.Success(listOf(varLimit("400000.00")))

        val breaches = service.evaluateBreaches(stressResponse(stressedVar = "500000.00"))

        breaches shouldHaveSize 1
        breaches[0].breachSeverity shouldBe "BREACHED"
        breaches[0].limitType shouldBe "VAR"
        breaches[0].limitLevel shouldBe "FIRM"
    }

    test("should return WARNING when stressed VaR is between 80% and 100% of limit") {
        coEvery { limitServiceClient.getLimits() } returns ClientResponse.Success(listOf(varLimit("600000.00")))

        val breaches = service.evaluateBreaches(stressResponse(stressedVar = "500000.00"))

        breaches shouldHaveSize 1
        breaches[0].breachSeverity shouldBe "WARNING"
    }

    test("should return OK when stressed VaR is below 80% of limit") {
        coEvery { limitServiceClient.getLimits() } returns ClientResponse.Success(listOf(varLimit("1000000.00")))

        val breaches = service.evaluateBreaches(stressResponse(stressedVar = "500000.00"))

        breaches shouldHaveSize 1
        breaches[0].breachSeverity shouldBe "OK"
    }

    test("should evaluate NOTIONAL limit against total stressed position market values") {
        coEvery { limitServiceClient.getLimits() } returns ClientResponse.Success(
            listOf(notionalLimit("400000.00")),
        )

        val breaches = service.evaluateBreaches(stressResponse(positionStressedValues = listOf("300000.00", "200000.00")))

        breaches shouldHaveSize 1
        breaches[0].breachSeverity shouldBe "BREACHED"
        breaches[0].limitType shouldBe "NOTIONAL"
        breaches[0].stressedValue shouldBe "500000.00"
    }

    test("should return empty list when no limits are defined") {
        coEvery { limitServiceClient.getLimits() } returns ClientResponse.Success(emptyList())

        val breaches = service.evaluateBreaches(stressResponse())

        breaches shouldHaveSize 0
    }

    test("should return empty list when limit service returns NotFound") {
        coEvery { limitServiceClient.getLimits() } returns ClientResponse.NotFound(404)

        val breaches = service.evaluateBreaches(stressResponse())

        breaches shouldHaveSize 0
    }

    test("should skip inactive limits") {
        coEvery { limitServiceClient.getLimits() } returns ClientResponse.Success(
            listOf(varLimit("100.00").copy(active = false)),
        )

        val breaches = service.evaluateBreaches(stressResponse())

        breaches shouldHaveSize 0
    }

    test("should evaluate multiple limits independently") {
        coEvery { limitServiceClient.getLimits() } returns ClientResponse.Success(
            listOf(varLimit("400000.00"), notionalLimit("400000.00")),
        )

        val breaches = service.evaluateBreaches(stressResponse(stressedVar = "500000.00", positionStressedValues = listOf("300000.00", "200000.00")))

        breaches shouldHaveSize 2
        breaches[0].limitType shouldBe "VAR"
        breaches[0].breachSeverity shouldBe "BREACHED"
        breaches[1].limitType shouldBe "NOTIONAL"
        breaches[1].breachSeverity shouldBe "BREACHED"
    }
})
