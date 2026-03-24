package com.kinetix.gateway.kafka

import com.kinetix.common.kafka.events.IntradayPnlEvent
import com.kinetix.gateway.websocket.PnlBroadcaster
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Instant

class KafkaIntradayPnlConsumerIntegrationTest : FunSpec({

    test("consumes intraday P&L event from Kafka and broadcasts to PnlBroadcaster") {
        val bootstrapServers = KafkaTestSetup.start()
        val topic = "risk.pnl.intraday.gw-test-1"
        val producer = KafkaTestSetup.createProducer(bootstrapServers)

        val event = IntradayPnlEvent(
            bookId = "book-1",
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
            correlationId = "corr-test",
        )
        producer.send(ProducerRecord(topic, event.bookId, Json.encodeToString(event))).get()
        producer.close()

        val broadcaster = mockk<PnlBroadcaster>(relaxed = true)
        val captured = slot<IntradayPnlEvent>()
        coEvery { broadcaster.broadcast(capture(captured)) } returns Unit

        val kafkaConsumer = KafkaTestSetup.createConsumer(bootstrapServers, "gateway-pnl-test-1")
        val consumer = KafkaIntradayPnlConsumer(kafkaConsumer, broadcaster, topic)

        val job = launch { consumer.start() }

        withTimeout(15_000) {
            while (!captured.isCaptured) {
                kotlinx.coroutines.delay(100)
            }
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

    test("uses book ID as partition key") {
        val bootstrapServers = KafkaTestSetup.start()
        val topic = "risk.pnl.intraday.gw-test-2"
        val producer = KafkaTestSetup.createProducer(bootstrapServers)

        val event = IntradayPnlEvent(
            bookId = "fx-desk",
            snapshotAt = Instant.now().toString(),
            baseCurrency = "USD",
            trigger = "trade_booked",
            totalPnl = "500.00",
            realisedPnl = "200.00",
            unrealisedPnl = "300.00",
            deltaPnl = "400.00",
            gammaPnl = "20.00",
            vegaPnl = "10.00",
            thetaPnl = "-5.00",
            rhoPnl = "2.00",
            unexplainedPnl = "73.00",
            highWaterMark = "500.00",
        )
        producer.send(ProducerRecord(topic, event.bookId, Json.encodeToString(event))).get()
        producer.close()

        // Verify key is set correctly by consuming directly
        val directConsumer = KafkaTestSetup.createConsumer(bootstrapServers, "direct-key-check")
        directConsumer.subscribe(listOf(topic))
        val records = directConsumer.poll(java.time.Duration.ofSeconds(10))
        records.first().key() shouldBe "fx-desk"
        directConsumer.close()
    }
})
