package com.kinetix.position.kafka

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

class PriceConsumerDlqTest : FunSpec({

    test("sends to DLQ topic after max retries exhausted on poison price message") {
        val dlqProducer = mockk<KafkaProducer<String, String>>()
        val recordSlot = slot<ProducerRecord<String, String>>()
        every { dlqProducer.send(capture(recordSlot)) } returns CompletableFuture.completedFuture(null)

        val retryableConsumer = RetryableConsumer(
            topic = "price.updates",
            maxRetries = 2,
            baseDelayMs = 1,
            dlqProducer = dlqProducer,
        )

        val poisonPayload = """{"bad":"price event"}"""

        runTest {
            runCatching {
                retryableConsumer.process("AAPL", poisonPayload) {
                    throw RuntimeException("invalid price format")
                }
            }
        }

        verify(exactly = 1) { dlqProducer.send(any()) }
        recordSlot.captured.topic() shouldBe "price.updates.dlq"
        recordSlot.captured.key() shouldBe "AAPL"
        recordSlot.captured.value() shouldBe poisonPayload
    }

    test("DLQ topic name is price.updates.dlq") {
        val dlqProducer = mockk<KafkaProducer<String, String>>()
        val recordSlot = slot<ProducerRecord<String, String>>()
        every { dlqProducer.send(capture(recordSlot)) } returns CompletableFuture.completedFuture(null)

        val retryableConsumer = RetryableConsumer(
            topic = "price.updates",
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

        recordSlot.captured.topic() shouldBe "price.updates.dlq"
    }

    test("does not send to DLQ when price message is processed successfully") {
        val dlqProducer = mockk<KafkaProducer<String, String>>()

        val retryableConsumer = RetryableConsumer(
            topic = "price.updates",
            maxRetries = 2,
            baseDelayMs = 1,
            dlqProducer = dlqProducer,
        )

        runTest {
            retryableConsumer.process("AAPL", "value") { "ok" }
        }

        verify(exactly = 0) { dlqProducer.send(any()) }
    }
})
