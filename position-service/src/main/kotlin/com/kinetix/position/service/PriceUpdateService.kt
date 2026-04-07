package com.kinetix.position.service

import com.kinetix.common.model.InstrumentId
import com.kinetix.common.model.Money
import com.kinetix.common.model.Position
import com.kinetix.position.persistence.PositionRepository
import org.slf4j.LoggerFactory

class PriceUpdateService(
    private val positionRepository: PositionRepository,
) {
    private val logger = LoggerFactory.getLogger(PriceUpdateService::class.java)

    suspend fun handle(instrumentId: InstrumentId, newPrice: Money): Int {
        val positions = positionRepository.findByInstrumentId(instrumentId)
        if (positions.isEmpty()) {
            return 0
        }

        val updatedPositions = mutableListOf<Position>()
        for (position in positions) {
            if (position.currency != newPrice.currency) {
                logger.debug(
                    "Skipping position {}/{}: currency mismatch (position={}, price={})",
                    position.bookId.value, instrumentId.value,
                    position.currency.currencyCode, newPrice.currency.currencyCode,
                )
                continue
            }
            updatedPositions.add(position.markToMarket(newPrice))
        }

        if (updatedPositions.isNotEmpty()) {
            positionRepository.saveAll(updatedPositions)
        }

        logger.info(
            "Updated {} positions for instrument {} to price {} {}",
            updatedPositions.size, instrumentId.value, newPrice.amount.toPlainString(), newPrice.currency.currencyCode,
        )
        return updatedPositions.size
    }
}
