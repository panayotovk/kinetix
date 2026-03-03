package com.kinetix.risk.client.dtos

import kotlinx.serialization.Serializable

@Serializable
data class LimitDefinitionDto(
    val id: String,
    val level: String,
    val entityId: String,
    val limitType: String,
    val limitValue: String,
    val intradayLimit: String? = null,
    val overnightLimit: String? = null,
    val active: Boolean,
)
