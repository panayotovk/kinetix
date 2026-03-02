package com.kinetix.risk.persistence

import kotlinx.serialization.Serializable

@Serializable
data class ComponentBreakdownJson(
    val assetClass: String,
    val varContribution: Double,
    val percentageOfTotal: Double,
)
