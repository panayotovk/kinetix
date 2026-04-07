package com.kinetix.regulatory.historical.dtos

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
