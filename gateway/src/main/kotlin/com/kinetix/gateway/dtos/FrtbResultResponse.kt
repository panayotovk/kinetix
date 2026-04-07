package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.FrtbResultSummary
import kotlinx.serialization.Serializable

@Serializable
data class FrtbResultResponse(
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
)

fun FrtbResultSummary.toResponse(): FrtbResultResponse = FrtbResultResponse(
    bookId = bookId,
    sbmCharges = sbmCharges.map { it.toDto() },
    totalSbmCharge = "%.2f".format(totalSbmCharge),
    grossJtd = "%.2f".format(grossJtd),
    hedgeBenefit = "%.2f".format(hedgeBenefit),
    netDrc = "%.2f".format(netDrc),
    exoticNotional = "%.2f".format(exoticNotional),
    otherNotional = "%.2f".format(otherNotional),
    totalRrao = "%.2f".format(totalRrao),
    totalCapitalCharge = "%.2f".format(totalCapitalCharge),
    calculatedAt = calculatedAt.toString(),
)
