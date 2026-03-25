package com.kinetix.risk.persistence

import com.kinetix.common.model.BookId
import com.kinetix.common.model.InstrumentId
import com.kinetix.risk.model.SodGreekSnapshot
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ExposedSodGreekSnapshotRepository(private val db: Database? = null) : SodGreekSnapshotRepository {

    override suspend fun saveAll(snapshots: List<SodGreekSnapshot>): Unit = newSuspendedTransaction(db = db) {
        SodGreekSnapshotsTable.batchInsert(snapshots, ignore = true) { s ->
            this[SodGreekSnapshotsTable.bookId] = s.bookId.value
            this[SodGreekSnapshotsTable.snapshotDate] = s.snapshotDate.toKotlinxDate()
            this[SodGreekSnapshotsTable.instrumentId] = s.instrumentId.value
            this[SodGreekSnapshotsTable.sodPrice] = s.sodPrice
            this[SodGreekSnapshotsTable.sodVol] = s.sodVol
            this[SodGreekSnapshotsTable.sodRate] = s.sodRate
            this[SodGreekSnapshotsTable.delta] = s.delta
            this[SodGreekSnapshotsTable.gamma] = s.gamma
            this[SodGreekSnapshotsTable.vega] = s.vega
            this[SodGreekSnapshotsTable.theta] = s.theta
            this[SodGreekSnapshotsTable.rho] = s.rho
            this[SodGreekSnapshotsTable.vanna] = s.vanna
            this[SodGreekSnapshotsTable.volga] = s.volga
            this[SodGreekSnapshotsTable.charm] = s.charm
            this[SodGreekSnapshotsTable.bondDv01] = s.bondDv01
            this[SodGreekSnapshotsTable.swapDv01] = s.swapDv01
            this[SodGreekSnapshotsTable.isLocked] = s.isLocked
            this[SodGreekSnapshotsTable.lockedAt] = s.lockedAt?.let { OffsetDateTime.ofInstant(it, ZoneOffset.UTC) }
            this[SodGreekSnapshotsTable.lockedBy] = s.lockedBy
            this[SodGreekSnapshotsTable.createdAt] = OffsetDateTime.ofInstant(s.createdAt, ZoneOffset.UTC)
        }
    }

    override suspend fun findByBookIdAndDate(
        bookId: BookId,
        date: LocalDate,
    ): List<SodGreekSnapshot> = newSuspendedTransaction(db = db) {
        SodGreekSnapshotsTable
            .selectAll()
            .where {
                (SodGreekSnapshotsTable.bookId eq bookId.value) and
                    (SodGreekSnapshotsTable.snapshotDate eq date.toKotlinxDate())
            }
            .map { it.toSodGreekSnapshot() }
    }

    override suspend fun findByInstrumentAndDate(
        bookId: BookId,
        instrumentId: InstrumentId,
        date: LocalDate,
    ): SodGreekSnapshot? = newSuspendedTransaction(db = db) {
        SodGreekSnapshotsTable
            .selectAll()
            .where {
                (SodGreekSnapshotsTable.bookId eq bookId.value) and
                    (SodGreekSnapshotsTable.snapshotDate eq date.toKotlinxDate()) and
                    (SodGreekSnapshotsTable.instrumentId eq instrumentId.value)
            }
            .firstOrNull()
            ?.toSodGreekSnapshot()
    }

    override suspend fun lockAll(
        bookId: BookId,
        date: LocalDate,
        lockedBy: String,
    ): Unit = newSuspendedTransaction(db = db) {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        SodGreekSnapshotsTable.update(
            where = {
                (SodGreekSnapshotsTable.bookId eq bookId.value) and
                    (SodGreekSnapshotsTable.snapshotDate eq date.toKotlinxDate()) and
                    (SodGreekSnapshotsTable.isLocked eq false)
            },
        ) {
            it[SodGreekSnapshotsTable.isLocked] = true
            it[SodGreekSnapshotsTable.lockedAt] = now
            it[SodGreekSnapshotsTable.lockedBy] = lockedBy
        }
    }

    private fun ResultRow.toSodGreekSnapshot(): SodGreekSnapshot = SodGreekSnapshot(
        id = this[SodGreekSnapshotsTable.id],
        bookId = BookId(this[SodGreekSnapshotsTable.bookId]),
        snapshotDate = this[SodGreekSnapshotsTable.snapshotDate].toJavaDate(),
        instrumentId = InstrumentId(this[SodGreekSnapshotsTable.instrumentId]),
        sodPrice = this[SodGreekSnapshotsTable.sodPrice],
        sodVol = this[SodGreekSnapshotsTable.sodVol],
        sodRate = this[SodGreekSnapshotsTable.sodRate],
        delta = this[SodGreekSnapshotsTable.delta],
        gamma = this[SodGreekSnapshotsTable.gamma],
        vega = this[SodGreekSnapshotsTable.vega],
        theta = this[SodGreekSnapshotsTable.theta],
        rho = this[SodGreekSnapshotsTable.rho],
        vanna = this[SodGreekSnapshotsTable.vanna],
        volga = this[SodGreekSnapshotsTable.volga],
        charm = this[SodGreekSnapshotsTable.charm],
        bondDv01 = this[SodGreekSnapshotsTable.bondDv01],
        swapDv01 = this[SodGreekSnapshotsTable.swapDv01],
        isLocked = this[SodGreekSnapshotsTable.isLocked],
        lockedAt = this[SodGreekSnapshotsTable.lockedAt]?.toInstant(),
        lockedBy = this[SodGreekSnapshotsTable.lockedBy],
        createdAt = this[SodGreekSnapshotsTable.createdAt].toInstant(),
    )
}
