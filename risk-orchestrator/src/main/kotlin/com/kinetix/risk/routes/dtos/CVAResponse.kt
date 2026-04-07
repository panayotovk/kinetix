package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class CVAResponse(
    val counterpartyId: String,
    val cva: Double,
    val isEstimated: Boolean,
    val hazardRate: Double,
    val pd1y: Double,
)
