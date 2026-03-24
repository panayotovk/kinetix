package com.kinetix.referencedata.persistence

import com.kinetix.referencedata.model.Counterparty

interface CounterpartyRepository {
    suspend fun findById(counterpartyId: String): Counterparty?
    suspend fun findAll(): List<Counterparty>
    suspend fun upsert(counterparty: Counterparty)
}
