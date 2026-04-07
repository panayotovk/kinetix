package com.kinetix.risk.model

data class PositionGreek(
    val instrumentId: String,
    val delta: Double,
    val gamma: Double,
    val vega: Double,
    val theta: Double,
    val rho: Double,
)
