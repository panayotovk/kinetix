package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.PortfolioAggregationSummary
import kotlinx.serialization.Serializable

@Serializable
data class PortfolioAggregationResponse(
    val bookId: String,
    val baseCurrency: String,
    val totalNav: MoneyDto,
    val totalUnrealizedPnl: MoneyDto,
    val currencyBreakdown: List<CurrencyExposureResponse>,
)

fun PortfolioAggregationSummary.toResponse(): PortfolioAggregationResponse = PortfolioAggregationResponse(
    bookId = bookId,
    baseCurrency = baseCurrency,
    totalNav = totalNav.toDto(),
    totalUnrealizedPnl = totalUnrealizedPnl.toDto(),
    currencyBreakdown = currencyBreakdown.map { it.toResponse() },
)
