package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.PnlAttributionSummary
import kotlinx.serialization.Serializable

@Serializable
data class PnlAttributionResponse(
    val bookId: String,
    val date: String,
    val totalPnl: String,
    val deltaPnl: String,
    val gammaPnl: String,
    val vegaPnl: String,
    val thetaPnl: String,
    val rhoPnl: String,
    val unexplainedPnl: String,
    val positionAttributions: List<PositionPnlAttributionResponse>,
    val calculatedAt: String,
)

fun PnlAttributionSummary.toResponse(): PnlAttributionResponse = PnlAttributionResponse(
    bookId = bookId,
    date = date,
    totalPnl = totalPnl,
    deltaPnl = deltaPnl,
    gammaPnl = gammaPnl,
    vegaPnl = vegaPnl,
    thetaPnl = thetaPnl,
    rhoPnl = rhoPnl,
    unexplainedPnl = unexplainedPnl,
    positionAttributions = positionAttributions.map { it.toResponse() },
    calculatedAt = calculatedAt,
)
