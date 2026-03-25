package com.kinetix.regulatory.historical

import com.kinetix.regulatory.client.RiskOrchestratorClient
import com.kinetix.regulatory.historical.dto.ReplayRequest
import com.kinetix.regulatory.historical.dto.ReplayResultResponse

/**
 * Runs a historical scenario replay for a given period against a live book.
 *
 * The service loads the period metadata and its stored daily returns from the
 * repository, then delegates the actual calculation to the risk-orchestrator,
 * which proxies to the risk-engine gRPC [StressTestService.RunHistoricalReplay].
 *
 * Responsibility split:
 *   - regulatory-service owns period metadata, governance, and stored returns
 *   - risk-orchestrator owns position loading and gRPC communication
 */
class HistoricalReplayService(
    private val repository: HistoricalScenarioRepository,
    private val riskOrchestratorClient: RiskOrchestratorClient,
) {

    suspend fun runReplay(periodId: String, request: ReplayRequest): ReplayResultResponse {
        val period = repository.findPeriodById(periodId)
            ?: throw NoSuchElementException("Historical scenario period not found: $periodId")

        val allReturns = repository.findAllReturns(periodId)
        val instrumentReturns = groupReturnsByInstrument(allReturns)

        val result = riskOrchestratorClient.runHistoricalReplay(
            bookId = request.bookId,
            instrumentReturns = instrumentReturns,
            windowStart = period.startDate,
            windowEnd = period.endDate,
        )

        return ReplayResultResponse(
            periodId = periodId,
            scenarioName = result.scenarioName,
            bookId = request.bookId,
            totalPnlImpact = result.totalPnlImpact,
            positionImpacts = result.positionImpacts.map { impact ->
                com.kinetix.regulatory.historical.dto.PositionReplayImpact(
                    instrumentId = impact.instrumentId,
                    assetClass = impact.assetClass,
                    marketValue = impact.marketValue,
                    pnlImpact = impact.pnlImpact,
                    dailyPnl = impact.dailyPnl,
                    proxyUsed = impact.proxyUsed,
                )
            },
            windowStart = result.windowStart,
            windowEnd = result.windowEnd,
            calculatedAt = result.calculatedAt,
        )
    }

    /**
     * Groups a flat list of daily returns into a map of instrumentId -> ordered return values.
     * The returns are already ordered by returnDate from the repository query.
     */
    private fun groupReturnsByInstrument(returns: List<HistoricalScenarioReturn>): Map<String, List<Double>> =
        returns
            .groupBy { it.instrumentId }
            .mapValues { (_, rows) -> rows.map { it.dailyReturn.toDouble() } }
}
