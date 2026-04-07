package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.ReportResult
import kotlinx.serialization.Serializable

@Serializable
data class ReportResponse(
    val bookId: String,
    val format: String,
    val content: String,
    val generatedAt: String,
)

fun ReportResult.toResponse(): ReportResponse = ReportResponse(
    bookId = bookId,
    format = format,
    content = content,
    generatedAt = generatedAt.toString(),
)
