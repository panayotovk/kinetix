package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.ReverseStressResultSummary
import kotlinx.serialization.Serializable

@Serializable
data class ReverseStressResponse(
    val shocks: List<InstrumentShockDto>,
    val achievedLoss: String,
    val targetLoss: String,
    val converged: Boolean,
    val calculatedAt: String,
)

fun ReverseStressResultSummary.toResponse(): ReverseStressResponse = ReverseStressResponse(
    shocks = shocks.map { it.toDto() },
    achievedLoss = achievedLoss,
    targetLoss = targetLoss,
    converged = converged,
    calculatedAt = calculatedAt,
)
