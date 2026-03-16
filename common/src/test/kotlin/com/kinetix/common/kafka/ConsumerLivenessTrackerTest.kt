package com.kinetix.common.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class ConsumerLivenessTrackerTest : FunSpec({

    test("initial state has zero counters and null timestamps") {
        val tracker = ConsumerLivenessTracker(topic = "test.topic", groupId = "test-group")

        tracker.lastProcessedAt.shouldBeNull()
        tracker.recordsProcessedTotal shouldBe 0
        tracker.recordsSentToDlqTotal shouldBe 0
        tracker.lastErrorAt.shouldBeNull()
        tracker.consecutiveErrorCount shouldBe 0
    }

    test("initial state is healthy because consumer just started") {
        val tracker = ConsumerLivenessTracker(topic = "test.topic", groupId = "test-group")

        tracker.isHealthy(stalenessThresholdMs = 60_000) shouldBe true
    }

    test("recordSuccess updates lastProcessedAt and increments total") {
        val tracker = ConsumerLivenessTracker(topic = "test.topic", groupId = "test-group")

        tracker.recordSuccess()
        tracker.recordSuccess()
        tracker.recordSuccess()

        tracker.lastProcessedAt.shouldNotBeNull()
        tracker.recordsProcessedTotal shouldBe 3
    }

    test("recordSuccess resets consecutiveErrorCount") {
        val tracker = ConsumerLivenessTracker(topic = "test.topic", groupId = "test-group")

        tracker.recordError()
        tracker.recordError()
        tracker.consecutiveErrorCount shouldBe 2

        tracker.recordSuccess()
        tracker.consecutiveErrorCount shouldBe 0
    }

    test("recordDlqSend increments DLQ counter") {
        val tracker = ConsumerLivenessTracker(topic = "test.topic", groupId = "test-group")

        tracker.recordDlqSend()
        tracker.recordDlqSend()

        tracker.recordsSentToDlqTotal shouldBe 2
    }

    test("recordError updates lastErrorAt and increments consecutive count") {
        val tracker = ConsumerLivenessTracker(topic = "test.topic", groupId = "test-group")

        tracker.recordError()

        tracker.lastErrorAt.shouldNotBeNull()
        tracker.consecutiveErrorCount shouldBe 1
    }

    test("isHealthy returns false when consecutiveErrorCount is positive") {
        val tracker = ConsumerLivenessTracker(topic = "test.topic", groupId = "test-group")

        tracker.recordSuccess()
        tracker.recordError()

        tracker.isHealthy(stalenessThresholdMs = 60_000) shouldBe false
    }

    test("isHealthy returns true when recently processed") {
        val tracker = ConsumerLivenessTracker(topic = "test.topic", groupId = "test-group")

        tracker.recordSuccess()

        tracker.isHealthy(stalenessThresholdMs = 60_000) shouldBe true
    }

    test("isHealthy returns false when lastProcessedAt is beyond staleness threshold") {
        val tracker = ConsumerLivenessTracker(topic = "test.topic", groupId = "test-group")

        tracker.recordSuccess()
        Thread.sleep(5)
        // Use a very short threshold so current time exceeds it
        tracker.isHealthy(stalenessThresholdMs = 1) shouldBe false
    }

    test("multiple errors increment consecutiveErrorCount correctly") {
        val tracker = ConsumerLivenessTracker(topic = "test.topic", groupId = "test-group")

        tracker.recordError()
        tracker.recordError()
        tracker.recordError()

        tracker.consecutiveErrorCount shouldBe 3
        tracker.lastErrorAt.shouldNotBeNull()
    }

    test("records processed total is independent of DLQ total") {
        val tracker = ConsumerLivenessTracker(topic = "test.topic", groupId = "test-group")

        tracker.recordSuccess()
        tracker.recordSuccess()
        tracker.recordDlqSend()
        tracker.recordSuccess()

        tracker.recordsProcessedTotal shouldBe 3
        tracker.recordsSentToDlqTotal shouldBe 1
    }

    test("topic and groupId are exposed") {
        val tracker = ConsumerLivenessTracker(topic = "trades.lifecycle", groupId = "audit-service-group")

        tracker.topic shouldBe "trades.lifecycle"
        tracker.groupId shouldBe "audit-service-group"
    }
})
