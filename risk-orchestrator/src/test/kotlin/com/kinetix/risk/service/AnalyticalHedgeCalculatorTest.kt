package com.kinetix.risk.service

import com.kinetix.risk.model.CandidateInstrument
import com.kinetix.risk.model.GreekImpact
import com.kinetix.risk.model.HedgeConstraints
import com.kinetix.risk.model.HedgeTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeWithinPercentageOf
import io.kotest.matchers.shouldBe

private val defaultConstraints = HedgeConstraints(
    maxNotional = null,
    maxSuggestions = 5,
    respectPositionLimits = false,
    instrumentUniverse = null,
    allowedSides = null,
)

private fun greeks(
    delta: Double = 0.0,
    gamma: Double = 0.0,
    vega: Double = 0.0,
    theta: Double = 0.0,
    rho: Double = 0.0,
) = GreekImpact(
    deltaBefore = delta, deltaAfter = delta,
    gammaBefore = gamma, gammaAfter = gamma,
    vegaBefore = vega, vegaAfter = vega,
    thetaBefore = theta, thetaAfter = theta,
    rhoBefore = rho, rhoAfter = rho,
)

private fun candidate(
    instrumentId: String,
    instrumentType: String = "STOCK",
    pricePerUnit: Double = 100.0,
    bidAskSpreadBps: Double = 10.0,
    deltaPerUnit: Double = 0.0,
    gammaPerUnit: Double = 0.0,
    vegaPerUnit: Double = 0.0,
    thetaPerUnit: Double = 0.0,
    rhoPerUnit: Double = 0.0,
    liquidityTier: String = "TIER_1",
    priceAgeMinutes: Int = 5,
) = CandidateInstrument(
    instrumentId = instrumentId,
    instrumentType = instrumentType,
    pricePerUnit = pricePerUnit,
    bidAskSpreadBps = bidAskSpreadBps,
    deltaPerUnit = deltaPerUnit,
    gammaPerUnit = gammaPerUnit,
    vegaPerUnit = vegaPerUnit,
    thetaPerUnit = thetaPerUnit,
    rhoPerUnit = rhoPerUnit,
    liquidityTier = liquidityTier,
    priceAgeMinutes = priceAgeMinutes,
)

