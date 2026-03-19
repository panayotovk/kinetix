package com.kinetix.risk.service

import com.kinetix.common.model.BookId
import com.kinetix.risk.cache.VaRCache
import com.kinetix.risk.client.PositionProvider
import com.kinetix.risk.model.PnlAttribution
import com.kinetix.risk.persistence.DailyRiskSnapshotRepository
import com.kinetix.risk.persistence.PnlAttributionRepository
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate

class PnlComputationService(
    private val sodSnapshotService: SodSnapshotService,
    private val dailyRiskSnapshotRepository: DailyRiskSnapshotRepository,
    private val pnlAttributionService: PnlAttributionService,
    private val pnlAttributionRepository: PnlAttributionRepository,
    private val varCache: VaRCache,
    private val positionProvider: PositionProvider,
) {
    private val logger = LoggerFactory.getLogger(PnlComputationService::class.java)

    suspend fun compute(
        bookId: BookId,
        date: LocalDate = LocalDate.now(),
    ): PnlAttribution {
        val status = sodSnapshotService.getBaselineStatus(bookId, date)
        if (!status.exists) {
            throw NoSodBaselineException(bookId.value)
        }

        val sodSnapshots = dailyRiskSnapshotRepository.findByBookIdAndDate(bookId, date)
        val currentPositions = positionProvider.getPositions(bookId)

        val inputs = sodSnapshots.map { snapshot ->
            val currentPosition = currentPositions.find { it.instrumentId == snapshot.instrumentId }
            val currentPrice = currentPosition?.marketPrice?.amount ?: snapshot.marketPrice
            val priceChange = currentPrice.subtract(snapshot.marketPrice)
            val totalPnl = priceChange.multiply(snapshot.quantity)

            PositionPnlInput(
                instrumentId = snapshot.instrumentId,
                assetClass = snapshot.assetClass,
                totalPnl = totalPnl,
                delta = BigDecimal.valueOf(snapshot.delta ?: 0.0),
                gamma = BigDecimal.valueOf(snapshot.gamma ?: 0.0),
                vega = BigDecimal.valueOf(snapshot.vega ?: 0.0),
                theta = BigDecimal.valueOf(snapshot.theta ?: 0.0),
                rho = BigDecimal.valueOf(snapshot.rho ?: 0.0),
                priceChange = priceChange,
                volChange = BigDecimal.ZERO,
                rateChange = BigDecimal.ZERO,
            )
        }

        val attribution = pnlAttributionService.attribute(bookId, inputs, date)
        pnlAttributionRepository.save(attribution)

        logger.info(
            "P&L attribution computed for portfolio {} on {}: totalPnl={}",
            bookId.value, date, attribution.totalPnl,
        )

        return attribution
    }
}
