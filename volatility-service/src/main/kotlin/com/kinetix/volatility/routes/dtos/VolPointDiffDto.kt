package com.kinetix.volatility.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class VolPointDiffDto(
    val strike: Double,
    val maturityDays: Int,
    val baseVol: Double,
    val compareVol: Double,
    val diff: Double,
)
