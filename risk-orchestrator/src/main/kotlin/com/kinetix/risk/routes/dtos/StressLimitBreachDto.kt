package com.kinetix.risk.routes.dtos

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
