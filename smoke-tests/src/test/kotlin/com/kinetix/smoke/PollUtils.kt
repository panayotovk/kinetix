package com.kinetix.smoke

import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

object PollUtils {
    private val logger = LoggerFactory.getLogger(PollUtils::class.java)

    suspend fun <T> pollUntil(
        timeoutMs: Long = SmokeTestConfig.timeoutMs,
        intervalMs: Long = 500,
        description: String,
        block: suspend () -> T?,
    ): T {
        val start = System.currentTimeMillis()
        var attempt = 0
        while (System.currentTimeMillis() - start < timeoutMs) {
            attempt++
            try {
                val result = block()
                if (result != null) {
                    logger.info("Poll '{}' succeeded after {} attempts ({}ms)", description, attempt, System.currentTimeMillis() - start)
                    return result
                }
            } catch (e: Exception) {
                logger.debug("Poll '{}' attempt {} failed: {}", description, attempt, e.message)
            }
            delay(intervalMs)
        }
        val elapsed = System.currentTimeMillis() - start
        throw AssertionError("Poll '$description' timed out after ${elapsed}ms ($attempt attempts)")
    }
}
