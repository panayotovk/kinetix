package com.kinetix.position.kafka

import com.kinetix.common.kafka.events.LimitBreachEvent

class NoOpLimitBreachEventPublisher : LimitBreachEventPublisher {
    override suspend fun publish(event: LimitBreachEvent) {
        // No-op for dev/test environments without Kafka
    }
}
