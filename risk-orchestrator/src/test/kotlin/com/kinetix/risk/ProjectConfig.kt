package com.kinetix.risk

import com.kinetix.risk.cache.RedisTestSetup
import com.kinetix.risk.kafka.KafkaTestSetup
import com.kinetix.risk.persistence.DatabaseTestSetup
import io.kotest.core.config.AbstractProjectConfig
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

/**
 * Kotest project-level config that starts all Testcontainers infrastructure
 * in parallel before any spec runs. Without this, containers start lazily and
 * sequentially as each spec first needs them, adding ~20-30s of overhead.
 */
class ProjectConfig : AbstractProjectConfig() {

    private val logger = LoggerFactory.getLogger(ProjectConfig::class.java)

    override suspend fun beforeProject() {
        try {
            logger.info("Starting all test containers in parallel")
            val kafkaFuture = CompletableFuture.runAsync { KafkaTestSetup.start() }
            val dbFuture = CompletableFuture.runAsync { DatabaseTestSetup.startAndMigrate() }
            val redisFuture = CompletableFuture.runAsync { RedisTestSetup.start() }

            CompletableFuture.allOf(kafkaFuture, dbFuture, redisFuture).join()
            logger.info("All test containers started successfully")
        } catch (e: Exception) {
            logger.warn("Parallel container startup failed, containers will start lazily: {}", e.message)
        }
    }
}
