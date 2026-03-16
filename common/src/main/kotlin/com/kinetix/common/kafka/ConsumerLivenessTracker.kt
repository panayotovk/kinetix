package com.kinetix.common.kafka

import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class ConsumerLivenessTracker(
    val topic: String,
    val groupId: String,
) {
    private val _lastProcessedAt = AtomicReference<Instant?>(null)
    private val _recordsProcessedTotal = AtomicLong(0)
    private val _recordsSentToDlqTotal = AtomicLong(0)
    private val _lastErrorAt = AtomicReference<Instant?>(null)
    private val _consecutiveErrorCount = AtomicLong(0)

    val lastProcessedAt: Instant? get() = _lastProcessedAt.get()
    val recordsProcessedTotal: Long get() = _recordsProcessedTotal.get()
    val recordsSentToDlqTotal: Long get() = _recordsSentToDlqTotal.get()
    val lastErrorAt: Instant? get() = _lastErrorAt.get()
    val consecutiveErrorCount: Long get() = _consecutiveErrorCount.get()

    fun recordSuccess() {
        _lastProcessedAt.set(Instant.now())
        _recordsProcessedTotal.incrementAndGet()
        _consecutiveErrorCount.set(0)
    }

    fun recordDlqSend() {
        _recordsSentToDlqTotal.incrementAndGet()
    }

    fun recordError() {
        _lastErrorAt.set(Instant.now())
        _consecutiveErrorCount.incrementAndGet()
    }

    fun isHealthy(stalenessThresholdMs: Long): Boolean {
        if (_consecutiveErrorCount.get() > 0) return false
        val lastProcessed = _lastProcessedAt.get() ?: return true // never processed = just started, OK
        val elapsed = Instant.now().toEpochMilli() - lastProcessed.toEpochMilli()
        return elapsed <= stalenessThresholdMs
    }
}
