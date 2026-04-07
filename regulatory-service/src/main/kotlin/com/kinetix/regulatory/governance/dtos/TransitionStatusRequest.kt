package com.kinetix.regulatory.governance.dtos

import kotlinx.serialization.Serializable

@Serializable
data class TransitionStatusRequest(
    val targetStatus: String,
    val approvedBy: String? = null,
)
