package com.kinetix.regulatory.dtos

import kotlinx.serialization.Serializable

@Serializable
data class RiskClassChargeDto(
    val riskClass: String,
    val deltaCharge: String,
    val vegaCharge: String,
    val curvatureCharge: String,
    val totalCharge: String,
    val tenorCharges: List<TenorChargeDto>? = null,
)
