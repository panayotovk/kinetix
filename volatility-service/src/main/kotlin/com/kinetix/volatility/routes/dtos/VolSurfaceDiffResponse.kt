package com.kinetix.volatility.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class VolSurfaceDiffResponse(
    val instrumentId: String,
    val baseDate: String,
    val compareDate: String,
    val diffs: List<VolPointDiffDto>,
)
