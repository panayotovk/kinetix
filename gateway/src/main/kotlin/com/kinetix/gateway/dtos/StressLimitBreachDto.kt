package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.StressLimitBreachItem
import kotlinx.serialization.Serializable

@Serializable
data class StressLimitBreachDto(
    val limitType: String,
    val limitLevel: String,
    val limitValue: String,
    val stressedValue: String,
    val breachSeverity: String,
    val scenarioName: String,
)

fun StressLimitBreachItem.toDto(): StressLimitBreachDto = StressLimitBreachDto(
    limitType = limitType,
    limitLevel = limitLevel,
    limitValue = limitValue,
    stressedValue = stressedValue,
    breachSeverity = breachSeverity,
    scenarioName = scenarioName,
)
