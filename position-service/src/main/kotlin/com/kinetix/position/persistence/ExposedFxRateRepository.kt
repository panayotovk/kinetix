package com.kinetix.position.persistence

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsert
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Currency

class ExposedFxRateRepository(private val db: Database? = null) : FxRateRepository {

    override suspend fun upsert(from: Currency, to: Currency, rate: BigDecimal): Unit =
        newSuspendedTransaction(db = db) {
            FxRatesTable.upsert(FxRatesTable.fromCurrency, FxRatesTable.toCurrency) {
                it[fromCurrency] = from.currencyCode
                it[toCurrency] = to.currencyCode
                it[FxRatesTable.rate] = rate
                it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
            }
        }

    override suspend fun findRate(from: Currency, to: Currency): BigDecimal? =
        newSuspendedTransaction(db = db) {
            FxRatesTable
                .selectAll()
                .where {
                    (FxRatesTable.fromCurrency eq from.currencyCode) and
                        (FxRatesTable.toCurrency eq to.currencyCode)
                }
                .singleOrNull()
                ?.get(FxRatesTable.rate)
        }
}
