package com.kinetix.position.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ExecutionCostResponse(
    val orderId: String,
    val bookId: String,
    val instrumentId: String,
    val completedAt: String,
    val arrivalPrice: String,
    val averageFillPrice: String,
    val side: String,
    val totalQty: String,
    val slippageBps: String,
    val marketImpactBps: String?,
    val timingCostBps: String?,
    val totalCostBps: String,
)
