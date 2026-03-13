package com.kinetix.risk.service

import com.kinetix.common.model.Position
import com.kinetix.risk.model.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class NoOpRunManifestCapture : RunManifestCapture {
    override suspend fun capture(
        jobId: UUID,
        request: VaRCalculationRequest,
        positions: List<Position>,
        fetchResults: List<FetchResult>,
        modelVersion: String,
        valuationDate: LocalDate,
    ): RunManifest = RunManifest(
        manifestId = UUID.randomUUID(),
        jobId = jobId,
        portfolioId = request.portfolioId.value,
        valuationDate = valuationDate,
        capturedAt = Instant.now(),
        modelVersion = modelVersion,
        calculationType = request.calculationType.name,
        confidenceLevel = request.confidenceLevel.name,
        timeHorizonDays = request.timeHorizonDays,
        numSimulations = request.numSimulations,
        monteCarloSeed = request.monteCarloSeed,
        positionCount = positions.size,
        positionDigest = "",
        marketDataDigest = "",
        inputDigest = "",
        status = ManifestStatus.COMPLETE,
    )
}
