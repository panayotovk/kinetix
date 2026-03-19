package com.kinetix.risk.persistence

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class ExposedManifestRetentionRepository(private val db: Database? = null) : ManifestRetentionRepository {

    override suspend fun deleteExpiredManifests(retentionDays: Long): Int = newSuspendedTransaction(db = db) {
        // Delete child rows first to satisfy foreign key constraints, then delete the manifests.
        // All three deletes are performed in the same transaction so the data stays consistent.
        @Suppress("SqlInjection")
        val deleteSnapshots = """
            DELETE FROM run_position_snapshots
            WHERE manifest_id IN (
                SELECT manifest_id FROM run_manifests
                WHERE captured_at < NOW() - INTERVAL '$retentionDays days'
            )
        """.trimIndent()

        @Suppress("SqlInjection")
        val deleteMarketDataRefs = """
            DELETE FROM run_manifest_market_data
            WHERE manifest_id IN (
                SELECT manifest_id FROM run_manifests
                WHERE captured_at < NOW() - INTERVAL '$retentionDays days'
            )
        """.trimIndent()

        @Suppress("SqlInjection")
        val deleteManifests = """
            DELETE FROM run_manifests
            WHERE captured_at < NOW() - INTERVAL '$retentionDays days'
        """.trimIndent()

        connection.prepareStatement(deleteSnapshots, false).executeUpdate()
        connection.prepareStatement(deleteMarketDataRefs, false).executeUpdate()
        connection.prepareStatement(deleteManifests, false).executeUpdate()
    }
}
