package com.kinetix.gateway.kafka

import com.kinetix.common.kafka.events.IntradayPnlEvent
import com.kinetix.gateway.websocket.PnlBroadcaster
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import java.time.Instant

private const val TOPIC = "risk.pnl.intraday"

private fun samplePnlEvent(
    bookId: String = "book-1",
    correlationId: String? = "corr-test",
) = IntradayPnlEvent(
    bookId = bookId,
    snapshotAt = Instant.parse("2026-03-24T09:30:00Z").toString(),
    baseCurrency = "USD",
    trigger = "position_change",
    totalPnl = "1500.00",
    realisedPnl = "500.00",
    unrealisedPnl = "1000.00",
    deltaPnl = "1200.00",
    gammaPnl = "80.00",
    vegaPnl = "40.00",
    thetaPnl = "-15.00",
    rhoPnl = "7.00",
    unexplainedPnl = "188.00",
    highWaterMark = "1800.00",
    correlationId = correlationId,
)

/**
 * Builds a mock [KafkaConsumer] that returns the given JSON payloads (with bookId as key)
 * on the first poll() and empty results afterwards. Mirrors the pattern used in
 * notification-service's MarketRegimeEventConsumerTest. We use a mock instead of a real
 * Testcontainers Kafka broker because docker-java's HTTP transport gets a stripped /info
 * response from Docker Desktop in this module's test JVM, while plain Java NIO socket
 * access to the same socket works — root cause undiagnosed (other modules with
 * Testcontainers-postgresql on the classpath are unaffected).
 */
private fun mockConsumerFor(vararg payloadsWithKey: Pair<String, String>): KafkaConsumer<String, String> {
    val tp = TopicPartition(TOPIC, 0)
    val records = ConsumerRecords(
        mapOf(tp to payloadsWithKey.mapIndexed { i, (key, payload) ->
            ConsumerRecord(TOPIC, 0, i.toLong(), key, payload)
        }),
    )
    val emptyRecords = ConsumerRecords<String, String>(emptyMap())

    return mockk<KafkaConsumer<String, String>>().also { mockConsumer ->
        every { mockConsumer.subscribe(any<Collection<String>>()) } returns Unit
        every { mockConsumer.commitSync() } returns Unit
        every { mockConsumer.close(any<Duration>()) } returns Unit
        var callCount = 0
        every { mockConsumer.poll(any<Duration>()) } answers {
            if (callCount++ == 0) records else emptyRecords
        }
    }
}

class KafkaIntradayPnlConsumerTest : FunSpec({

    test("consumes intraday P&L event from Kafka and broadcasts to PnlBroadcaster") {
        val event = samplePnlEvent(bookId = "book-1")
        val payload = Json.encodeToString(event)

        val broadcaster = mockk<PnlBroadcaster>(relaxed = true)
        val captured = slot<IntradayPnlEvent>()
        coEvery { broadcaster.broadcast(capture(captured)) } returns Unit

        val consumer = KafkaIntradayPnlConsumer(
            consumer = mockConsumerFor("book-1" to payload),
            broadcaster = broadcaster,
            topic = TOPIC,
        )

        val job = launch { consumer.start() }
        withTimeout(5_000) {
            while (!captured.isCaptured) delay(50)
        }
        job.cancelAndJoin()

        coVerify(exactly = 1) { broadcaster.broadcast(any()) }
        captured.captured.bookId shouldBe "book-1"
        captured.captured.totalPnl shouldBe "1500.00"
        captured.captured.realisedPnl shouldBe "500.00"
        captured.captured.unrealisedPnl shouldBe "1000.00"
        captured.captured.highWaterMark shouldBe "1800.00"
        captured.captured.trigger shouldBe "position_change"
        captured.captured.correlationId shouldBe "corr-test"
    }

    test("preserves the bookId on records keyed by the same value (verifies partition-key contract)") {
        // The publisher uses bookId as the Kafka record key for partition affinity. The consumer
        // sees that key in the ConsumerRecord — we assert the deserialized event's bookId matches.
        val event = samplePnlEvent(bookId = "fx-desk")
        val payload = Json.encodeToString(event)

        val broadcaster = mockk<PnlBroadcaster>(relaxed = true)
        val captured = slot<IntradayPnlEvent>()
        coEvery { broadcaster.broadcast(capture(captured)) } returns Unit

        val consumer = KafkaIntradayPnlConsumer(
            consumer = mockConsumerFor("fx-desk" to payload),
            broadcaster = broadcaster,
            topic = TOPIC,
        )

        val job = launch { consumer.start() }
        withTimeout(5_000) {
            while (!captured.isCaptured) delay(50)
        }
        job.cancelAndJoin()

        captured.captured.bookId shouldBe "fx-desk"
    }

    test("malformed JSON payload is logged and skipped without halting the consumer loop") {
        val goodPayload = Json.encodeToString(samplePnlEvent(bookId = "book-good"))
        val malformedPayload = "{not-json"

        val broadcaster = mockk<PnlBroadcaster>(relaxed = true)
        val captured = slot<IntradayPnlEvent>()
        coEvery { broadcaster.broadcast(capture(captured)) } returns Unit

        val consumer = KafkaIntradayPnlConsumer(
            consumer = mockConsumerFor(
                "book-malformed" to malformedPayload,
                "book-good" to goodPayload,
            ),
            broadcaster = broadcaster,
            topic = TOPIC,
        )

        val job = launch { consumer.start() }
        withTimeout(5_000) {
            while (!captured.isCaptured) delay(50)
        }
        job.cancelAndJoin()

        // Only the well-formed record produced a broadcast.
        coVerify(exactly = 1) { broadcaster.broadcast(any()) }
        captured.captured.bookId shouldBe "book-good"
    }

    test("broadcaster failure is logged and the consumer continues processing subsequent records") {
        val payload1 = Json.encodeToString(samplePnlEvent(bookId = "book-fail"))
        val payload2 = Json.encodeToString(samplePnlEvent(bookId = "book-second"))

        val broadcaster = mockk<PnlBroadcaster>()
        val seen = mutableListOf<String>()
        coEvery { broadcaster.broadcast(any()) } answers {
            val ev = firstArg<IntradayPnlEvent>()
            seen += ev.bookId
            if (ev.bookId == "book-fail") throw RuntimeException("broadcast failure")
        }

        val consumer = KafkaIntradayPnlConsumer(
            consumer = mockConsumerFor(
                "book-fail" to payload1,
                "book-second" to payload2,
            ),
            broadcaster = broadcaster,
            topic = TOPIC,
        )

        val job = launch { consumer.start() }
        withTimeout(5_000) {
            while (seen.size < 2) delay(50)
        }
        job.cancelAndJoin()

        seen shouldBe listOf("book-fail", "book-second")
    }
})
