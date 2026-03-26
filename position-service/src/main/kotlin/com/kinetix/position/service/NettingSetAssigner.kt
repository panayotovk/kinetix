package com.kinetix.position.service

import com.kinetix.position.client.ReferenceDataServiceClient
import com.kinetix.position.persistence.NettingSetTradeRepository
import org.slf4j.LoggerFactory

class NettingSetAssigner(
    private val referenceDataClient: ReferenceDataServiceClient,
    private val nettingSetTradeRepository: NettingSetTradeRepository,
) {
    private val logger = LoggerFactory.getLogger(NettingSetAssigner::class.java)

    /**
     * Looks up netting agreements for the trade's counterparty and assigns the trade
     * to the first agreement's netting set. If counterpartyId is null, or no agreements
     * are found, or the lookup fails, the method returns without throwing.
     */
    suspend fun assignIfApplicable(tradeId: String, counterpartyId: String?) {
        if (counterpartyId == null) return

        val nettingSetId = try {
            val agreements = referenceDataClient.getNettingAgreementsForCounterparty(counterpartyId)
            agreements.firstOrNull()?.nettingSetId
        } catch (e: Exception) {
            logger.warn(
                "Netting set lookup failed for tradeId={}, counterpartyId={} — skipping assignment: {}",
                tradeId, counterpartyId, e.message,
            )
            return
        }

        if (nettingSetId == null) {
            logger.debug("No netting agreements found for counterpartyId={}, tradeId={}", counterpartyId, tradeId)
            return
        }

        nettingSetTradeRepository.assign(tradeId, nettingSetId)
        logger.info("Trade assigned to netting set: tradeId={}, nettingSetId={}", tradeId, nettingSetId)
    }
}
