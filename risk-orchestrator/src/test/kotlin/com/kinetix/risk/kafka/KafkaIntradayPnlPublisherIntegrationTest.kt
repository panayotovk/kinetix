package com.kinetix.risk.kafka

import com.kinetix.common.kafka.events.IntradayPnlEvent
import com.kinetix.common.model.BookId
import com.kinetix.risk.model.IntradayPnlSnapshot
import com.kinetix.risk.model.PnlTrigger
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

private fun bd(v: String) = BigDecimal(v)

class KafkaIntradayPnlPublisherIntegrationTest : FunSpec({

    test("publishes intraday P&L snapshot to Kafka and can be consumed") {
        val bootstrapServers = KafkaTestSetup.start()
        val topic = "risk.pnl.intraday.test-1"
        val producer = KafkaTestSetup.createProducer(bootstrapServers)
        val publisher = KafkaIntradayPnlPublisher(producer, topic)

        val snapshot = IntradayPnlSnapshot(
            bookId = BookId("book-1"),
            snapshotAt = Instant.parse("2026-03-24T10:15:00Z"),
            baseCurrency = "USD",
            trigger = PnlTrigger.POSITION_CHANGE,
            totalPnl = bd("12345.67"),
            realisedPnl = bd("5000.00"),
            unrealisedPnl = bd("7345.67"),
            deltaPnl = bd("8000.00"),
            gammaPnl = bd("500.00"),
            vegaPnl = bd("200.00"),
            thetaPnl = bd("-100.00"),
            rhoPnl = bd("50.00"),
            unexplainedPnl = bd("3695.67"),
            highWaterMark = bd("15000.00"),
            correlationId = "corr-abc",
        )

        publisher.publish(snapshot)

        val consumer = KafkaTestSetup.createConsumer(bootstrapServers, "pnl-publisher-test-1")
        consumer.subscribe(listOf(topic))
        val records = consumer.poll(Duration.ofSeconds(10))
        records.count() shouldBe 1

        val record = records.first()
        record.key() shouldBe "book-1"

        val event = Json.decodeFromString<IntradayPnlEvent>(record.value())
        event.bookId shouldBe "book-1"
        event.totalPnl shouldBe "12345.67"
        event.realisedPnl shouldBe "5000.00"
        event.unrealisedPnl shouldBe "7345.67"
        event.highWaterMark shouldBe "15000.00"
        event.trigger shouldBe "position_change"
        event.correlationId shouldBe "corr-abc"

        consumer.close()
        producer.close()
    }

    test("uses book ID as partition key") {
        val bootstrapServers = KafkaTestSetup.start()
        val topic = "risk.pnl.intraday.test-2"
        val producer = KafkaTestSetup.createProducer(bootstrapServers)
        val publisher = KafkaIntradayPnlPublisher(producer, topic)

        val snapshot = IntradayPnlSnapshot(
            bookId = BookId("fx-desk"),
            snapshotAt = Instant.now(),
            baseCurrency = "USD",
            trigger = PnlTrigger.TRADE_BOOKED,
            totalPnl = bd("500.00"),
            realisedPnl = bd("200.00"),
            unrealisedPnl = bd("300.00"),
            deltaPnl = bd("0.00"),
            gammaPnl = bd("0.00"),
            vegaPnl = bd("0.00"),
            thetaPnl = bd("0.00"),
            rhoPnl = bd("0.00"),
            unexplainedPnl = bd("500.00"),
            highWaterMark = bd("500.00"),
        )

        publisher.publish(snapshot)

        val consumer = KafkaTestSetup.createConsumer(bootstrapServers, "pnl-publisher-test-2")
        consumer.subscribe(listOf(topic))
        val records = consumer.poll(Duration.ofSeconds(10))
        records.first().key() shouldBe "fx-desk"

        consumer.close()
        producer.close()
    }
})
