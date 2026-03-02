package com.kinetix.risk.persistence

import kotlinx.serialization.Serializable

@Serializable
data class PositionRiskJson(
    val instrumentId: String,
    val assetClass: String,
    val marketValue: String,
    val delta: Double?,
    val gamma: Double?,
    val vega: Double?,
    val varContribution: String,
    val esContribution: String,
    val percentageOfTotal: String,
)
