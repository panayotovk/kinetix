package com.kinetix.risk.service

import com.kinetix.common.model.AssetClass
import com.kinetix.common.model.BookId
import com.kinetix.common.model.InstrumentId
import com.kinetix.risk.model.AttributionDataQuality
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import java.math.BigDecimal
import java.math.RoundingMode

private fun bd(value: String) = BigDecimal(value)

private fun inputWith(
    instrumentId: String = "AAPL",
    totalPnl: String = "0.00",
    delta: String = "0.0",
    gamma: String = "0.0",
    vega: String = "0.0",
    theta: String = "0.0",
    rho: String = "0.0",
    vanna: String = "0.0",
    volga: String = "0.0",
    charm: String = "0.0",
    priceChange: String = "0.0",
    volChange: String = "0.0",
    rateChange: String = "0.0",
): PositionPnlInput = PositionPnlInput(
    instrumentId = InstrumentId(instrumentId),
    assetClass = AssetClass.EQUITY,
    totalPnl = bd(totalPnl),
    delta = bd(delta),
    gamma = bd(gamma),
    vega = bd(vega),
    theta = bd(theta),
    rho = bd(rho),
    vanna = bd(vanna),
    volga = bd(volga),
    charm = bd(charm),
    priceChange = bd(priceChange),
    volChange = bd(volChange),
    rateChange = bd(rateChange),
)

