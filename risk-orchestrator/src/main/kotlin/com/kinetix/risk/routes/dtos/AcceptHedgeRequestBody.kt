package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class AcceptHedgeRequestBody(
    val acceptedBy: String,
)
