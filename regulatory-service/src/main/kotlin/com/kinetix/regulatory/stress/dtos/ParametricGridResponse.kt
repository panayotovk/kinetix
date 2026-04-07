package com.kinetix.regulatory.stress.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ParametricGridResponse(
    val primaryAxis: String,
    val secondaryAxis: String,
    val cells: List<GridCellResponse>,
    val worstPnlImpact: String?,
)
