package com.kinetix.risk.model

data class BrinsonSectorAttribution(
    val sectorLabel: String,
    val portfolioWeight: Double,
    val benchmarkWeight: Double,
    val portfolioReturn: Double,
    val benchmarkReturn: Double,
    val allocationEffect: Double,
    val selectionEffect: Double,
    val interactionEffect: Double,
    val totalActiveContribution: Double,
)
