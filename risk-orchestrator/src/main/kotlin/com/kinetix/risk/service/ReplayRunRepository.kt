package com.kinetix.risk.service

import com.kinetix.risk.model.ReplayRun
import java.util.UUID

interface ReplayRunRepository {
    suspend fun save(replayRun: ReplayRun)
    suspend fun findByManifestId(manifestId: UUID): List<ReplayRun>
    suspend fun findByOriginalJobId(jobId: UUID): List<ReplayRun>
}
