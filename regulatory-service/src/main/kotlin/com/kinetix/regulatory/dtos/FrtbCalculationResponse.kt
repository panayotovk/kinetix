package com.kinetix.regulatory.dtos

import kotlinx.serialization.Serializable

@Serializable
data class FrtbCalculationResponse(
    val id: String,
    val bookId: String,
    val sbmCharges: List<RiskClassChargeDto>,
    val totalSbmCharge: String,
    val grossJtd: String,
    val hedgeBenefit: String,
    val netDrc: String,
    val exoticNotional: String,
    val otherNotional: String,
    val totalRrao: String,
    val totalCapitalCharge: String,
    val calculatedAt: String,
    val storedAt: String,
)
