package com.kinetix.risk.persistence

import com.kinetix.risk.model.InstrumentFactorLoading
import java.time.LocalDate

interface InstrumentFactorLoadingRepository {
    suspend fun save(loading: InstrumentFactorLoading)
    suspend fun findByInstrumentAndFactor(instrumentId: String, factorName: String): InstrumentFactorLoading?
    suspend fun findByInstrument(instrumentId: String): List<InstrumentFactorLoading>
    suspend fun findStaleByDate(cutoff: LocalDate): List<InstrumentFactorLoading>
}
