package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class BrinsonAttributionResponse(
    val bookId: String,
    val benchmarkId: String,
    val asOfDate: String,
    val sectors: List<BrinsonSectorAttributionResponse>,
    val totalActiveReturn: Double,
    val totalAllocationEffect: Double,
    val totalSelectionEffect: Double,
    val totalInteractionEffect: Double,
)
