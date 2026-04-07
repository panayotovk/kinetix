package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.BatchScenarioResultItem
import kotlinx.serialization.Serializable

@Serializable
data class BatchScenarioResultDto(
    val scenarioName: String,
    val baseVar: String,
    val stressedVar: String,
    val pnlImpact: String,
)

fun BatchScenarioResultItem.toDto() = BatchScenarioResultDto(
    scenarioName = scenarioName,
    baseVar = baseVar,
    stressedVar = stressedVar,
    pnlImpact = pnlImpact,
)
