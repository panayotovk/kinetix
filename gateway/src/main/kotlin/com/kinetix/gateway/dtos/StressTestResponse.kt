package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.StressTestResultSummary
import kotlinx.serialization.Serializable

@Serializable
data class StressTestResponse(
    val scenarioName: String,
    val baseVar: String,
    val stressedVar: String,
    val pnlImpact: String,
    val assetClassImpacts: List<AssetClassImpactDto>,
    val calculatedAt: String,
    val positionImpacts: List<PositionStressImpactDto> = emptyList(),
    val limitBreaches: List<StressLimitBreachDto> = emptyList(),
)

fun StressTestResultSummary.toResponse(): StressTestResponse = StressTestResponse(
    scenarioName = scenarioName,
    baseVar = "%.2f".format(baseVar),
    stressedVar = "%.2f".format(stressedVar),
    pnlImpact = "%.2f".format(pnlImpact),
    assetClassImpacts = assetClassImpacts.map { it.toDto() },
    calculatedAt = calculatedAt.toString(),
    positionImpacts = positionImpacts.map { it.toDto() },
    limitBreaches = limitBreaches.map { it.toDto() },
)
