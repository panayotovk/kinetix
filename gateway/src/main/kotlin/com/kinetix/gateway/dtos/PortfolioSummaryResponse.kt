package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.PortfolioSummary
import kotlinx.serialization.Serializable

@Serializable
data class PortfolioSummaryResponse(
    val bookId: String,
)

fun PortfolioSummary.toResponse(): PortfolioSummaryResponse = PortfolioSummaryResponse(
    bookId = id.value,
)
