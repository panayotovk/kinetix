package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.HistoricalReplayResultSummary
import kotlinx.serialization.Serializable

@Serializable
data class HistoricalReplayResponse(
    val scenarioName: String,
    val totalPnlImpact: String,
    val positionImpacts: List<PositionReplayImpactDto>,
    val windowStart: String?,
    val windowEnd: String?,
    val calculatedAt: String,
)

fun HistoricalReplayResultSummary.toResponse(): HistoricalReplayResponse = HistoricalReplayResponse(
    scenarioName = scenarioName,
    totalPnlImpact = totalPnlImpact,
    positionImpacts = positionImpacts.map { it.toDto() },
    windowStart = windowStart,
    windowEnd = windowEnd,
    calculatedAt = calculatedAt,
)
