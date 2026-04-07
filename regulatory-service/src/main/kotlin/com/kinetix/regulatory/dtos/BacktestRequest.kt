package com.kinetix.regulatory.dtos

import kotlinx.serialization.Serializable

@Serializable
data class BacktestRequest(
    val dailyVarPredictions: List<Double>,
    val dailyPnl: List<Double>,
    val confidenceLevel: Double = 0.99,
    val calculationType: String = "PARAMETRIC",
)
