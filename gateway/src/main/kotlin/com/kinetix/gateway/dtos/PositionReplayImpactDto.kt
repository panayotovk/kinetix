package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.PositionReplayImpactSummary
import kotlinx.serialization.Serializable

@Serializable
data class PositionReplayImpactDto(
    val instrumentId: String,
    val assetClass: String,
    val marketValue: String,
    val pnlImpact: String,
    val dailyPnl: List<String>,
    val proxyUsed: Boolean,
)

fun PositionReplayImpactSummary.toDto(): PositionReplayImpactDto = PositionReplayImpactDto(
    instrumentId = instrumentId,
    assetClass = assetClass,
    marketValue = marketValue,
    pnlImpact = pnlImpact,
    dailyPnl = dailyPnl,
    proxyUsed = proxyUsed,
)
