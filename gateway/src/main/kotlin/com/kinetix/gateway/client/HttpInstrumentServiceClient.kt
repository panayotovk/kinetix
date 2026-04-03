package com.kinetix.gateway.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class HttpInstrumentServiceClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : InstrumentServiceClient {

    override suspend fun fetchAll(): List<InstrumentSummary> {
        val response = httpClient.get("$baseUrl/api/v1/instruments")
        val dtos: List<InstrumentDto> = response.body()
        return dtos.map { InstrumentSummary(it.instrumentId, it.instrumentType, it.displayName) }
    }
}
