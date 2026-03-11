package com.kinetix.position.kafka

import com.kinetix.common.model.TradeEvent

interface TradeEventPublisher {
    suspend fun publish(event: TradeEvent)
}
