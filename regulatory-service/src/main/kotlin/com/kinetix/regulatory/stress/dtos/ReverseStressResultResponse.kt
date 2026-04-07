package com.kinetix.regulatory.stress.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ReverseStressResultResponse(
    val shocks: List<InstrumentShock>,
    val achievedLoss: String,
    val targetLoss: String,
    val converged: Boolean,
    val calculatedAt: String,
)
