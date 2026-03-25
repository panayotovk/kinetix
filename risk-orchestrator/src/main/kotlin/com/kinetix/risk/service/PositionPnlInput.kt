package com.kinetix.risk.service

import com.kinetix.common.model.AssetClass
import com.kinetix.common.model.InstrumentId
import java.math.BigDecimal

data class PositionPnlInput(
    val instrumentId: InstrumentId,
    val assetClass: AssetClass,
    val totalPnl: BigDecimal,
    // First-order Greeks (pricing sensitivities from SOD snapshot)
    val delta: BigDecimal,
    val gamma: BigDecimal,
    val vega: BigDecimal,
    val theta: BigDecimal,
    val rho: BigDecimal,
    // Cross-Greeks (second-order mixed sensitivities from SOD snapshot)
    val vanna: BigDecimal = BigDecimal.ZERO,
    val volga: BigDecimal = BigDecimal.ZERO,
    val charm: BigDecimal = BigDecimal.ZERO,
    // Market moves since SOD
    val priceChange: BigDecimal,
    val volChange: BigDecimal,
    val rateChange: BigDecimal,
)
