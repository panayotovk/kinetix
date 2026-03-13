package com.kinetix.risk.persistence

import com.kinetix.risk.model.ReplayRun
import com.kinetix.risk.service.ReplayRunRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class ExposedReplayRunRepository(private val db: Database? = null) : ReplayRunRepository {

    override suspend fun save(replayRun: ReplayRun): Unit = newSuspendedTransaction(db = db) {
        ReplayRunsTable.insert {
            it[replayId] = replayRun.replayId
            it[manifestId] = replayRun.manifestId
            it[originalJobId] = replayRun.originalJobId
            it[replayedAt] = OffsetDateTime.ofInstant(replayRun.replayedAt, ZoneOffset.UTC)
            it[triggeredBy] = replayRun.triggeredBy
            it[replayVarValue] = replayRun.replayVarValue
            it[replayExpectedShortfall] = replayRun.replayExpectedShortfall
            it[replayModelVersion] = replayRun.replayModelVersion
            it[replayOutputDigest] = replayRun.replayOutputDigest
            it[originalVarValue] = replayRun.originalVarValue
            it[originalExpectedShortfall] = replayRun.originalExpectedShortfall
            it[inputDigestMatch] = replayRun.inputDigestMatch
            it[originalInputDigest] = replayRun.originalInputDigest
            it[replayInputDigest] = replayRun.replayInputDigest
        }
    }

    override suspend fun findByManifestId(manifestId: UUID): List<ReplayRun> =
        newSuspendedTransaction(db = db) {
            ReplayRunsTable
                .selectAll()
                .where { ReplayRunsTable.manifestId eq manifestId }
                .orderBy(ReplayRunsTable.replayedAt, SortOrder.DESC)
                .map { it.toReplayRun() }
        }

    override suspend fun findByOriginalJobId(jobId: UUID): List<ReplayRun> =
        newSuspendedTransaction(db = db) {
            ReplayRunsTable
                .selectAll()
                .where { ReplayRunsTable.originalJobId eq jobId }
                .orderBy(ReplayRunsTable.replayedAt, SortOrder.DESC)
                .map { it.toReplayRun() }
        }

    private fun ResultRow.toReplayRun(): ReplayRun = ReplayRun(
        replayId = this[ReplayRunsTable.replayId],
        manifestId = this[ReplayRunsTable.manifestId],
        originalJobId = this[ReplayRunsTable.originalJobId],
        replayedAt = this[ReplayRunsTable.replayedAt].toInstant(),
        triggeredBy = this[ReplayRunsTable.triggeredBy],
        replayVarValue = this[ReplayRunsTable.replayVarValue],
        replayExpectedShortfall = this[ReplayRunsTable.replayExpectedShortfall],
        replayModelVersion = this[ReplayRunsTable.replayModelVersion],
        replayOutputDigest = this[ReplayRunsTable.replayOutputDigest],
        originalVarValue = this[ReplayRunsTable.originalVarValue],
        originalExpectedShortfall = this[ReplayRunsTable.originalExpectedShortfall],
        inputDigestMatch = this[ReplayRunsTable.inputDigestMatch],
        originalInputDigest = this[ReplayRunsTable.originalInputDigest],
        replayInputDigest = this[ReplayRunsTable.replayInputDigest],
    )
}
