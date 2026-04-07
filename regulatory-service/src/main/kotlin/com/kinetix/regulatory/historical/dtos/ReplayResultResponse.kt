package com.kinetix.regulatory.historical.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ReplayResultResponse(
    val periodId: String,
    val scenarioName: String,
    val bookId: String,
    val totalPnlImpact: String,
    val positionImpacts: List<PositionReplayImpact>,
    val windowStart: String?,
    val windowEnd: String?,
    val calculatedAt: String,
)
