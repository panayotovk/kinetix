package com.kinetix.risk.persistence

import com.kinetix.risk.model.FactorReturn
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsert
import java.time.LocalDate

class ExposedFactorReturnRepository(
    private val db: Database? = null,
) : FactorReturnRepository {

    override suspend fun save(factorReturn: FactorReturn): Unit =
        newSuspendedTransaction(db = db) {
            FactorReturnsTable.upsert(
                FactorReturnsTable.factorName,
                FactorReturnsTable.asOfDate,
            ) {
                it[factorName] = factorReturn.factorName
                it[asOfDate] = factorReturn.asOfDate.toKotlinxDate()
                it[returnValue] = factorReturn.returnValue
                it[returnSource] = factorReturn.source
            }
        }

    override suspend fun saveBatch(factorReturns: List<FactorReturn>): Unit =
        newSuspendedTransaction(db = db) {
            factorReturns.forEach { fr ->
                FactorReturnsTable.upsert(
                    FactorReturnsTable.factorName,
                    FactorReturnsTable.asOfDate,
                ) {
                    it[factorName] = fr.factorName
                    it[asOfDate] = fr.asOfDate.toKotlinxDate()
                    it[returnValue] = fr.returnValue
                    it[returnSource] = fr.source
                }
            }
        }

    override suspend fun findByFactorAndDate(factorName: String, asOfDate: LocalDate): FactorReturn? =
        newSuspendedTransaction(db = db) {
            FactorReturnsTable
                .selectAll()
                .where {
                    (FactorReturnsTable.factorName eq factorName) and
                        (FactorReturnsTable.asOfDate eq asOfDate.toKotlinxDate())
                }
                .singleOrNull()
                ?.let { row ->
                    FactorReturn(
                        factorName = row[FactorReturnsTable.factorName],
                        asOfDate = row[FactorReturnsTable.asOfDate].toJavaDate(),
                        returnValue = row[FactorReturnsTable.returnValue],
                        source = row[FactorReturnsTable.returnSource],
                    )
                }
        }

    override suspend fun findByFactorAndDateRange(
        factorName: String,
        from: LocalDate,
        to: LocalDate,
    ): List<FactorReturn> =
        newSuspendedTransaction(db = db) {
            FactorReturnsTable
                .selectAll()
                .where {
                    (FactorReturnsTable.factorName eq factorName) and
                        (FactorReturnsTable.asOfDate greaterEq from.toKotlinxDate()) and
                        (FactorReturnsTable.asOfDate lessEq to.toKotlinxDate())
                }
                .orderBy(FactorReturnsTable.asOfDate, SortOrder.ASC)
                .map { row ->
                    FactorReturn(
                        factorName = row[FactorReturnsTable.factorName],
                        asOfDate = row[FactorReturnsTable.asOfDate].toJavaDate(),
                        returnValue = row[FactorReturnsTable.returnValue],
                        source = row[FactorReturnsTable.returnSource],
                    )
                }
        }
}
