package com.kinetix.risk.persistence

import com.kinetix.common.model.BookId
import com.kinetix.risk.model.IntradayPnlSnapshot
import com.kinetix.risk.model.PnlTrigger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ExposedIntradayPnlRepository(private val db: Database? = null) : IntradayPnlRepository {

    override suspend fun save(snapshot: IntradayPnlSnapshot): Unit = newSuspendedTransaction(db = db) {
        IntradayPnlSnapshotsTable.insert {
            it[bookId] = snapshot.bookId.value
            it[snapshotAt] = OffsetDateTime.ofInstant(snapshot.snapshotAt, ZoneOffset.UTC)
            it[baseCurrency] = snapshot.baseCurrency
            it[trigger] = snapshot.trigger.name
            it[totalPnl] = snapshot.totalPnl
            it[realisedPnl] = snapshot.realisedPnl
            it[unrealisedPnl] = snapshot.unrealisedPnl
            it[deltaPnl] = snapshot.deltaPnl
            it[gammaPnl] = snapshot.gammaPnl
            it[vegaPnl] = snapshot.vegaPnl
            it[thetaPnl] = snapshot.thetaPnl
            it[rhoPnl] = snapshot.rhoPnl
            it[unexplainedPnl] = snapshot.unexplainedPnl
            it[highWaterMark] = snapshot.highWaterMark
            it[correlationId] = snapshot.correlationId
        }
    }

    override suspend fun findLatest(bookId: BookId): IntradayPnlSnapshot? = newSuspendedTransaction(db = db) {
        IntradayPnlSnapshotsTable
            .selectAll()
            .where { IntradayPnlSnapshotsTable.bookId eq bookId.value }
            .orderBy(IntradayPnlSnapshotsTable.snapshotAt, SortOrder.DESC)
            .limit(1)
            .firstOrNull()
            ?.toSnapshot()
    }

    override suspend fun findSeries(
        bookId: BookId,
        from: Instant,
        to: Instant,
    ): List<IntradayPnlSnapshot> = newSuspendedTransaction(db = db) {
        val fromOffset = OffsetDateTime.ofInstant(from, ZoneOffset.UTC)
        val toOffset = OffsetDateTime.ofInstant(to, ZoneOffset.UTC)
        IntradayPnlSnapshotsTable
            .selectAll()
            .where {
                (IntradayPnlSnapshotsTable.bookId eq bookId.value) and
                    (IntradayPnlSnapshotsTable.snapshotAt greaterEq fromOffset) and
                    (IntradayPnlSnapshotsTable.snapshotAt lessEq toOffset)
            }
            .orderBy(IntradayPnlSnapshotsTable.snapshotAt, SortOrder.ASC)
            .map { it.toSnapshot() }
    }

    private fun ResultRow.toSnapshot(): IntradayPnlSnapshot = IntradayPnlSnapshot(
        id = this[IntradayPnlSnapshotsTable.id],
        bookId = BookId(this[IntradayPnlSnapshotsTable.bookId]),
        snapshotAt = this[IntradayPnlSnapshotsTable.snapshotAt].toInstant(),
        baseCurrency = this[IntradayPnlSnapshotsTable.baseCurrency],
        trigger = PnlTrigger.valueOf(this[IntradayPnlSnapshotsTable.trigger]),
        totalPnl = this[IntradayPnlSnapshotsTable.totalPnl],
        realisedPnl = this[IntradayPnlSnapshotsTable.realisedPnl],
        unrealisedPnl = this[IntradayPnlSnapshotsTable.unrealisedPnl],
        deltaPnl = this[IntradayPnlSnapshotsTable.deltaPnl],
        gammaPnl = this[IntradayPnlSnapshotsTable.gammaPnl],
        vegaPnl = this[IntradayPnlSnapshotsTable.vegaPnl],
        thetaPnl = this[IntradayPnlSnapshotsTable.thetaPnl],
        rhoPnl = this[IntradayPnlSnapshotsTable.rhoPnl],
        unexplainedPnl = this[IntradayPnlSnapshotsTable.unexplainedPnl],
        highWaterMark = this[IntradayPnlSnapshotsTable.highWaterMark],
        correlationId = this[IntradayPnlSnapshotsTable.correlationId],
    )
}
