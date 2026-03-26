package com.kinetix.risk.model

data class EarlyWarning(
    val signalName: String,
    val currentValue: Double,
    val threshold: Double,
    val proximityPct: Double,
    val message: String,
)
