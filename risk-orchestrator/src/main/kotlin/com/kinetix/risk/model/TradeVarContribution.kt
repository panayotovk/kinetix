package com.kinetix.risk.model

data class TradeVarContribution(
    val instrumentId: String,
    val side: String,
    val quantity: String,
    val marginalVarImpact: Double,
    val executionCost: Double,
)
