package com.kinetix.risk.model

data class BrinsonAttributionResult(
    val sectors: List<BrinsonSectorAttribution>,
    val totalActiveReturn: Double,
    val totalAllocationEffect: Double,
    val totalSelectionEffect: Double,
    val totalInteractionEffect: Double,
)
