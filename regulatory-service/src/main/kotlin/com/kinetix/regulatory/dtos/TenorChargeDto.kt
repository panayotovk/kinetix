package com.kinetix.regulatory.dtos

import kotlinx.serialization.Serializable

@Serializable
data class TenorChargeDto(
    val tenorLabel: String,
    val sensitivity: String,
    val riskWeight: String,
    val weightedSensitivity: String,
)
