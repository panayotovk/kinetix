package com.kinetix.gateway.client

data class InstrumentSummary(
    val instrumentId: String,
    val instrumentType: String,
    val displayName: String,
)

interface InstrumentServiceClient {
    suspend fun fetchAll(): List<InstrumentSummary>
}
