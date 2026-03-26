package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class TenorChargeDto(
    val tenorLabel: String,
    val sensitivity: String,
    val riskWeight: String,
    val weightedSensitivity: String,
)

@Serializable
data class RiskClassChargeDto(
    val riskClass: String,
    val deltaCharge: String,
    val vegaCharge: String,
    val curvatureCharge: String,
    val totalCharge: String,
    val tenorCharges: List<TenorChargeDto>? = null,
)
