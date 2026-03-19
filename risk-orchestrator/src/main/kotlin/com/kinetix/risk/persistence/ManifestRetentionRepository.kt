package com.kinetix.risk.persistence

interface ManifestRetentionRepository {
    suspend fun deleteExpiredManifests(retentionDays: Long): Int
}
