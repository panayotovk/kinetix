package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class MarketDataQuantDiffResponse(
    val dataType: String,
    val instrumentId: String,
    val magnitude: String,
    val diagnostic: Boolean = true,
)
