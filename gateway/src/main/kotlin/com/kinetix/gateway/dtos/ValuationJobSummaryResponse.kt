package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.ValuationJobSummaryItem
import kotlinx.serialization.Serializable

@Serializable
data class ValuationJobSummaryResponse(
    val jobId: String,
    val bookId: String,
    val triggerType: String,
    val status: String,
    val startedAt: String,
    val completedAt: String? = null,
    val durationMs: Long? = null,
    val calculationType: String? = null,
    val confidenceLevel: String? = null,
    val varValue: Double? = null,
    val expectedShortfall: Double? = null,
    val pvValue: Double? = null,
    val delta: Double? = null,
    val gamma: Double? = null,
    val vega: Double? = null,
    val theta: Double? = null,
    val rho: Double? = null,
    val valuationDate: String? = null,
    val runLabel: String? = null,
    val promotedAt: String? = null,
    val promotedBy: String? = null,
    val currentPhase: String? = null,
    val manifestId: String? = null,
)

fun ValuationJobSummaryItem.toResponse(): ValuationJobSummaryResponse = ValuationJobSummaryResponse(
    jobId = jobId,
    bookId = bookId,
    triggerType = triggerType,
    status = status,
    startedAt = startedAt.toString(),
    completedAt = completedAt?.toString(),
    durationMs = durationMs,
    calculationType = calculationType,
    confidenceLevel = confidenceLevel,
    varValue = varValue,
    expectedShortfall = expectedShortfall,
    pvValue = pvValue,
    delta = delta,
    gamma = gamma,
    vega = vega,
    theta = theta,
    rho = rho,
    valuationDate = valuationDate,
    runLabel = runLabel,
    promotedAt = promotedAt,
    promotedBy = promotedBy,
    currentPhase = currentPhase,
    manifestId = manifestId,
)
