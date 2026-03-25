package com.kinetix.risk.model

import com.kinetix.common.model.AssetClass
import com.kinetix.common.model.InstrumentId
import java.math.BigDecimal

data class PositionPnlAttribution(
    val instrumentId: InstrumentId,
    val assetClass: AssetClass,
    val totalPnl: BigDecimal,
    // First-order Greek attribution
    val deltaPnl: BigDecimal,
    val gammaPnl: BigDecimal,
    val vegaPnl: BigDecimal,
    val thetaPnl: BigDecimal,
    val rhoPnl: BigDecimal,
    // Cross-Greek attribution (second-order mixed terms)
    val vannaPnl: BigDecimal = BigDecimal.ZERO,
    val volgaPnl: BigDecimal = BigDecimal.ZERO,
    val charmPnl: BigDecimal = BigDecimal.ZERO,
    val crossGammaPnl: BigDecimal = BigDecimal.ZERO,
    // Residual: total_pnl minus sum of all attributed terms
    val unexplainedPnl: BigDecimal,
)
