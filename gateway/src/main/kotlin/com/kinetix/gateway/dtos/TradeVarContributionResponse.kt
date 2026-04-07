package com.kinetix.gateway.dtos

import kotlinx.serialization.Serializable

@Serializable
data class TradeVarContributionResponse(
    val instrumentId: String,
    val side: String,
    val quantity: String,
    val marginalVarImpact: String,
    val executionCost: String,
)
