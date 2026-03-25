package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class BrinsonSectorAttributionResponse(
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
