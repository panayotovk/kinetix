package com.kinetix.gateway.kafka

import com.kinetix.common.kafka.events.IntradayPnlEvent
import com.kinetix.gateway.websocket.PnlBroadcaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.coroutines.coroutineContext

class KafkaIntradayPnlConsumer(
    private val consumer: KafkaConsumer<String, String>,
    private val broadcaster: PnlBroadcaster,
    private val topic: String = "risk.pnl.intraday",
) {
    private val logger = LoggerFactory.getLogger(KafkaIntradayPnlConsumer::class.java)

    suspend fun start() {
        withContext(Dispatchers.IO) {
            consumer.subscribe(listOf(topic))
        }
        try {
            while (coroutineContext.isActive) {
                val records = withContext(Dispatchers.IO) {
                    consumer.poll(Duration.ofMillis(100))
                }
                for (record in records) {
                    val event = try {
                        Json.decodeFromString<IntradayPnlEvent>(record.value())
                    } catch (e: Exception) {
                        logger.error("Failed to deserialize IntradayPnlEvent from topic {}: {}", topic, e.message)
                        continue
                    }
                    try {
                        broadcaster.broadcast(event)
                    } catch (e: Exception) {
                        logger.error("Failed to broadcast P&L event for book {}: {}", event.bookId, e.message)
                    }
                }
                if (!records.isEmpty) {
                    withContext(Dispatchers.IO) { consumer.commitSync() }
                }
            }
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                logger.info("Closing intraday P&L Kafka consumer")
                consumer.close(Duration.ofSeconds(10))
            }
        }
    }
}
