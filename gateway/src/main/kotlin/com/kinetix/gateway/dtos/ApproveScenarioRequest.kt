package com.kinetix.gateway.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ApproveScenarioRequest(
    val approvedBy: String,
)
