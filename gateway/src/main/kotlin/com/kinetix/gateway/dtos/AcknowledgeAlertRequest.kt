package com.kinetix.gateway.dtos

import kotlinx.serialization.Serializable

@Serializable
data class AcknowledgeAlertRequest(
    val acknowledgedBy: String,
    val notes: String? = null,
)
