package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class PFEPositionInputDto(
    val instrumentId: String,
    val marketValue: Double,
    val assetClass: String,
    val volatility: Double = 0.20,
    val sector: String = "",
)
