package com.kinetix.risk.service

import com.kinetix.risk.model.CandidateInstrument
import com.kinetix.risk.model.GreekImpact
import com.kinetix.risk.model.HedgeConstraints
import com.kinetix.risk.model.HedgeSuggestion
import com.kinetix.risk.model.HedgeTarget
import kotlin.math.abs

private const val STALE_PRICE_THRESHOLD_MINUTES = 15

/**
 * Pure function: given current portfolio Greeks, a set of candidate instruments with
 * per-unit sensitivities, a target metric and reduction percentage, and optional
 * constraints, returns a ranked list of hedge suggestions.
 *
 * Phase 1 (analytical). Phase 2 will delegate to the Python gRPC optimizer for
 * VaR-minimising strategies via scipy.optimize.
 *
 * Each suggestion surfaces the FULL Greek impact — not only the target Greek —
 * so the caller can detect Lyapunov trade-offs (e.g., reducing vega increases gamma).
 */
class AnalyticalHedgeCalculator {

    fun suggest(
        currentGreeks: GreekImpact,
        target: HedgeTarget,
        targetReductionPct: Double,
        candidates: List<CandidateInstrument>,
        constraints: HedgeConstraints,
    ): List<HedgeSuggestion> {
        val currentMetricValue = metricValue(currentGreeks, target)
        val targetReduction = currentMetricValue * targetReductionPct

        return candidates
            .mapNotNull { candidate -> buildSuggestion(candidate, currentGreeks, target, currentMetricValue, targetReduction, constraints) }
            .filter { suggestion -> constraintsSatisfied(suggestion, constraints) }
            .sortedByDescending { it.targetReduction / it.estimatedCost.coerceAtLeast(0.01) }
            .take(constraints.maxSuggestions)
    }

    private fun metricValue(greeks: GreekImpact, target: HedgeTarget): Double = when (target) {
        HedgeTarget.DELTA -> greeks.deltaBefore
        HedgeTarget.GAMMA -> greeks.gammaBefore
        HedgeTarget.VEGA -> greeks.vegaBefore
        HedgeTarget.VAR -> greeks.deltaBefore // VAR target handled by Phase 2 gRPC; fall back to delta for now
    }

    private fun sensitivityPerUnit(candidate: CandidateInstrument, target: HedgeTarget): Double = when (target) {
        HedgeTarget.DELTA -> candidate.deltaPerUnit
        HedgeTarget.GAMMA -> candidate.gammaPerUnit
        HedgeTarget.VEGA -> candidate.vegaPerUnit
        HedgeTarget.VAR -> candidate.deltaPerUnit
    }

    private fun buildSuggestion(
        candidate: CandidateInstrument,
        currentGreeks: GreekImpact,
        target: HedgeTarget,
        currentMetricValue: Double,
        targetReduction: Double,
        constraints: HedgeConstraints,
    ): HedgeSuggestion? {
        val sensitivity = sensitivityPerUnit(candidate, target)
        if (sensitivity == 0.0) return null

        // We want to reduce the metric value by targetReduction.
        // units_needed * sensitivity = -targetReduction  =>  units_needed = -targetReduction / sensitivity
        val unitsNeeded = -targetReduction / sensitivity
        if (unitsNeeded <= 0.0) return null // buying would worsen the target

        val side = determineSide(unitsNeeded, constraints) ?: return null

        val quantity = abs(unitsNeeded)
        val bidAskSpreadAmount = candidate.pricePerUnit * candidate.bidAskSpreadBps / 10_000.0
        val crossingCost = 0.5 * bidAskSpreadAmount * quantity
        val premium = quantity * candidate.pricePerUnit
        val estimatedCost = premium + crossingCost

        val actualDeltaChange = quantity * candidate.deltaPerUnit * sideSign(side)
        val actualGammaChange = quantity * candidate.gammaPerUnit * sideSign(side)
        val actualVegaChange = quantity * candidate.vegaPerUnit * sideSign(side)
        val actualThetaChange = quantity * candidate.thetaPerUnit * sideSign(side)
        val actualRhoChange = quantity * candidate.rhoPerUnit * sideSign(side)

        val greekImpact = GreekImpact(
            deltaBefore = currentGreeks.deltaBefore,
            deltaAfter = currentGreeks.deltaBefore + actualDeltaChange,
            gammaBefore = currentGreeks.gammaBefore,
            gammaAfter = currentGreeks.gammaBefore + actualGammaChange,
            vegaBefore = currentGreeks.vegaBefore,
            vegaAfter = currentGreeks.vegaBefore + actualVegaChange,
            thetaBefore = currentGreeks.thetaBefore,
            thetaAfter = currentGreeks.thetaBefore + actualThetaChange,
            rhoBefore = currentGreeks.rhoBefore,
            rhoAfter = currentGreeks.rhoBefore + actualRhoChange,
        )

        val actualReduction = currentMetricValue - metricValueAfter(greekImpact, target)
        val actualReductionPct = if (currentMetricValue != 0.0) actualReduction / abs(currentMetricValue) else 0.0
        val residualMetric = metricValueAfter(greekImpact, target)

        val dataQuality = if (candidate.priceAgeMinutes >= STALE_PRICE_THRESHOLD_MINUTES) "STALE" else "FRESH"

        return HedgeSuggestion(
            instrumentId = candidate.instrumentId,
            instrumentType = candidate.instrumentType,
            side = side,
            quantity = quantity,
            estimatedCost = estimatedCost,
            crossingCost = crossingCost,
            carrycostPerDay = if (candidate.thetaPerUnit != 0.0) quantity * candidate.thetaPerUnit else null,
            targetReduction = actualReduction,
            targetReductionPct = actualReductionPct,
            residualMetric = residualMetric,
            greekImpact = greekImpact,
            liquidityTier = candidate.liquidityTier,
            dataQuality = dataQuality,
        )
    }

    private fun determineSide(unitsNeeded: Double, constraints: HedgeConstraints): String? {
        // unitsNeeded > 0 means we buy the instrument
        val naturalSide = if (unitsNeeded > 0) "BUY" else "SELL"
        val allowedSides = constraints.allowedSides
        return if (allowedSides == null || naturalSide in allowedSides) naturalSide else null
    }

    private fun sideSign(side: String): Double = if (side == "BUY") 1.0 else -1.0

    private fun metricValueAfter(greekImpact: GreekImpact, target: HedgeTarget): Double = when (target) {
        HedgeTarget.DELTA -> greekImpact.deltaAfter
        HedgeTarget.GAMMA -> greekImpact.gammaAfter
        HedgeTarget.VEGA -> greekImpact.vegaAfter
        HedgeTarget.VAR -> greekImpact.deltaAfter
    }

    private fun constraintsSatisfied(suggestion: HedgeSuggestion, constraints: HedgeConstraints): Boolean {
        val notionalUsed = suggestion.quantity * extractPrice(suggestion)
        if (constraints.maxNotional != null && notionalUsed > constraints.maxNotional) return false
        return true
    }

    private fun extractPrice(suggestion: HedgeSuggestion): Double {
        // estimatedCost = quantity * price + crossingCost
        // => price = (estimatedCost - crossingCost) / quantity
        if (suggestion.quantity == 0.0) return 0.0
        return (suggestion.estimatedCost - suggestion.crossingCost) / suggestion.quantity
    }
}
