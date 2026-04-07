package com.kinetix.regulatory.stress.dtos

import kotlinx.serialization.Serializable

@Serializable
data class GridCellResponse(
    val primaryAxis: String,
    val primaryShock: Double,
    val secondaryAxis: String,
    val secondaryShock: Double,
    val pnlImpact: String,
)
