package com.kinetix.risk.service

import com.kinetix.risk.client.ClientResponse
import com.kinetix.risk.client.ReferenceDataServiceClient
import com.kinetix.risk.client.SaCcrClient
import com.kinetix.risk.client.SaCcrPositionInput
import com.kinetix.risk.client.SaCcrResult
import org.slf4j.LoggerFactory

/**
 * Orchestrates SA-CCR (BCBS 279) regulatory capital calculations.
 *
 * SA-CCR is the REGULATORY capital model — deterministic, formulaic.
 * It is distinct from the Monte Carlo PFE model in CounterpartyRiskOrchestrationService.
 */
class SaCcrService(
    private val referenceDataClient: ReferenceDataServiceClient,
    private val saCcrClient: SaCcrClient,
) {
    private val logger = LoggerFactory.getLogger(SaCcrService::class.java)

    suspend fun calculateSaCcr(
        counterpartyId: String,
        positions: List<SaCcrPositionInput>,
        collateralNet: Double = 0.0,
    ): SaCcrResult {
        when (val resp = referenceDataClient.getCounterparty(counterpartyId)) {
            is ClientResponse.Success -> resp.value
            else -> throw IllegalArgumentException("Counterparty not found: $counterpartyId")
        }

        val nettingSetId = "$counterpartyId-SA-CCR"

        return saCcrClient.calculateSaCcr(
            nettingSetId = nettingSetId,
            counterpartyId = counterpartyId,
            positions = positions,
            collateralNet = collateralNet,
        )
    }
}
