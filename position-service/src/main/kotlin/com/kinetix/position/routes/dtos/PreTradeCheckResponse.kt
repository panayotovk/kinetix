package com.kinetix.position.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class PreTradeCheckResponse(
    val approved: Boolean,
    val result: String,
    val warnings: List<LimitBreachDto>,
    val breaches: List<LimitBreachDto>,
)

@Serializable
data class LimitBreachDto(
    val limitType: String,
    val severity: String,
    val currentValue: String,
    val limitValue: String,
    val message: String,
)
