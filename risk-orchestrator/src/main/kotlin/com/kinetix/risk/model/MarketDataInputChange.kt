package com.kinetix.risk.model

data class MarketDataInputChange(
    val dataType: String,
    val instrumentId: String,
    val assetClass: String,
    val changeType: MarketDataInputChangeType,
    val baseContentHash: String?,
    val targetContentHash: String?,
    val magnitude: ChangeMagnitude? = null,
)
