package com.kinetix.risk.service

import com.kinetix.common.model.BookId
import com.kinetix.risk.client.AttributionEngineClient
import com.kinetix.risk.client.BenchmarkServiceClient
import com.kinetix.risk.client.ClientResponse
import com.kinetix.risk.client.PositionProvider
import com.kinetix.risk.client.SectorInput
import com.kinetix.risk.model.BrinsonAttributionResult
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Orchestrates Brinson-Hood-Beebower performance attribution for a book against a benchmark.
 *
 * Portfolio weights are computed from position market values relative to the total book market value.
 * Benchmark weights are read from the benchmark's constituent list as of the requested date.
 * Returns are set to zero — the current implementation uses this as a structural skeleton that
 * can be extended once historical price returns are wired in.
 */
class BenchmarkAttributionService(
    private val positionProvider: PositionProvider,
    private val benchmarkServiceClient: BenchmarkServiceClient,
    private val attributionEngineClient: AttributionEngineClient,
) {
    private val logger = LoggerFactory.getLogger(BenchmarkAttributionService::class.java)

    suspend fun calculateAttribution(
        bookId: BookId,
        benchmarkId: String,
        asOfDate: LocalDate,
    ): BrinsonAttributionResult {
        val positions = positionProvider.getPositions(bookId)
        require(positions.isNotEmpty()) {
            "No positions found for book ${bookId.value}; cannot compute attribution"
        }

        val benchmarkDetail = when (val resp = benchmarkServiceClient.getBenchmarkDetail(benchmarkId, asOfDate)) {
            is ClientResponse.Success -> resp.value
            is ClientResponse.NotFound -> throw IllegalArgumentException(
                "Benchmark not found: $benchmarkId"
            )
        }

        val benchmarkWeightByInstrument: Map<String, Double> = benchmarkDetail.constituents
            .associate { it.instrumentId to it.weight.toDouble() }

        val totalMarketValue: BigDecimal = positions.sumOf { it.marketValue.amount }
        require(totalMarketValue > BigDecimal.ZERO) {
            "Total market value is zero for book ${bookId.value}; cannot compute portfolio weights"
        }

        val sectors = positions.map { position ->
            val instrumentId = position.instrumentId.value
            val portfolioWeight = position.marketValue.amount
                .divide(totalMarketValue, 10, java.math.RoundingMode.HALF_UP)
                .toDouble()
            val benchmarkWeight = benchmarkWeightByInstrument[instrumentId] ?: 0.0
            SectorInput(
                sectorLabel = instrumentId,
                portfolioWeight = portfolioWeight,
                benchmarkWeight = benchmarkWeight,
                portfolioReturn = 0.0,
                benchmarkReturn = 0.0,
            )
        }

        logger.debug(
            "Calculating Brinson attribution for book={} benchmark={} asOfDate={} sectors={}",
            bookId.value, benchmarkId, asOfDate, sectors.size,
        )

        return attributionEngineClient.calculateBrinsonAttribution(sectors)
    }
}
