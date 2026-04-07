package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.PositionStressImpactItem
import kotlinx.serialization.Serializable

@Serializable
data class PositionStressImpactDto(
    val instrumentId: String,
    val assetClass: String,
    val baseMarketValue: String,
    val stressedMarketValue: String,
    val pnlImpact: String,
    val percentageOfTotal: String,
)

fun PositionStressImpactItem.toDto(): PositionStressImpactDto = PositionStressImpactDto(
    instrumentId = instrumentId,
    assetClass = assetClass,
    baseMarketValue = "%.2f".format(baseMarketValue),
    stressedMarketValue = "%.2f".format(stressedMarketValue),
    pnlImpact = "%.2f".format(pnlImpact),
    percentageOfTotal = "%.2f".format(percentageOfTotal),
)
