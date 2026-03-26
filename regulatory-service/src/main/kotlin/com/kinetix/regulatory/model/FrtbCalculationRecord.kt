package com.kinetix.regulatory.model

import java.math.BigDecimal
import java.time.Instant

data class TenorCharge(
    val tenorLabel: String,
    val sensitivity: BigDecimal,
    val riskWeight: BigDecimal,
    val weightedSensitivity: BigDecimal,
)

data class RiskClassCharge(
    val riskClass: String,
    val deltaCharge: BigDecimal,
    val vegaCharge: BigDecimal,
    val curvatureCharge: BigDecimal,
    val totalCharge: BigDecimal,
    val tenorCharges: List<TenorCharge>? = null,
)

data class FrtbCalculationRecord(
    val id: String,
    val bookId: String,
    val totalSbmCharge: BigDecimal,
    val grossJtd: BigDecimal,
    val hedgeBenefit: BigDecimal,
    val netDrc: BigDecimal,
    val exoticNotional: BigDecimal,
    val otherNotional: BigDecimal,
    val totalRrao: BigDecimal,
    val totalCapitalCharge: BigDecimal,
    val sbmCharges: List<RiskClassCharge>,
    val calculatedAt: Instant,
    val storedAt: Instant,
)
