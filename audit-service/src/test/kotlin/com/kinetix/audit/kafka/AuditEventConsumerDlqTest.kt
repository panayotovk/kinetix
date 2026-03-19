package com.kinetix.audit.kafka

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

class AuditEventConsumerDlqTest : FunSpec({

    test("sends to DLQ topic after max retries exhausted on poison message") {
        val dlqProducer = mockk<KafkaProducer<String, String>>()
        val recordSlot = slot<ProducerRecord<String, String>>()
        every { dlqProducer.send(capture(recordSlot)) } returns CompletableFuture.completedFuture(null)

        val retryableConsumer = RetryableConsumer(
            topic = "trades.lifecycle",
            maxRetries = 2,
            baseDelayMs = 1,
            dlqProducer = dlqProducer,
        )

        val poisonPayload = """{"invalid":"json that will fail parsing"}"""

        runTest {
            runCatching {
                retryableConsumer.process("trade-key", poisonPayload) {
                    throw RuntimeException("permanent parse failure")
                }
            }
        }

        verify(exactly = 1) { dlqProducer.send(any()) }
        recordSlot.captured.topic() shouldBe "trades.lifecycle.dlq"
        recordSlot.captured.key() shouldBe "trade-key"
        recordSlot.captured.value() shouldBe poisonPayload
    }

    test("DLQ topic name is trades.lifecycle.dlq suffix") {
        val dlqProducer = mockk<KafkaProducer<String, String>>()
        val recordSlot = slot<ProducerRecord<String, String>>()
        every { dlqProducer.send(capture(recordSlot)) } returns CompletableFuture.completedFuture(null)

        val retryableConsumer = RetryableConsumer(
            topic = "trades.lifecycle",
            maxRetries = 1,
            baseDelayMs = 1,
            dlqProducer = dlqProducer,
        )

        runTest {
            runCatching {
                retryableConsumer.process("key", "value") {
                    throw RuntimeException("fail")
                }
            }
        }

        recordSlot.captured.topic() shouldBe "trades.lifecycle.dlq"
    }

    test("does not send to DLQ when message is processed successfully") {
        val dlqProducer = mockk<KafkaProducer<String, String>>()

        val retryableConsumer = RetryableConsumer(
            topic = "trades.lifecycle",
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
