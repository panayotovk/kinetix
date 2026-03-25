package com.kinetix.risk.client

import com.kinetix.risk.model.BrinsonAttributionResult

interface AttributionEngineClient {
    suspend fun calculateBrinsonAttribution(sectors: List<SectorInput>): BrinsonAttributionResult
}
