package com.kinetix.risk.service

import com.kinetix.common.model.BookId
import com.kinetix.common.model.Position
import com.kinetix.risk.client.RiskEngineClient
import com.kinetix.risk.model.FactorDecompositionSnapshot
import com.kinetix.risk.model.TimeSeriesMarketData
import com.kinetix.risk.persistence.FactorDecompositionRepository
import org.slf4j.LoggerFactory

class FactorRiskService(
    private val riskEngineClient: RiskEngineClient,
    private val repository: FactorDecompositionRepository,
) {
    private val logger = LoggerFactory.getLogger(FactorRiskService::class.java)

    /**
     * Decomposes the portfolio VaR for [bookId] into systematic and idiosyncratic
     * components using the 5-factor model in the risk engine.
     *
     * @param marketData map of instrument id → HISTORICAL_PRICES time series,
     *   as fetched earlier in the VaR pipeline. Factor proxy returns are extracted
     *   from entries keyed by the proxy instrument ids (IDX-SPX, US10Y, EURUSD, …).
     * @param totalVar the total portfolio VaR computed in the preceding valuation step.
     * @return the persisted [FactorDecompositionSnapshot], or null if there are no
     *   positions or if the gRPC call fails.
     */
    suspend fun decompose(
        bookId: BookId,
        positions: List<Position>,
        marketData: Map<String, TimeSeriesMarketData>,
        totalVar: Double,
    ): FactorDecompositionSnapshot? {
        if (positions.isEmpty()) {
            logger.info("No positions for book {}, skipping factor decomposition", bookId.value)
            return null
        }

        val snapshot = try {
            riskEngineClient.decomposeFactorRisk(bookId, positions, marketData, totalVar)
        } catch (e: Exception) {
            logger.warn(
                "Factor risk decomposition failed for book {}, skipping persistence",
                bookId.value, e,
            )
            return null
        }

        repository.save(snapshot)

        logger.info(
            "Factor decomposition complete for book {}: totalVar={}, systematicVar={}, " +
                "idiosyncraticVar={}, rSquared={:.4f}, concentrationWarning={}",
            bookId.value, snapshot.totalVar, snapshot.systematicVar,
            snapshot.idiosyncraticVar, snapshot.rSquared, snapshot.concentrationWarning,
        )

        return snapshot
    }
}
