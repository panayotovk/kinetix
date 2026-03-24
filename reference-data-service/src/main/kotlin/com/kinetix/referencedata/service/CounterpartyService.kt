package com.kinetix.referencedata.service

import com.kinetix.referencedata.model.Counterparty
import com.kinetix.referencedata.model.NettingAgreement
import com.kinetix.referencedata.persistence.CounterpartyRepository
import com.kinetix.referencedata.persistence.NettingAgreementRepository

class CounterpartyService(
    private val counterpartyRepository: CounterpartyRepository,
    private val nettingAgreementRepository: NettingAgreementRepository,
) {
    suspend fun findById(counterpartyId: String): Counterparty? =
        counterpartyRepository.findById(counterpartyId)

    suspend fun findAll(): List<Counterparty> =
        counterpartyRepository.findAll()

    suspend fun upsert(counterparty: Counterparty) =
        counterpartyRepository.upsert(counterparty)

    suspend fun findNettingAgreementsForCounterparty(counterpartyId: String): List<NettingAgreement> =
        nettingAgreementRepository.findByCounterpartyId(counterpartyId)

    suspend fun findNettingAgreementById(nettingSetId: String): NettingAgreement? =
        nettingAgreementRepository.findById(nettingSetId)

    suspend fun upsertNettingAgreement(agreement: NettingAgreement) =
        nettingAgreementRepository.upsert(agreement)
}
