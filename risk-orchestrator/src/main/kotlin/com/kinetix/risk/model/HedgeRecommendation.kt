package com.kinetix.risk.model

import java.time.Instant
import java.util.UUID

data class HedgeRecommendation(
    val id: UUID,
    val bookId: String,
    val targetMetric: HedgeTarget,
    val targetReductionPct: Double,
    val requestedAt: Instant,
    val status: HedgeStatus,
    val constraints: HedgeConstraints,
    val suggestions: List<HedgeSuggestion>,
    val preHedgeGreeks: GreekImpact,
    val sourceJobId: String?,
    val acceptedBy: String?,
    val acceptedAt: Instant?,
    val expiresAt: Instant,
) {
    val isExpired: Boolean get() = expiresAt <= Instant.now()
    val bestSuggestion: HedgeSuggestion? get() = suggestions.firstOrNull()
    val totalEstimatedCost: Double get() = suggestions.sumOf { it.estimatedCost }
}