class CrossGreekPnlAttributionServiceTest : FunSpec({

    val service = PnlAttributionService()

    // ------------------------------------------------------------------
    // Formula tests: individual cross-Greek terms with known inputs
    // ------------------------------------------------------------------

    test("vanna_pnl = vanna * dS * dvol with known inputs") {
        // vanna=0.5, dS=10.0, dvol=0.02  =>  0.5 * 10.0 * 0.02 = 0.10
        val input = inputWith(
            totalPnl = "0.10",
            vanna = "0.5",
            priceChange = "10.0",
            volChange = "0.02",
        )

        val result = service.attribute(BookId("book-1"), listOf(input))
        val pos = result.positionAttributions[0]

        pos.vannaPnl.setScale(6, RoundingMode.HALF_UP) shouldBe bd("0.100000")
    }

    test("volga_pnl = 0.5 * volga * dvol^2 with known inputs") {
        // volga=200.0, dvol=0.02  =>  0.5 * 200.0 * 0.02^2 = 0.5 * 200.0 * 0.0004 = 0.04
        val input = inputWith(
            totalPnl = "0.04",
            volga = "200.0",
            volChange = "0.02",
        )

        val result = service.attribute(BookId("book-1"), listOf(input))
        val pos = result.positionAttributions[0]

        pos.volgaPnl.setScale(6, RoundingMode.HALF_UP) shouldBe bd("0.040000")
    }

    test("charm_pnl = charm * dS * dT with known inputs") {
        // charm=-0.05, dS=10.0, dT=1/252  =>  -0.05 * 10.0 * (1/252) = -0.001984...
        val input = inputWith(
            totalPnl = "-0.001984",
            charm = "-0.05",
            priceChange = "10.0",
        )

        val result = service.attribute(BookId("book-1"), listOf(input))
        val pos = result.positionAttributions[0]

        // charm_pnl = -0.05 * 10.0 * (1/252) = -0.00198412698...
        pos.charmPnl.setScale(6, RoundingMode.HALF_UP) shouldBe bd("-0.001984")
    }

    test("cross_gamma_pnl is zero for single-asset books") {
        val input = inputWith(
            totalPnl = "1.00",
            gamma = "0.5",
            priceChange = "2.0",
        )

        val result = service.attribute(BookId("book-1"), listOf(input))
        val pos = result.positionAttributions[0]

        pos.crossGammaPnl.compareTo(BigDecimal.ZERO) shouldBe 0
    }

    // ------------------------------------------------------------------
    // Accounting identity: sum(all terms) + unexplained = totalPnl
    // ------------------------------------------------------------------

    test("accounting identity holds: sum of all attributed terms plus unexplained equals total_pnl") {
        val input = inputWith(
            totalPnl = "5.00",
            delta = "0.5",
            gamma = "0.1",
            vega = "200",
            theta = "-50",
            rho = "30",
            vanna = "0.5",
            volga = "200.0",
            charm = "-0.05",
            priceChange = "2.0",
            volChange = "0.01",
            rateChange = "0.005",
        )

        val result = service.attribute(BookId("book-1"), listOf(input))
        val pos = result.positionAttributions[0]

        val sumOfTerms = pos.deltaPnl + pos.gammaPnl + pos.vegaPnl +
            pos.thetaPnl + pos.rhoPnl +
            pos.vannaPnl + pos.volgaPnl + pos.charmPnl + pos.crossGammaPnl

        val recomputed = sumOfTerms + pos.unexplainedPnl

        recomputed.setScale(8, RoundingMode.HALF_UP) shouldBe
            pos.totalPnl.setScale(8, RoundingMode.HALF_UP)
    }

    test("portfolio-level accounting identity: sum(all terms) + unexplained = total_pnl") {
        val inputs = listOf(
            inputWith(
                instrumentId = "AAPL",
                totalPnl = "3.00",
                delta = "0.5",
                gamma = "0.1",
                vega = "100",
                theta = "-20",
                rho = "10",
                vanna = "0.3",
                volga = "100.0",
                charm = "-0.02",
                priceChange = "1.5",
                volChange = "0.01",
                rateChange = "0.003",
            ),
            inputWith(
                instrumentId = "MSFT",
                totalPnl = "2.00",
                delta = "0.3",
                gamma = "0.05",
                vega = "80",
                theta = "-15",
                rho = "8",
                vanna = "0.2",
                volga = "80.0",
                charm = "-0.01",
                priceChange = "1.0",
                volChange = "0.008",
                rateChange = "0.002",
            ),
        )

        val result = service.attribute(BookId("book-1"), inputs)

        val portfolioSumOfTerms = result.deltaPnl + result.gammaPnl + result.vegaPnl +
            result.thetaPnl + result.rhoPnl +
            result.vannaPnl + result.volgaPnl + result.charmPnl + result.crossGammaPnl

        val recomputed = portfolioSumOfTerms + result.unexplainedPnl

        recomputed.setScale(8, RoundingMode.HALF_UP) shouldBe
            result.totalPnl.setScale(8, RoundingMode.HALF_UP)
    }

    // ------------------------------------------------------------------
    // data_quality_flag
    // ------------------------------------------------------------------

    test("data_quality_flag is FULL_ATTRIBUTION when all cross-Greeks are non-zero") {
        val input = inputWith(
            totalPnl = "1.00",
            vanna = "0.5",
            volga = "200.0",
            charm = "-0.05",
            priceChange = "2.0",
            volChange = "0.02",
        )

        val result = service.attribute(BookId("book-1"), listOf(input))

        result.dataQualityFlag shouldBe AttributionDataQuality.FULL_ATTRIBUTION
    }

    test("data_quality_flag is PRICE_ONLY when vanna and volga and charm are all zero") {
        val input = inputWith(
            totalPnl = "1.00",
            delta = "0.5",
            priceChange = "2.0",
            // vanna, volga, charm all default to 0
        )

        val result = service.attribute(BookId("book-1"), listOf(input))

        result.dataQualityFlag shouldBe AttributionDataQuality.PRICE_ONLY
    }

    // ------------------------------------------------------------------
    // cross-Greek terms sum to portfolio-level aggregates
    // ------------------------------------------------------------------

    test("position-level vanna/volga/charm sum to portfolio totals") {
        val inputs = listOf(
            inputWith(
                instrumentId = "AAPL",
                totalPnl = "2.00",
                vanna = "0.5",
                volga = "100.0",
                charm = "-0.03",
                priceChange = "2.0",
                volChange = "0.01",
            ),
            inputWith(
                instrumentId = "MSFT",
                totalPnl = "1.50",
                vanna = "0.3",
                volga = "60.0",
                charm = "-0.02",
                priceChange = "1.5",
                volChange = "0.008",
            ),
        )

        val result = service.attribute(BookId("book-1"), inputs)

        val sumVanna = result.positionAttributions.fold(BigDecimal.ZERO) { acc, p -> acc + p.vannaPnl }
        result.vannaPnl.setScale(10, RoundingMode.HALF_UP) shouldBe
            sumVanna.setScale(10, RoundingMode.HALF_UP)

        val sumVolga = result.positionAttributions.fold(BigDecimal.ZERO) { acc, p -> acc + p.volgaPnl }
        result.volgaPnl.setScale(10, RoundingMode.HALF_UP) shouldBe
            sumVolga.setScale(10, RoundingMode.HALF_UP)

        val sumCharm = result.positionAttributions.fold(BigDecimal.ZERO) { acc, p -> acc + p.charmPnl }
        result.charmPnl.setScale(10, RoundingMode.HALF_UP) shouldBe
            sumCharm.setScale(10, RoundingMode.HALF_UP)
    }

    // ------------------------------------------------------------------
    // Backward-compatibility: existing Greek terms are unchanged
    // ------------------------------------------------------------------

    test("existing first-order Greek formulas are unchanged with cross-Greeks present") {
        // Same inputs as original PnlAttributionServiceTest — verify we haven't broken anything
        val input = PositionPnlInput(
            instrumentId = InstrumentId("AAPL"),
            assetClass = AssetClass.EQUITY,
            totalPnl = bd("3.5"),
            delta = bd("0.5"),
            gamma = bd("0.1"),
            vega = bd("200"),
            theta = bd("-50"),
            rho = bd("30"),
            vanna = BigDecimal.ZERO,
            volga = BigDecimal.ZERO,
            charm = BigDecimal.ZERO,
            priceChange = bd("2.0"),
            volChange = bd("0.01"),
            rateChange = bd("0.005"),
        )

        val result = service.attribute(BookId("port-1"), listOf(input))
        val pos = result.positionAttributions[0]

        pos.deltaPnl.setScale(6, RoundingMode.HALF_UP) shouldBe bd("1.000000")
        pos.gammaPnl.setScale(6, RoundingMode.HALF_UP) shouldBe bd("0.200000")
        pos.vegaPnl.setScale(6, RoundingMode.HALF_UP) shouldBe bd("2.000000")
        pos.thetaPnl.setScale(6, RoundingMode.HALF_UP) shouldBe bd("-0.198413")
        pos.rhoPnl.setScale(6, RoundingMode.HALF_UP) shouldBe bd("0.150000")
    }
})
