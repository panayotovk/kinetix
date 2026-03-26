package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class EarlyWarningDto(
    val signalName: String,
    val currentValue: Double,
    val threshold: Double,
    val proximityPct: Double,
    val message: String,
)
