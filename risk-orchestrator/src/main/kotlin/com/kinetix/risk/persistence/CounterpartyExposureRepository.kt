package com.kinetix.risk.persistence

import com.kinetix.risk.model.CounterpartyExposureSnapshot

interface CounterpartyExposureRepository {
    suspend fun save(snapshot: CounterpartyExposureSnapshot): CounterpartyExposureSnapshot
    suspend fun findLatestByCounterpartyId(counterpartyId: String): CounterpartyExposureSnapshot?
    suspend fun findByCounterpartyId(counterpartyId: String, limit: Int = 90): List<CounterpartyExposureSnapshot>
    suspend fun findLatestForAllCounterparties(): List<CounterpartyExposureSnapshot>
}
