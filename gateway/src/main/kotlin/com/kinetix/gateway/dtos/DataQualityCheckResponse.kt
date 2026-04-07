package com.kinetix.gateway.dtos

import kotlinx.serialization.Serializable

@Serializable
data class DataQualityCheckResponse(
    val name: String,
    val status: String,
    val message: String,
    val lastChecked: String,
)
