package com.kinetix.risk.persistence

import com.kinetix.risk.model.InstrumentFactorLoading
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsert
import java.time.LocalDate

class ExposedInstrumentFactorLoadingRepository(
    private val db: Database? = null,
) : InstrumentFactorLoadingRepository {

    override suspend fun save(loading: InstrumentFactorLoading): Unit =
        newSuspendedTransaction(db = db) {
            InstrumentFactorLoadingsTable.upsert(
                InstrumentFactorLoadingsTable.instrumentId,
                InstrumentFactorLoadingsTable.factorName,
            ) {
                it[instrumentId] = loading.instrumentId
                it[factorName] = loading.factorName
                it[this.loading] = loading.loading
                it[rSquared] = loading.rSquared
                it[method] = loading.method
                it[estimationDate] = loading.estimationDate.toKotlinxDate()
                it[estimationWindow] = loading.estimationWindow
            }
        }

    override suspend fun findByInstrumentAndFactor(
        instrumentId: String,
        factorName: String,
    ): InstrumentFactorLoading? =
        newSuspendedTransaction(db = db) {
            InstrumentFactorLoadingsTable
                .selectAll()
                .where {
                    (InstrumentFactorLoadingsTable.instrumentId eq instrumentId) and
                        (InstrumentFactorLoadingsTable.factorName eq factorName)
                }
                .singleOrNull()
                ?.toModel()
        }

    override suspend fun findByInstrument(instrumentId: String): List<InstrumentFactorLoading> =
        newSuspendedTransaction(db = db) {
            InstrumentFactorLoadingsTable
                .selectAll()
                .where { InstrumentFactorLoadingsTable.instrumentId eq instrumentId }
                .map { it.toModel() }
        }

    override suspend fun findStaleByDate(cutoff: LocalDate): List<InstrumentFactorLoading> =
        newSuspendedTransaction(db = db) {
            InstrumentFactorLoadingsTable
                .selectAll()
                .where { InstrumentFactorLoadingsTable.estimationDate less cutoff.toKotlinxDate() }
                .map { it.toModel() }
        }

    private fun org.jetbrains.exposed.sql.ResultRow.toModel() = InstrumentFactorLoading(
        instrumentId = this[InstrumentFactorLoadingsTable.instrumentId],
        factorName = this[InstrumentFactorLoadingsTable.factorName],
        loading = this[InstrumentFactorLoadingsTable.loading],
        rSquared = this[InstrumentFactorLoadingsTable.rSquared],
        method = this[InstrumentFactorLoadingsTable.method],
        estimationDate = this[InstrumentFactorLoadingsTable.estimationDate].toJavaDate(),
        estimationWindow = this[InstrumentFactorLoadingsTable.estimationWindow],
    )
}
