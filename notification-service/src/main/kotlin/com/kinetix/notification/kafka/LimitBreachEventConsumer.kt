package com.kinetix.notification.kafka

import com.kinetix.common.kafka.RetryableConsumer
import com.kinetix.common.kafka.events.LimitBreachEvent
import com.kinetix.notification.delivery.DeliveryService
import com.kinetix.notification.engine.LimitBreachRule
import com.kinetix.notification.persistence.AlertEventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.coroutines.coroutineContext

class LimitBreachEventConsumer(
    private val consumer: KafkaConsumer<String, String>,
    private val rule: LimitBreachRule = LimitBreachRule(),
    private val deliveryService: DeliveryService? = null,
    private val eventRepository: AlertEventRepository? = null,
    private val topic: String = "limits.breaches",
    private val retryableConsumer: RetryableConsumer = RetryableConsumer(topic = topic),
) {
    private val logger = LoggerFactory.getLogger(LimitBreachEventConsumer::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun start() {
        withContext(Dispatchers.IO) {
            consumer.subscribe(listOf(topic))
        }
        logger.info("Subscribed to topic: {}", topic)
        try {
            while (coroutineContext.isActive) {
                val records = withContext(Dispatchers.IO) {
                    consumer.poll(Duration.ofMillis(100))
                }
                for (record in records) {
                    try {
                        retryableConsumer.process(record.key() ?: "", record.value()) {
                            val event = json.decodeFromString<LimitBreachEvent>(record.value())
                            processEvent(event)
                        }
                    } catch (e: Exception) {
                        logger.error("Failed to process limit-breach event after retries: {}", e.message)
                    }
                }
                if (!records.isEmpty) {
                    withContext(Dispatchers.IO) { consumer.commitSync() }
                }
            }
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                logger.info("Closing limit-breach event Kafka consumer")
                consumer.close(Duration.ofSeconds(10))
            }
        }
    }

    private suspend fun processEvent(event: LimitBreachEvent) {
        val alert = rule.evaluate(event)

        logger.info(
            "Limit breach alert: bookId={} limitType={} severity={} message={}",
            event.bookId, event.limitType, alert.severity, event.message,
        )

        eventRepository?.save(alert)
        deliveryService?.deliver(alert)
    }
}
