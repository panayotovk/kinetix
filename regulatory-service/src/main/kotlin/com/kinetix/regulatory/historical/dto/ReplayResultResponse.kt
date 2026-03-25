package com.kinetix.regulatory.historical.dto

import kotlinx.serialization.Serializable

@Serializable
data class PositionReplayImpact(
    val instrumentId: String,
    val assetClass: String,
    val marketValue: String,
    val pnlImpact: String,
    val dailyPnl: List<String>,
    val proxyUsed: Boolean,
)

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
