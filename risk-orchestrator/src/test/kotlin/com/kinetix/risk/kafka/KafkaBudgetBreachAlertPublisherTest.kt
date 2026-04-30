package com.kinetix.risk.kafka

import com.kinetix.common.kafka.events.RiskResultEvent
import com.kinetix.risk.kafka.KafkaBudgetBreachAlertPublisher.Companion.BUDGET_BREACH_SENTINEL_ID
import com.kinetix.risk.model.BreachStatus
import com.kinetix.risk.model.BudgetUtilisation
import com.kinetix.risk.model.HierarchyLevel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.CompletableFuture

class KafkaBudgetBreachAlertPublisherTest : FunSpec({

    fun makePublisher(): Pair<KafkaProducer<String, String>, KafkaBudgetBreachAlertPublisher> {
        val producer = mockk<KafkaProducer<String, String>>()
        return producer to KafkaBudgetBreachAlertPublisher(producer)
    }

    fun breach(
        level: HierarchyLevel = HierarchyLevel.DESK,
        entityId: String = "desk-rates",
        utilisationPct: BigDecimal = BigDecimal("110.00"),
        currentVar: BigDecimal = BigDecimal("5500000.0"),
    ) = BudgetUtilisation(
        entityLevel = level,
        entityId = entityId,
        budgetType = "VAR_BUDGET",
        budgetAmount = BigDecimal("5000000.0"),
        currentVar = currentVar,
        utilisationPct = utilisationPct,
        breachStatus = BreachStatus.BREACH,
        updatedAt = Instant.parse("2026-04-30T12:00:00Z"),
    )

    test("publishes to risk.results topic with entity partition key") {
        val (producer, publisher) = makePublisher()
        val captured = slot<ProducerRecord<String, String>>()
        every { producer.send(capture(captured)) } returns CompletableFuture.completedFuture(mockk())

        publisher.publishBreach(breach())

        captured.captured.topic() shouldBe "risk.results"
        captured.captured.key() shouldBe "DESK:desk-rates"
    }

    test("publishes a sentinel ConcentrationItem with VAR_BUDGET id and utilisation percentage") {
        val (producer, publisher) = makePublisher()
        val captured = slot<ProducerRecord<String, String>>()
        every { producer.send(capture(captured)) } returns CompletableFuture.completedFuture(mockk())

        publisher.publishBreach(breach(utilisationPct = BigDecimal("125.50")))

        val event = Json.decodeFromString<RiskResultEvent>(captured.captured.value())
        event.calculationType shouldBe "BUDGET_BREACH"
        event.concentrationByInstrument!!.size shouldBe 1
        event.concentrationByInstrument!![0].instrumentId shouldBe BUDGET_BREACH_SENTINEL_ID
        event.concentrationByInstrument!![0].percentage shouldBe 125.50
    }

    test("varValue and calculatedAt reflect the breaching utilisation snapshot") {
        val (producer, publisher) = makePublisher()
        val captured = slot<ProducerRecord<String, String>>()
        every { producer.send(capture(captured)) } returns CompletableFuture.completedFuture(mockk())

        publisher.publishBreach(breach(currentVar = BigDecimal("7800000.0")))

        val event = Json.decodeFromString<RiskResultEvent>(captured.captured.value())
        event.varValue shouldBe "7800000.0"
        event.calculatedAt shouldBe "2026-04-30T12:00:00Z"
    }
})
