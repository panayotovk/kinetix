package com.kinetix.common.model

import java.util.UUID

data class TradeEvent(
    val trade: Trade,
    val correlationId: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val userRole: String? = null,
) {
    /** Derives the audit-facing event type from the trade's lifecycle action. */
    val auditEventType: String get() = when (trade.eventType) {
        TradeEventType.NEW -> "TRADE_BOOKED"
        TradeEventType.AMEND -> "TRADE_AMENDED"
        TradeEventType.CANCEL -> "TRADE_CANCELLED"
    }
}
