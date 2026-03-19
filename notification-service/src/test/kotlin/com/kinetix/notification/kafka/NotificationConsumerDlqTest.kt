package com.kinetix.notification.kafka

import com.kinetix.common.kafka.RetryableConsumer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.concurrent.CompletableFuture

class NotificationConsumerDlqTest : FunSpec({

    test("RiskResultConsumer sends to risk.results.dlq after max retries exhausted") {
        val dlqProducer = mockk<KafkaProducer<String, String>>()
        val recordSlot = slot<ProducerRecord<String, String>>()
        every { dlqProducer.send(capture(recordSlot)) } returns CompletableFuture.completedFuture(null)

        val retryableConsumer = RetryableConsumer(
            topic = "risk.results",
            maxRetries = 2,
            baseDelayMs = 1,
            dlqProducer = dlqProducer,
        )

        val poisonPayload = """{"bad":"risk result"}"""

        runTest {
            runCatching {
                retryableConsumer.process("book-1", poisonPayload) {
                    throw RuntimeException("failed to evaluate risk result")
                }
            }
        }

        verify(exactly = 1) { dlqProducer.send(any()) }
        recordSlot.captured.topic() shouldBe "risk.results.dlq"
        recordSlot.captured.key() shouldBe "book-1"
        recordSlot.captured.value() shouldBe poisonPayload
    }

    test("AnomalyEventConsumer sends to risk.anomalies.dlq after max retries exhausted") {
        val dlqProducer = mockk<KafkaProducer<String, String>>()
        val recordSlot = slot<ProducerRecord<String, String>>()
        every { dlqProducer.send(capture(recordSlot)) } returns CompletableFuture.completedFuture(null)

        val retryableConsumer = RetryableConsumer(
            topic = "risk.anomalies",
            maxRetries = 2,
            baseDelayMs = 1,
            dlqProducer = dlqProducer,
        )

        val poisonPayload = """{"bad":"anomaly event"}"""

        runTest {
            runCatching {
                retryableConsumer.process("anomaly-key", poisonPayload) {
                    throw RuntimeException("failed to process anomaly")
                }
            }
        }

        verify(exactly = 1) { dlqProducer.send(any()) }
        recordSlot.captured.topic() shouldBe "risk.anomalies.dlq"
        recordSlot.captured.key() shouldBe "anomaly-key"
        recordSlot.captured.value() shouldBe poisonPayload
    }

    test("risk.results DLQ topic name has correct suffix") {
        val dlqProducer = mockk<KafkaProducer<String, String>>()
        val recordSlot = slot<ProducerRecord<String, String>>()
        every { dlqProducer.send(capture(recordSlot)) } returns CompletableFuture.completedFuture(null)

        val retryableConsumer = RetryableConsumer(
            topic = "risk.results",
            maxRetries = 1,
            baseDelayMs = 1,
            dlqProducer = dlqProducer,
        )

        runTest {
            runCatching {
                retryableConsumer.process("key", "value") { throw RuntimeException("fail") }
            }
        }

        recordSlot.captured.topic() shouldBe "risk.results.dlq"
    }

    test("risk.anomalies DLQ topic name has correct suffix") {
        val dlqProducer = mockk<KafkaProducer<String, String>>()
        val recordSlot = slot<ProducerRecord<String, String>>()
        every { dlqProducer.send(capture(recordSlot)) } returns CompletableFuture.completedFuture(null)

        val retryableConsumer = RetryableConsumer(
            topic = "risk.anomalies",
            maxRetries = 1,
            baseDelayMs = 1,
            dlqProducer = dlqProducer,
        )

        runTest {
            runCatching {
                retryableConsumer.process("key", "value") { throw RuntimeException("fail") }
            }
        }

        recordSlot.captured.topic() shouldBe "risk.anomalies.dlq"
    }

    test("does not send to DLQ when risk result is processed successfully") {
        val dlqProducer = mockk<KafkaProducer<String, String>>()

        val retryableConsumer = RetryableConsumer(
            topic = "risk.results",
            maxRetries = 2,
            baseDelayMs = 1,
            dlqProducer = dlqProducer,
        )

        runTest {
            retryableConsumer.process("key", "value") { "ok" }
        }

        verify(exactly = 0) { dlqProducer.send(any()) }
    }
})
