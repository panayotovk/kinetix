package com.kinetix.risk.client.dtos

import com.kinetix.common.model.BookId
import kotlinx.serialization.Serializable

@Serializable
data class PortfolioSummaryDto(
    val portfolioId: String,
) {
    fun toDomain(): BookId = BookId(portfolioId)
}
