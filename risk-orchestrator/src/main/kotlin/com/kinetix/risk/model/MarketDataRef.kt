package com.kinetix.risk.model

import java.time.Instant

data class MarketDataRef(
    val dataType: String,
    val instrumentId: String,
    val assetClass: String,
    val contentHash: String,
    val status: MarketDataSnapshotStatus,
    val sourceService: String,
    val sourcedAt: Instant,
)

enum class MarketDataSnapshotStatus { FETCHED, MISSING }
