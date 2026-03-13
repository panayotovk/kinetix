package com.kinetix.risk.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object ReplayRunsTable : Table("replay_runs") {
    val replayId = uuid("replay_id")
    val manifestId = uuid("manifest_id").references(RunManifestsTable.manifestId)
    val originalJobId = uuid("original_job_id")
    val replayedAt = timestampWithTimeZone("replayed_at")
    val triggeredBy = varchar("triggered_by", 255)
    val replayVarValue = double("replay_var_value").nullable()
    val replayExpectedShortfall = double("replay_expected_shortfall").nullable()
    val replayModelVersion = varchar("replay_model_version", 100).nullable()
    val replayOutputDigest = varchar("replay_output_digest", 64).nullable()
    val originalVarValue = double("original_var_value").nullable()
    val originalExpectedShortfall = double("original_expected_shortfall").nullable()
    val inputDigestMatch = bool("input_digest_match")
    val originalInputDigest = varchar("original_input_digest", 64)
    val replayInputDigest = varchar("replay_input_digest", 64)

    override val primaryKey = PrimaryKey(replayId)
}
