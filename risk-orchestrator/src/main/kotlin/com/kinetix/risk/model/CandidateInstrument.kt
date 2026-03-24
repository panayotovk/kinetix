package com.kinetix.risk.model

/**
 * A candidate instrument for hedge suggestion. Carries per-unit Greek sensitivities
 * and pricing data needed by the analytical hedge calculator.
 */
data class CandidateInstrument(
    val instrumentId: String,
    val instrumentType: String,
    val pricePerUnit: Double,
    val bidAskSpreadBps: Double,
    val deltaPerUnit: Double,
    val gammaPerUnit: Double,
    val vegaPerUnit: Double,
    val thetaPerUnit: Double,
    val rhoPerUnit: Double,
    val liquidityTier: String,
    /** Age of the price data in minutes. Used to flag STALE (>= 15 min) data quality. */
    val priceAgeMinutes: Int,
)
