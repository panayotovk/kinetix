package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class NettingSetExposureResponse(
    val nettingSetId: String,
    val agreementType: String,
    val netExposure: Double,
    val peakPfe: Double,
)
