package com.kinetix.risk.client

import com.kinetix.common.model.BookId
import com.kinetix.common.model.Position
import com.kinetix.proto.risk.DataDependenciesResponse
import com.kinetix.risk.client.dtos.InstrumentDto
import com.kinetix.risk.model.FactorDecompositionSnapshot
import com.kinetix.risk.model.MarketDataValue
import com.kinetix.risk.model.TimeSeriesMarketData
import com.kinetix.risk.model.VaRCalculationRequest
import com.kinetix.risk.model.VaRResult
import com.kinetix.risk.model.ValuationResult

/**
 * Configurable test double for [RiskEngineClient].
 *
 * Set [nextCalculateVaR] before each interaction to control whether the fake
 * succeeds or throws, driving circuit-breaker state transitions without mockk.
 */
class FakeRiskEngineClient : RiskEngineClient {

    /** Lambda invoked by [calculateVaR]. Assign before use. */
    var nextCalculateVaR: () -> VaRResult = { error("FakeRiskEngineClient.nextCalculateVaR not configured") }

    override suspend fun calculateVaR(
        request: VaRCalculationRequest,
        positions: List<Position>,
        marketData: List<MarketDataValue>,
        instrumentMap: Map<String, InstrumentDto>,
    ): VaRResult = nextCalculateVaR()

    override suspend fun valuate(
        request: VaRCalculationRequest,
        positions: List<Position>,
        marketData: List<MarketDataValue>,
        instrumentMap: Map<String, InstrumentDto>,
    ): ValuationResult = error("not implemented in test")

    override suspend fun discoverDependencies(
        positions: List<Position>,
        calculationType: String,
        confidenceLevel: String,
        instrumentMap: Map<String, InstrumentDto>,
    ): DataDependenciesResponse = error("not implemented in test")

    override suspend fun decomposeFactorRisk(
        bookId: BookId,
        positions: List<Position>,
        marketData: Map<String, TimeSeriesMarketData>,
        totalVar: Double,
    ): FactorDecompositionSnapshot = error("not implemented in test")
}
