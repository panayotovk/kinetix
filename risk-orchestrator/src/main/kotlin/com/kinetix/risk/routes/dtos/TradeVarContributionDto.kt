package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class TradeVarContributionDto(
    val instrumentId: String,
    val side: String,
    val quantity: String,
    val marginalVarImpact: String,
    val executionCost: String,
)
