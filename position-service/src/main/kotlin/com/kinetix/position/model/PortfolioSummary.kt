package com.kinetix.position.model

import com.kinetix.common.model.Money
import com.kinetix.common.model.BookId
import java.util.Currency

data class PortfolioSummary(
    val portfolioId: BookId,
    val baseCurrency: Currency,
    val totalNav: Money,
    val totalUnrealizedPnl: Money,
    val currencyBreakdown: List<CurrencyExposure>,
)
