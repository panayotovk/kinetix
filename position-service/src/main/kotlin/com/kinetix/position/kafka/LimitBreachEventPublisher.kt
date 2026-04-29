package com.kinetix.position.kafka

import com.kinetix.common.kafka.events.LimitBreachEvent

interface LimitBreachEventPublisher {
    suspend fun publish(event: LimitBreachEvent)
}
