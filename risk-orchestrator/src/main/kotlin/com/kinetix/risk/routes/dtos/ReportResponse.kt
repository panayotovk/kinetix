package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ReportResponse(
    val bookId: String,
    val format: String,
    val content: String,
    val generatedAt: String,
)
