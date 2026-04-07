package com.kinetix.gateway.dtos

import kotlinx.serialization.Serializable

@Serializable
data class DataQualityStatusResponse(
    val overall: String,
    val checks: List<DataQualityCheckResponse>,
)
