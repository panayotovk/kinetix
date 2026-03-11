package com.kinetix.position.kafka

import com.kinetix.common.model.TradeEvent

class NoOpTradeEventPublisher : TradeEventPublisher {
    override suspend fun publish(event: TradeEvent) {
        // No-op for dev: avoids requiring Kafka producer
    }
}
