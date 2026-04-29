package com.kinetix.notification.kafka

import com.kinetix.common.kafka.events.LimitBreachEvent
import com.kinetix.notification.delivery.DeliveryService
import com.kinetix.notification.engine.LimitBreachRule
import com.kinetix.notification.model.AlertEvent
import com.kinetix.notification.model.AlertType
import com.kinetix.notification.model.Severity
import com.kinetix.notification.persistence.InMemoryAlertEventRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
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

private fun breachEvent(
    bookId: String = "BOOK-001",
    limitType: String = "VAR",
    severity: String = "HARD",
    correlationId: String? = null,
) = LimitBreachEvent(
    eventId = "evt-1",
    bookId = bookId,
    limitType = limitType,
    severity = severity,
    currentValue = "1500000",
    limitValue = "1000000",
    message = "VaR exceeds firm limit",
    breachedAt = "2026-04-29T10:00:00Z",
    tradeId = "t-1",
    correlationId = correlationId,
)

private fun kafkaConsumerFor(vararg payloads: String): KafkaConsumer<String, String> {
    val tp = TopicPartition("limits.breaches", 0)
    val records = ConsumerRecords(
        mapOf(tp to payloads.mapIndexed { i, p -> ConsumerRecord("limits.breaches", 0, i.toLong(), "key", p) })
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

class LimitBreachEventConsumerTest : FunSpec({

    test("a HARD breach is persisted and delivered as a CRITICAL alert") {
        val payload = Json.encodeToString(
            LimitBreachEvent.serializer(),
            breachEvent(bookId = "BOOK-001", severity = "HARD"),
        )

        val deliveryService = mockk<DeliveryService>(relaxed = true)
        val repo = InMemoryAlertEventRepository()

        val consumer = LimitBreachEventConsumer(
            consumer = kafkaConsumerFor(payload),
            rule = LimitBreachRule(),
            deliveryService = deliveryService,
            eventRepository = repo,
        )

        val job = launch { consumer.start() }
        delay(200)
        job.cancelAndJoin()

        val saved = repo.findRecent(10)
        saved shouldHaveSize 1
        saved[0].type shouldBe AlertType.LIMIT_BREACH
        saved[0].severity shouldBe Severity.CRITICAL
        saved[0].ruleId shouldBe "LIMIT_BREACH"
        saved[0].bookId shouldBe "BOOK-001"
        saved[0].message shouldBe "VaR exceeds firm limit"

        coVerify(exactly = 1) { deliveryService.deliver(any<AlertEvent>()) }
    }

    test("a SOFT breach is downgraded to WARNING") {
        val payload = Json.encodeToString(
            LimitBreachEvent.serializer(),
            breachEvent(severity = "SOFT"),
        )

        val repo = InMemoryAlertEventRepository()
        val consumer = LimitBreachEventConsumer(
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

    test("currentValue and threshold are populated from the event's numeric strings") {
        val payload = Json.encodeToString(
            LimitBreachEvent.serializer(),
            breachEvent(),
        )

        val repo = InMemoryAlertEventRepository()
        val consumer = LimitBreachEventConsumer(
            consumer = kafkaConsumerFor(payload),
            eventRepository = repo,
        )

        val job = launch { consumer.start() }
        delay(200)
        job.cancelAndJoin()

        val saved = repo.findRecent(10).first()
        saved.currentValue shouldBe 1_500_000.0
        saved.threshold shouldBe 1_000_000.0
    }

    test("malformed JSON payload is caught by RetryableConsumer and does not halt the consumer loop") {
        val malformed = "this-is-not-valid-json"
        val goodPayload = Json.encodeToString(
            LimitBreachEvent.serializer(),
            breachEvent(),
        )

        val repo = InMemoryAlertEventRepository()
        val consumer = LimitBreachEventConsumer(
            consumer = kafkaConsumerFor(malformed, goodPayload),
            eventRepository = repo,
        )

        val job = launch { consumer.start() }
        delay(500)
        job.cancelAndJoin()

        val saved = repo.findRecent(10)
        saved shouldHaveSize 1
        saved[0].bookId shouldBe "BOOK-001"
    }

    test("correlationId flows from the breach event through the generated alert") {
        val payload = Json.encodeToString(
            LimitBreachEvent.serializer(),
            breachEvent(correlationId = "corr-breach-1"),
        )

        val repo = InMemoryAlertEventRepository()
        val consumer = LimitBreachEventConsumer(
            consumer = kafkaConsumerFor(payload),
            eventRepository = repo,
        )

        val job = launch { consumer.start() }
        delay(200)
        job.cancelAndJoin()

        repo.findRecent(10)[0].correlationId shouldBe "corr-breach-1"
    }
})
