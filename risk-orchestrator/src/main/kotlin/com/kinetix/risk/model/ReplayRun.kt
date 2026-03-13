package com.kinetix.risk.model

import java.time.Instant
import java.util.UUID

data class ReplayRun(
    val replayId: UUID,
    val manifestId: UUID,
    val originalJobId: UUID,
    val replayedAt: Instant,
    val triggeredBy: String,
    val replayVarValue: Double?,
    val replayExpectedShortfall: Double?,
    val replayModelVersion: String?,
    val replayOutputDigest: String?,
    val originalVarValue: Double?,
    val originalExpectedShortfall: Double?,
    val inputDigestMatch: Boolean,
    val originalInputDigest: String,
    val replayInputDigest: String,
)
