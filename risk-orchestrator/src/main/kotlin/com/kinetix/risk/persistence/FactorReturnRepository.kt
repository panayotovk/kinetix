package com.kinetix.risk.persistence

import com.kinetix.risk.model.FactorReturn
import java.time.LocalDate

interface FactorReturnRepository {
    suspend fun save(factorReturn: FactorReturn)
    suspend fun saveBatch(factorReturns: List<FactorReturn>)
    suspend fun findByFactorAndDate(factorName: String, asOfDate: LocalDate): FactorReturn?
    suspend fun findByFactorAndDateRange(factorName: String, from: LocalDate, to: LocalDate): List<FactorReturn>
}