class AnalyticalHedgeCalculatorTest : FunSpec({

    val calculator = AnalyticalHedgeCalculator()

    // ------------------------------------------------------------------ delta neutralisation

    test("suggests a sell for a long-delta book when target is delta") {
        val currentGreeks = greeks(delta = 1000.0)
        val candidates = listOf(
            candidate("HEDGE-PUT", deltaPerUnit = -1.0, pricePerUnit = 5.0),
        )

        val suggestions = calculator.suggest(
            currentGreeks = currentGreeks,
            target = HedgeTarget.DELTA,
            targetReductionPct = 1.0,
            candidates = candidates,
            constraints = defaultConstraints,
        )

        suggestions shouldHaveSize 1
        val s = suggestions.first()
        s.instrumentId shouldBe "HEDGE-PUT"
        s.side shouldBe "BUY"
        s.quantity.shouldBeWithinPercentageOf(1000.0, 1.0)
    }

    test("reduces delta by approximately the requested percentage") {
        val currentGreeks = greeks(delta = 2000.0)
        val candidates = listOf(
            candidate("HEDGE-A", deltaPerUnit = -1.0, pricePerUnit = 10.0),
        )

        val suggestions = calculator.suggest(
            currentGreeks = currentGreeks,
            target = HedgeTarget.DELTA,
            targetReductionPct = 0.50,
            candidates = candidates,
            constraints = defaultConstraints,
        )

        suggestions shouldHaveSize 1
        val s = suggestions.first()
        s.targetReductionPct.shouldBeWithinPercentageOf(0.50, 5.0)
        s.greekImpact.deltaAfter.shouldBeWithinPercentageOf(1000.0, 5.0)
    }

    // ------------------------------------------------------------------ vega neutralisation

    test("suggests a trade to reduce vega") {
        val currentGreeks = greeks(delta = 100.0, vega = 500.0)
        val candidates = listOf(
            candidate("VOL-SWAP", instrumentType = "VARIANCE_SWAP", vegaPerUnit = -10.0, pricePerUnit = 1000.0),
        )

        val suggestions = calculator.suggest(
            currentGreeks = currentGreeks,
            target = HedgeTarget.VEGA,
            targetReductionPct = 1.0,
            candidates = candidates,
            constraints = defaultConstraints,
        )

        suggestions shouldHaveSize 1
        val s = suggestions.first()
        s.greekImpact.vegaAfter.shouldBeWithinPercentageOf(0.0, 10.0)
    }

    // ------------------------------------------------------------------ full greek impact (Lyapunov check)

    test("shows full greek impact for all greeks, not only the target") {
        val currentGreeks = greeks(delta = 1000.0, gamma = 200.0, vega = 500.0, theta = -30.0, rho = 10.0)
        val candidates = listOf(
            candidate(
                "OPTION",
                instrumentType = "OPTION",
                deltaPerUnit = -1.0,
                gammaPerUnit = 0.05,
                vegaPerUnit = 2.0,
                thetaPerUnit = -0.5,
                rhoPerUnit = 0.1,
                pricePerUnit = 5.0,
            )
        )

        val suggestions = calculator.suggest(
            currentGreeks = currentGreeks,
            target = HedgeTarget.DELTA,
            targetReductionPct = 1.0,
            candidates = candidates,
            constraints = defaultConstraints,
        )

        val impact = suggestions.first().greekImpact
        impact.deltaBefore.shouldBeWithinPercentageOf(1000.0, 1.0)
        impact.deltaAfter.shouldBeWithinPercentageOf(0.0, 10.0)
        // Gamma increases when hedging delta with options — the Lyapunov problem
        impact.gammaAfter shouldBe impact.gammaBefore + (1000.0 * 0.05)
        // Vega also changes
        impact.vegaAfter shouldBe impact.vegaBefore + (1000.0 * 2.0)
    }

    // ------------------------------------------------------------------ cost calculation

    test("calculates crossing cost as 0.5 * bid_ask_spread * quantity") {
        val currentGreeks = greeks(delta = 100.0)
        val candidates = listOf(
            // bid_ask_spread_bps = 20bps on a $100 instrument => spread = $0.02
            // quantity needed = 100 (delta/deltaPerUnit = 100/1.0)
            // crossing_cost = 0.5 * 0.02 * 100 = $1.0
            candidate("STOCK-A", deltaPerUnit = -1.0, pricePerUnit = 100.0, bidAskSpreadBps = 20.0),
        )

        val suggestions = calculator.suggest(
            currentGreeks = currentGreeks,
            target = HedgeTarget.DELTA,
            targetReductionPct = 1.0,
            candidates = candidates,
            constraints = defaultConstraints,
        )

        val s = suggestions.first()
        val expectedBidAskSpread = 100.0 * 20.0 / 10000.0 // = $0.20 per unit
        val expectedQuantity = 100.0
        val expectedCrossingCost = 0.5 * expectedBidAskSpread * expectedQuantity // = $10
        s.crossingCost.shouldBeWithinPercentageOf(expectedCrossingCost, 1.0)
    }

    test("total estimated cost includes premium plus crossing cost") {
        val currentGreeks = greeks(delta = 100.0)
        val candidates = listOf(
            candidate("STOCK-A", deltaPerUnit = -1.0, pricePerUnit = 50.0, bidAskSpreadBps = 20.0),
        )

        val suggestions = calculator.suggest(
            currentGreeks = currentGreeks,
            target = HedgeTarget.DELTA,
            targetReductionPct = 1.0,
            candidates = candidates,
            constraints = defaultConstraints,
        )

        val s = suggestions.first()
        s.estimatedCost.shouldBeWithinPercentageOf(s.quantity * 50.0 + s.crossingCost, 1.0)
    }

    // ------------------------------------------------------------------ constraint: max notional

    test("respects maxNotional constraint and excludes candidates that would breach it") {
        val currentGreeks = greeks(delta = 1000.0)
        val candidates = listOf(
            // quantity needed = 1000, price = $500 => notional = $500,000 > maxNotional
            candidate("EXPENSIVE", deltaPerUnit = -1.0, pricePerUnit = 500.0),
            // quantity needed = 1000, price = $50 => notional = $50,000 <= maxNotional
            candidate("AFFORDABLE", deltaPerUnit = -1.0, pricePerUnit = 50.0),
        )

        val constraints = defaultConstraints.copy(maxNotional = 100_000.0)

        val suggestions = calculator.suggest(
            currentGreeks = currentGreeks,
            target = HedgeTarget.DELTA,
            targetReductionPct = 1.0,
            candidates = candidates,
            constraints = constraints,
        )

        suggestions.none { it.instrumentId == "EXPENSIVE" } shouldBe true
        suggestions.any { it.instrumentId == "AFFORDABLE" } shouldBe true
    }

    // ------------------------------------------------------------------ constraint: allowed sides

    test("respects allowedSides constraint") {
        val currentGreeks = greeks(delta = 1000.0)
        // Both candidates have negative sensitivity so BUY side reduces delta
        val candidates = listOf(
            candidate("HEDGE-A", deltaPerUnit = -1.0, pricePerUnit = 10.0),
        )

        // Only SELL allowed — no suggestion should be produced for a positive-delta book
        val constraints = defaultConstraints.copy(allowedSides = listOf("SELL"))

        val suggestions = calculator.suggest(
            currentGreeks = currentGreeks,
            target = HedgeTarget.DELTA,
            targetReductionPct = 1.0,
            candidates = candidates,
            constraints = constraints,
        )

        suggestions.shouldBeEmpty()
    }

    // ------------------------------------------------------------------ ranking

    test("ranks suggestions by cost-effectiveness descending (best first)") {
        val currentGreeks = greeks(delta = 1000.0)
        val candidates = listOf(
            // Candidate A: needs 1000 units at $1 + small spread => very effective
            candidate("CHEAP-A", deltaPerUnit = -1.0, pricePerUnit = 1.0, bidAskSpreadBps = 5.0),
            // Candidate B: needs 1000 units at $100 + large spread => less effective
            candidate("EXPENSIVE-B", deltaPerUnit = -1.0, pricePerUnit = 100.0, bidAskSpreadBps = 50.0),
        )

        val suggestions = calculator.suggest(
            currentGreeks = currentGreeks,
            target = HedgeTarget.DELTA,
            targetReductionPct = 1.0,
            candidates = candidates,
            constraints = defaultConstraints,
        )

        suggestions shouldHaveSize 2
        suggestions[0].instrumentId shouldBe "CHEAP-A"
        suggestions[1].instrumentId shouldBe "EXPENSIVE-B"
    }

    // ------------------------------------------------------------------ maxSuggestions

    test("returns at most maxSuggestions results") {
        val currentGreeks = greeks(delta = 1000.0)
        val candidates = (1..10).map { i ->
            candidate("STOCK-$i", deltaPerUnit = -1.0, pricePerUnit = i * 10.0)
        }

        val constraints = defaultConstraints.copy(maxSuggestions = 3)

        val suggestions = calculator.suggest(
            currentGreeks = currentGreeks,
            target = HedgeTarget.DELTA,
            targetReductionPct = 1.0,
            candidates = candidates,
            constraints = constraints,
        )

        suggestions shouldHaveSize 3
    }

    // ------------------------------------------------------------------ stale data quality

    test("marks suggestions as STALE when price is older than 15 minutes") {
        val currentGreeks = greeks(delta = 100.0)
        val candidates = listOf(
            candidate("FRESH-STOCK", deltaPerUnit = -1.0, priceAgeMinutes = 5),
            candidate("STALE-STOCK", deltaPerUnit = -1.0, priceAgeMinutes = 20),
        )

        val suggestions = calculator.suggest(
            currentGreeks = currentGreeks,
            target = HedgeTarget.DELTA,
            targetReductionPct = 1.0,
            candidates = candidates,
            constraints = defaultConstraints.copy(maxSuggestions = 10),
        )

        suggestions.find { it.instrumentId == "FRESH-STOCK" }!!.dataQuality shouldBe "FRESH"
        suggestions.find { it.instrumentId == "STALE-STOCK" }!!.dataQuality shouldBe "STALE"
    }

    // ------------------------------------------------------------------ empty candidates

    test("returns empty list when candidates list is empty") {
        val currentGreeks = greeks(delta = 1000.0)

        val suggestions = calculator.suggest(
            currentGreeks = currentGreeks,
            target = HedgeTarget.DELTA,
            targetReductionPct = 1.0,
            candidates = emptyList(),
            constraints = defaultConstraints,
        )

        suggestions.shouldBeEmpty()
    }

    // ------------------------------------------------------------------ gamma target

    test("suggests trade to reduce gamma") {
        val currentGreeks = greeks(gamma = 200.0)
        val candidates = listOf(
            candidate("GAMMA-HEDGE", gammaPerUnit = -2.0, pricePerUnit = 10.0),
        )

        val suggestions = calculator.suggest(
            currentGreeks = currentGreeks,
            target = HedgeTarget.GAMMA,
            targetReductionPct = 1.0,
            candidates = candidates,
            constraints = defaultConstraints,
        )

        suggestions shouldHaveSize 1
        suggestions.first().greekImpact.gammaAfter.shouldBeWithinPercentageOf(0.0, 10.0)
    }
})
