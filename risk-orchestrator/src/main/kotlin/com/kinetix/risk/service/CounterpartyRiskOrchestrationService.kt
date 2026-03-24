package com.kinetix.risk.service

import com.kinetix.risk.client.CVAResult
import com.kinetix.risk.client.ClientResponse
import com.kinetix.risk.client.CounterpartyRiskClient
import com.kinetix.risk.client.PFEPositionInput
import com.kinetix.risk.client.ReferenceDataServiceClient
import com.kinetix.risk.model.CounterpartyExposureSnapshot
import com.kinetix.risk.model.ExposureAtTenor
import com.kinetix.risk.persistence.CounterpartyExposureRepository
import java.time.Instant

class CounterpartyRiskOrchestrationService(
    private val referenceDataClient: ReferenceDataServiceClient,
    private val counterpartyRiskClient: CounterpartyRiskClient,
    private val repository: CounterpartyExposureRepository,
) {
    /**
     * Fetches netting set data for the counterparty, runs PFE via Monte Carlo for each
     * netting set, and persists the combined snapshot.
     *
     * For v1: assumes positions all belong to the first netting agreement found.
     * If no netting agreement exists, treats all positions as a single un-netted set.
     */
    suspend fun computeAndPersistPFE(
        counterpartyId: String,
        positions: List<PFEPositionInput>,
        numSimulations: Int = 0,
        seed: Long = 0L,
    ): CounterpartyExposureSnapshot {
        val counterparty = when (val resp = referenceDataClient.getCounterparty(counterpartyId)) {
            is ClientResponse.Success -> resp.value
            else -> throw IllegalArgumentException("Counterparty not found: $counterpartyId")
        }

        val nettingAgreements = when (val resp = referenceDataClient.getNettingAgreements(counterpartyId)) {
            is ClientResponse.Success -> resp.value
            else -> emptyList()
        }

        val primaryAgreement = nettingAgreements.firstOrNull()
        val nettingSetId = primaryAgreement?.nettingSetId ?: "$counterpartyId-DEFAULT"
        val agreementType = primaryAgreement?.agreementType ?: "NONE"

        val pfeResult = counterpartyRiskClient.calculatePFE(
            counterpartyId = counterpartyId,
            nettingSetId = nettingSetId,
            agreementType = agreementType,
            positions = positions,
            numSimulations = numSimulations,
            seed = seed,
        )

        val peakPfe = if (pfeResult.exposureProfile.isEmpty()) 0.0
        else pfeResult.exposureProfile.maxOf { it.pfe95 }

        val snapshot = CounterpartyExposureSnapshot(
            counterpartyId = counterpartyId,
            calculatedAt = Instant.now(),
            pfeProfile = pfeResult.exposureProfile,
            currentNetExposure = pfeResult.netExposure,
            peakPfe = peakPfe,
            cva = null,
            cvaEstimated = false,
        )

        return repository.save(snapshot)
    }

    /**
     * Computes CVA for the counterparty using its credit data from reference data service
     * and a pre-computed exposure profile (typically from a PFE run).
     */
    suspend fun computeCVA(
        counterpartyId: String,
        exposureProfile: List<ExposureAtTenor>,
    ): CVAResult {
        val counterparty = when (val resp = referenceDataClient.getCounterparty(counterpartyId)) {
            is ClientResponse.Success -> resp.value
            else -> throw IllegalArgumentException("Counterparty not found: $counterpartyId")
        }

        return counterpartyRiskClient.calculateCVA(
            counterpartyId = counterpartyId,
            exposureProfile = exposureProfile,
            lgd = counterparty.lgd,
            pd1y = counterparty.pd1y ?: 0.0,
            cdsSpreadssBps = counterparty.cdsSpreadBps ?: 0.0,
            rating = counterparty.ratingSp ?: "",
            sector = counterparty.sector ?: "",
            riskFreeRate = 0.0,
        )
    }

    suspend fun getLatestExposure(counterpartyId: String): CounterpartyExposureSnapshot? =
        repository.findLatestByCounterpartyId(counterpartyId)

    suspend fun getExposureHistory(counterpartyId: String, limit: Int = 90): List<CounterpartyExposureSnapshot> =
        repository.findByCounterpartyId(counterpartyId, limit)

    suspend fun getAllLatestExposures(): List<CounterpartyExposureSnapshot> =
        repository.findLatestForAllCounterparties()
}
