package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.PositionPnlAttributionSummary
import kotlinx.serialization.Serializable

@Serializable
data class PositionPnlAttributionResponse(
    val instrumentId: String,
    val assetClass: String,
    val totalPnl: String,
    val deltaPnl: String,
    val gammaPnl: String,
    val vegaPnl: String,
    val thetaPnl: String,
    val rhoPnl: String,
    val unexplainedPnl: String,
)

fun PositionPnlAttributionSummary.toResponse(): PositionPnlAttributionResponse = PositionPnlAttributionResponse(
    instrumentId = instrumentId,
    assetClass = assetClass,
    totalPnl = totalPnl,
    deltaPnl = deltaPnl,
    gammaPnl = gammaPnl,
    vegaPnl = vegaPnl,
    thetaPnl = thetaPnl,
    rhoPnl = rhoPnl,
    unexplainedPnl = unexplainedPnl,
)
