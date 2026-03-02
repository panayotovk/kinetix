package com.kinetix.risk.persistence

import kotlinx.serialization.Serializable

@Serializable
data class AssetClassGreeksJson(
    val assetClass: String,
    val delta: Double,
    val gamma: Double,
    val vega: Double,
)
