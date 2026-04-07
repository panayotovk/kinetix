package com.kinetix.gateway.dtos

import kotlinx.serialization.Serializable

@Serializable
data class GenerateReportRequest(
    val format: String? = null,
)
