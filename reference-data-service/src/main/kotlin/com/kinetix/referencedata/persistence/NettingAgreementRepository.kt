package com.kinetix.referencedata.persistence

import com.kinetix.referencedata.model.NettingAgreement

interface NettingAgreementRepository {
    suspend fun findById(nettingSetId: String): NettingAgreement?
    suspend fun findByCounterpartyId(counterpartyId: String): List<NettingAgreement>
    suspend fun upsert(agreement: NettingAgreement)
}
