package com.kinetix.risk.client

data class SectorInput(
    val sectorLabel: String,
    val portfolioWeight: Double,
    val benchmarkWeight: Double,
    val portfolioReturn: Double,
    val benchmarkReturn: Double,
)
