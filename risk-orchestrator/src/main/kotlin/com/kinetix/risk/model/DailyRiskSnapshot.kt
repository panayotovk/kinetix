package com.kinetix.risk.model

import com.kinetix.common.model.AssetClass
import com.kinetix.common.model.InstrumentId
import com.kinetix.common.model.BookId
import java.math.BigDecimal
import java.time.LocalDate

data class DailyRiskSnapshot(
    val id: Long? = null,
    val bookId: BookId,
    val snapshotDate: LocalDate,
    val instrumentId: InstrumentId,
    val assetClass: AssetClass,
    val quantity: BigDecimal,
    val marketPrice: BigDecimal,
    val delta: Double? = null,
    val gamma: Double? = null,
    val vega: Double? = null,
    val theta: Double? = null,
    val rho: Double? = null,
    val varContribution: BigDecimal? = null,
    val esContribution: BigDecimal? = null,
    /** Implied volatility (ATM, 1-month tenor) captured at start-of-day. Null when no vol surface exists. */
    val sodVol: Double? = null,
    /** Risk-free rate (1Y tenor) captured at start-of-day. Null when no rate data exists. */
    val sodRate: Double? = null,
)
