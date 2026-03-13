package com.kinetix.risk.service

interface MarketDataBlobStore {
    suspend fun putIfAbsent(contentHash: String, dataType: String, instrumentId: String, assetClass: String, payload: String)
    suspend fun get(contentHash: String): String?
}
