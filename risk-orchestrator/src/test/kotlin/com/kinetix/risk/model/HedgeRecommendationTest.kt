package com.kinetix.risk.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import java.time.Instant
import java.util.UUID

private fun sampleGreekImpact() = GreekImpact(
    deltaBefore = 1000.0,
    deltaAfter = 50.0,
    gammaBefore = 200.0,
    gammaAfter = 180.0,
    vegaBefore = 500.0,
    vegaAfter = 480.0,
    thetaBefore = -30.0,
    thetaAfter = -31.5,
    rhoBefore = 10.0,
    rhoAfter = 9.8,
)

private fun sampleSuggestion(estimatedCost: Double = 12_000.0) = HedgeSuggestion(
    instrumentId = "AAPL-P-2026",
    instrumentType = "OPTION",
    side = "BUY",
    quantity = 10.0,
    estimatedCost = estimatedCost,
    crossingCost = 250.0,
    carrycostPerDay = -5.0,
    targetReduction = 950.0,
    targetReductionPct = 0.95,
    residualMetric = 50.0,
    greekImpact = sampleGreekImpact(),
    liquidityTier = "TIER_1",
    dataQuality = "FRESH",
)

private fun sampleRecommendation(
    status: HedgeStatus = HedgeStatus.PENDING,
    expiresAt: Instant = Instant.now().plusSeconds(1800),
    suggestions: List<HedgeSuggestion> = listOf(sampleSuggestion(10_000.0), sampleSuggestion(8_000.0)),
) = HedgeRecommendation(
    id = UUID.randomUUID(),
    bookId = "BOOK-1",
    targetMetric = HedgeTarget.DELTA,
    targetReductionPct = 0.90,
    requestedAt = Instant.now(),
    status = status,
    constraints = HedgeConstraints(
        maxNotional = 500_000.0,
        maxSuggestions = 5,
        respectPositionLimits = true,
        instrumentUniverse = null,
        allowedSides = listOf("BUY"),
    ),
    suggestions = suggestions,
    preHedgeGreeks = sampleGreekImpact(),
    sourceJobId = "job-abc-123",
    acceptedBy = null,
    acceptedAt = null,
    expiresAt = expiresAt,
)

class HedgeRecommendationTest : FunSpec({

    test("isExpired returns false when expiresAt is in the future") {
        val rec = sampleRecommendation(expiresAt = Instant.now().plusSeconds(1800))
        rec.isExpired.shouldBeFalse()
    }

    test("isExpired returns true when expiresAt is in the past") {
        val rec = sampleRecommendation(expiresAt = Instant.now().minusSeconds(1))
        rec.isExpired.shouldBeTrue()
    }

    test("bestSuggestion returns the first suggestion") {
        val first = sampleSuggestion(estimatedCost = 10_000.0)
        val second = sampleSuggestion(estimatedCost = 8_000.0)
        val rec = sampleRecommendation(suggestions = listOf(first, second))
        rec.bestSuggestion shouldBe first
    }

    test("bestSuggestion returns null when there are no suggestions") {
        val rec = sampleRecommendation(suggestions = emptyList())
        rec.bestSuggestion shouldBe null
    }

    test("totalEstimatedCost sums all suggestion costs") {
        val rec = sampleRecommendation(
            suggestions = listOf(
                sampleSuggestion(estimatedCost = 10_000.0),
                sampleSuggestion(estimatedCost = 8_000.0),
                sampleSuggestion(estimatedCost = 5_500.0),
            )
        )
        rec.totalEstimatedCost shouldBe 23_500.0
    }

    test("totalEstimatedCost is zero for an empty suggestions list") {
        val rec = sampleRecommendation(suggestions = emptyList())
        rec.totalEstimatedCost shouldBe 0.0
    }
})
