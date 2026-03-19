package com.kinetix.position.reconciliation

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import kotlin.coroutines.coroutineContext

class PositionReconciliationJob(
    private val reconciliationRepository: ReconciliationRepository,
    private val intervalMillis: Long = 86_400_000L,
) {
    private val logger = LoggerFactory.getLogger(PositionReconciliationJob::class.java)

    suspend fun start() {
        while (coroutineContext.isActive) {
            runReconciliation()
            delay(intervalMillis)
        }
    }

    suspend fun runReconciliation() {
        logger.info("Starting trade-to-position reconciliation")
        try {
            val tradeQuantities = reconciliationRepository.findTradeQuantityByPosition()
            val positionQuantities = reconciliationRepository.findCurrentPositionQuantities()

            val tradeMap = tradeQuantities.associateBy { it.portfolioId to it.instrumentId }
            val positionMap = positionQuantities.associateBy { it.portfolioId to it.instrumentId }

            val allKeys = tradeMap.keys + positionMap.keys

            var discrepancyCount = 0
            for (key in allKeys) {
                val tradeQty = tradeMap[key]?.quantity ?: BigDecimal.ZERO
                val positionQty = positionMap[key]?.quantity ?: BigDecimal.ZERO

                if (tradeQty.compareTo(positionQty) != 0) {
                    discrepancyCount++
                    logger.warn(
                        "Reconciliation mismatch: portfolio={} instrument={} tradeQty={} positionQty={}",
                        key.first, key.second, tradeQty, positionQty,
                    )
                }
            }

            if (discrepancyCount == 0) {
                logger.info("Reconciliation complete: all {} positions match trade events", positionQuantities.size)
            } else {
                logger.warn("Reconciliation complete: {} discrepancies found", discrepancyCount)
            }
        } catch (e: Exception) {
            logger.error("Reconciliation failed with error", e)
        }
    }
}
