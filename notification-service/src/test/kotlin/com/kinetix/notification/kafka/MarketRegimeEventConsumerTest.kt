package com.kinetix.notification.kafka

import com.kinetix.common.kafka.events.MarketRegimeEvent
import com.kinetix.notification.delivery.DeliveryService
import com.kinetix.notification.engine.RegimeChangeRule
import com.kinetix.notification.model.AlertEvent
import com.kinetix.notification.model.AlertType
import com.kinetix.notification.model.Severity
import com.kinetix.notification.persistence.InMemoryAlertEventRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import java.time.Duration

private fun regimeEvent(
    from: String,
    to: String,
    degradedInputs: Boolean = false,
    correlationId: String? = null,
) = MarketRegimeEvent(
    regime = to,
    previousRegime = from,
    transitionedAt = "2025-01-15T10:00:00Z",
    confidence = "0.92",
    degradedInputs = degradedInputs,
    consecutiveObservations = 5,
    realisedVol20d = "0.25",
    crossAssetCorrelation = "0.65",
    effectiveCalculationType = "MONTE_CARLO",
    effectiveConfidenceLevel = "CL_99",
    effectiveTimeHorizonDays = 1,
    effectiveCorrelationMethod = "EWMA",
    correlationId = correlationId,
)

private fun kafkaConsumerFor(vararg payloads: String): KafkaConsumer<String, String> {
    val tp = TopicPartition("risk.regime.changes", 0)
    val records = ConsumerRecords(
        mapOf(tp to payloads.mapIndexed { i, p -> ConsumerRecord("risk.regime.changes", 0, i.toLong(), "key", p) })
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

class MarketRegimeEventConsumerTest : FunSpec({

    test("NORMAL -> CRISIS triggers a CRITICAL regime-change alert and delivers it") {
        val event = regimeEvent(from = "NORMAL", to = "CRISIS")
        val payload = Json.encodeToString(MarketRegimeEvent.serializer(), event)

        val deliveryService = mockk<DeliveryService>(relaxed = true)
        val repo = InMemoryAlertEventRepository()

        val consumer = MarketRegimeEventConsumer(
            consumer = kafkaConsumerFor(payload),
            rule = RegimeChangeRule(),
            deliveryService = deliveryService,
            eventRepository = repo,
        )

        val job = launch { consumer.start() }
        delay(200)
        job.cancelAndJoin()

        val saved = repo.findRecent(10)
        saved shouldHaveSize 1
        saved[0].type shouldBe AlertType.REGIME_CHANGE
        saved[0].severity shouldBe Severity.CRITICAL
        saved[0].ruleId shouldBe "REGIME_CHANGE"

        coVerify(exactly = 1) { deliveryService.deliver(any<AlertEvent>()) }
    }

    test("NORMAL -> ELEVATED_VOL triggers a WARNING alert") {
        val payload = Json.encodeToString(
            MarketRegimeEvent.serializer(),
            regimeEvent(from = "NORMAL", to = "ELEVATED_VOL"),
        )

        val repo = InMemoryAlertEventRepository()
        val consumer = MarketRegimeEventConsumer(
            consumer = kafkaConsumerFor(payload),
            eventRepository = repo,
        )

        val job = launch { consumer.start() }
        delay(200)
        job.cancelAndJoin()

        val saved = repo.findRecent(10)
        saved shouldHaveSize 1
        saved[0].severity shouldBe Severity.WARNING
    }

    test("NORMAL -> NORMAL produces no alert and no delivery") {
        val payload = Json.encodeToString(
            MarketRegimeEvent.serializer(),
            regimeEvent(from = "NORMAL", to = "NORMAL"),
        )

        val deliveryService = mockk<DeliveryService>(relaxed = true)
        val repo = InMemoryAlertEventRepository()

        val consumer = MarketRegimeEventConsumer(
            consumer = kafkaConsumerFor(payload),
            deliveryService = deliveryService,
            eventRepository = repo,
        )

        val job = launch { consumer.start() }
        delay(200)
        job.cancelAndJoin()

        repo.findRecent(10).shouldBeEmpty()
        coVerify(exactly = 0) { deliveryService.deliver(any<AlertEvent>()) }
    }

    test("malformed JSON payload is caught by RetryableConsumer and does not halt the consumer loop") {
        val malformed = "this-is-not-valid-json"
        val goodPayload = Json.encodeToString(
            MarketRegimeEvent.serializer(),
            regimeEvent(from = "NORMAL", to = "CRISIS"),
        )

        val repo = InMemoryAlertEventRepository()
        val consumer = MarketRegimeEventConsumer(
            consumer = kafkaConsumerFor(malformed, goodPayload),
            eventRepository = repo,
        )

        val job = launch { consumer.start() }
        delay(500)
        job.cancelAndJoin()

        // The good payload must still be processed — the malformed one does not block the loop.
        val saved = repo.findRecent(10)
        saved shouldHaveSize 1
        saved[0].severity shouldBe Severity.CRITICAL
    }

    test("correlationId flows from the regime event through the generated alert") {
        val payload = Json.encodeToString(
            MarketRegimeEvent.serializer(),
            regimeEvent(from = "NORMAL", to = "CRISIS", correlationId = "corr-regime-1"),
        )

        val repo = InMemoryAlertEventRepository()
        val consumer = MarketRegimeEventConsumer(
            consumer = kafkaConsumerFor(payload),
            eventRepository = repo,
        )

        val job = launch { consumer.start() }
        delay(200)
        job.cancelAndJoin()

        repo.findRecent(10)[0].correlationId shouldBe "corr-regime-1"
    }
})
