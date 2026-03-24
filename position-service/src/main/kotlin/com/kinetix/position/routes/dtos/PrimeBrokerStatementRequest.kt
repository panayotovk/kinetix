package com.kinetix.position.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class PrimeBrokerStatementRequest(
    val bookId: String,
    val date: String,
    val positions: List<PrimeBrokerPositionDto>,
)

@Serializable
data class PrimeBrokerPositionDto(
    val instrumentId: String,
    val quantity: String,
    val price: String,
)
