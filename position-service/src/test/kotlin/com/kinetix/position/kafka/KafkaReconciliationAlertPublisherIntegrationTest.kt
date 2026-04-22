package com.kinetix.position.kafka

import com.kinetix.common.kafka.events.RiskResultEvent
import com.kinetix.position.fix.PrimeBrokerReconciliation
import com.kinetix.position.fix.ReconciliationBreak
import com.kinetix.position.fix.ReconciliationBreakSeverity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

private fun reconciliation(
    bookId: String = "EQ-001",
    breaks: List<ReconciliationBreak>,
) = PrimeBrokerReconciliation(
    reconciliationDate = "2025-01-15",
    bookId = bookId,
    status = "COMPLETED",
    totalPositions = 10,
    matchedCount = 10 - breaks.size,
    breakCount = breaks.size,
    breaks = breaks,
    reconciledAt = Instant.parse("2025-01-15T17:00:00Z"),
)

private fun breakRow(
    instrumentId: String = "AAPL",
    severity: ReconciliationBreakSeverity = ReconciliationBreakSeverity.CRITICAL,
) = ReconciliationBreak(
    instrumentId = instrumentId,
    internalQty = BigDecimal("1000"),
    primeBrokerQty = BigDecimal("950"),
    breakQty = BigDecimal("50"),
    breakNotional = BigDecimal("15000.00"),
    severity = severity,
)

class KafkaReconciliationAlertPublisherIntegrationTest : FunSpec({

    val bootstrapServers = KafkaTestSetup.start()

    test("publishes RECONCILIATION_BREAK risk event when at least one CRITICAL break exists") {
        val topic = "risk.results.reconciliation-critical-test"
        val producer = KafkaTestSetup.createProducer(bootstrapServers)
        val publisher = KafkaReconciliationAlertPublisher(producer, topic)

        publisher.publishBreakAlert(
            reconciliation(
                breaks = listOf(
                    breakRow(instrumentId = "AAPL", severity = ReconciliationBreakSeverity.CRITICAL),
                    breakRow(instrumentId = "MSFT", severity = ReconciliationBreakSeverity.NORMAL),
                )
            )
        )

        val consumer = KafkaTestSetup.createConsumer(bootstrapServers, "reconciliation-critical-group")
        consumer.subscribe(listOf(topic))

        val records = consumer.poll(Duration.ofSeconds(10))
        records.count() shouldBe 1

        val record = records.first()
        record.key() shouldBe "EQ-001"

        val event = Json.decodeFromString<RiskResultEvent>(record.value())
        event.bookId shouldBe "EQ-001"
        event.calculationType shouldBe "RECONCILIATION_BREAK"

        consumer.close()
        producer.close()
    }

    test("publishes nothing when there are no CRITICAL breaks") {
        val topic = "risk.results.reconciliation-noop-test"
        val producer = KafkaTestSetup.createProducer(bootstrapServers)
        val publisher = KafkaReconciliationAlertPublisher(producer, topic)

        publisher.publishBreakAlert(
            reconciliation(
                breaks = listOf(
                    breakRow(instrumentId = "AAPL", severity = ReconciliationBreakSeverity.NORMAL),
                    breakRow(instrumentId = "MSFT", severity = ReconciliationBreakSeverity.NORMAL),
                )
            )
        )
        publisher.publishBreakAlert(
            reconciliation(breaks = emptyList())
        )

        val consumer = KafkaTestSetup.createConsumer(bootstrapServers, "reconciliation-noop-group")
        consumer.subscribe(listOf(topic))

        val records = consumer.poll(Duration.ofSeconds(3))
        records.count() shouldBe 0

        consumer.close()
        producer.close()
    }

    test("uses bookId as partition key so alerts for the same book land on the same partition") {
        val topic = "risk.results.reconciliation-ordering-test"
        val producer = KafkaTestSetup.createProducer(bootstrapServers)
        val publisher = KafkaReconciliationAlertPublisher(producer, topic)

        publisher.publishBreakAlert(
            reconciliation(bookId = "FX-001", breaks = listOf(breakRow()))
        )
        publisher.publishBreakAlert(
            reconciliation(bookId = "FX-001", breaks = listOf(breakRow(instrumentId = "GBPUSD")))
        )

        val consumer = KafkaTestSetup.createConsumer(bootstrapServers, "reconciliation-ordering-group")
        consumer.subscribe(listOf(topic))

        val records = consumer.poll(Duration.ofSeconds(10))
        records.count() shouldBe 2

        val partitions = records.map { it.partition() }.toSet()
        partitions.size shouldBe 1

        consumer.close()
        producer.close()
    }
})
