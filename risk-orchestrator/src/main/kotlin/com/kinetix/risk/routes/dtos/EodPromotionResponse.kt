package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class EodPromotionResponse(
    val jobId: String,
    val bookId: String,
    val valuationDate: String,
    val runLabel: String,
    val promotedAt: String?,
    val promotedBy: String?,
)
