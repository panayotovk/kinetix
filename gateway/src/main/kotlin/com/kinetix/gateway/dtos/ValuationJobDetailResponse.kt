package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.ValuationJobDetailItem
import kotlinx.serialization.Serializable

@Serializable
data class ValuationJobDetailResponse(
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
    val phases: List<JobPhaseDto> = emptyList(),
    val error: String? = null,
    val valuationDate: String? = null,
    val runLabel: String? = null,
    val promotedAt: String? = null,
    val promotedBy: String? = null,
    val currentPhase: String? = null,
    val manifestId: String? = null,
)

fun ValuationJobDetailItem.toResponse(): ValuationJobDetailResponse = ValuationJobDetailResponse(
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
    phases = phases.map { it.toDto() },
    error = error,
    valuationDate = valuationDate,
    runLabel = runLabel,
    promotedAt = promotedAt,
    promotedBy = promotedBy,
    currentPhase = currentPhase,
    manifestId = manifestId,
)
