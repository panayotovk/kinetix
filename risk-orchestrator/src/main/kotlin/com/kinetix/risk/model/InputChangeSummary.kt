package com.kinetix.risk.model

data class InputChangeSummary(
    val positionsChanged: Boolean,
    val marketDataChanged: Boolean,
    val modelVersionChanged: Boolean,
    val baseModelVersion: String,
    val targetModelVersion: String,
    val positionChanges: List<PositionInputChange>,
    val marketDataChanges: List<MarketDataInputChange>,
    val baseManifestId: String? = null,
    val targetManifestId: String? = null,
)
