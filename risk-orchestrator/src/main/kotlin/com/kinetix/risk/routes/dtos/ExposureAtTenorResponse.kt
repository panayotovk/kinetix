package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ExposureAtTenorResponse(
    val tenor: String,
    val tenorYears: Double,
    val expectedExposure: Double,
    val pfe95: Double,
    val pfe99: Double,
)
