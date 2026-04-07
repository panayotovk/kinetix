package com.kinetix.regulatory.stress.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ApproveScenarioRequest(
    val approvedBy: String,
)
