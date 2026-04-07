package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class PositionGreekDto(
    val instrumentId: String,
    val delta: String,
    val gamma: String,
    val vega: String,
    val theta: String,
    val rho: String,
)
