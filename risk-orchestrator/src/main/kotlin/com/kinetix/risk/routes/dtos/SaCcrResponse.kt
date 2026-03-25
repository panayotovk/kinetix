package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

/**
 * HTTP response for SA-CCR (BCBS 279) regulatory EAD calculation.
 *
 * Note: pfeAddon here is the SA-CCR regulatory add-on computed via the
 * deterministic BCBS 279 formula.  It is NOT the Monte Carlo PFE exposure
 * returned by the CalculatePFE endpoint.
 */
@Serializable
data class SaCcrResponse(
    val nettingSetId: String,
    val counterpartyId: String,
    val replacementCost: Double,
    val pfeAddon: Double,
    val multiplier: Double,
    val ead: Double,
    val alpha: Double,
)
