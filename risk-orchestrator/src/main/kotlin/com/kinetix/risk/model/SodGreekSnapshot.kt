package com.kinetix.risk.model

import com.kinetix.common.model.BookId
import com.kinetix.common.model.InstrumentId
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * Immutable per-instrument PRICING Greeks locked before market open.
 *
 * These are analytical Black-Scholes sensitivities captured at SOD, distinct from
 * the bump-and-reprice VaR Greeks stored in [DailyRiskSnapshot]. The distinction
 * matters for P&L attribution: BS closed-form partials are used for the Taylor
 * expansion decomposition; VaR Greeks are used for risk aggregation.
 *
 * Once [isLocked] is true the record must never be modified for the trading day.
 */
data class SodGreekSnapshot(
    val id: Long? = null,
    val bookId: BookId,
    val snapshotDate: LocalDate,
    val instrumentId: InstrumentId,
    // SOD market state used to compute the Greeks
    val sodPrice: BigDecimal,
    val sodVol: Double? = null,
    val sodRate: Double? = null,
    // First-order pricing Greeks (analytical BS for options; DV01 for fixed income)
    val delta: Double? = null,
    val gamma: Double? = null,
    val vega: Double? = null,
    val theta: Double? = null,
    val rho: Double? = null,
    // Cross-Greeks (second-order mixed sensitivities, analytical BS)
    val vanna: Double? = null,
    val volga: Double? = null,
    val charm: Double? = null,
    // Fixed-income sensitivities (null for non-fixed-income instruments)
    val bondDv01: Double? = null,
    val swapDv01: Double? = null,
    // Immutability lock
    val isLocked: Boolean = false,
    val lockedAt: Instant? = null,
    val lockedBy: String? = null,
    val createdAt: Instant,
)
