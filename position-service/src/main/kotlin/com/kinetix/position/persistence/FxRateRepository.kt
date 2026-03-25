package com.kinetix.position.persistence

import java.math.BigDecimal
import java.util.Currency

interface FxRateRepository {
    suspend fun upsert(from: Currency, to: Currency, rate: BigDecimal)
    suspend fun findRate(from: Currency, to: Currency): BigDecimal?
}
