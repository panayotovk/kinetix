package com.kinetix.common.kafka.events

import kotlinx.serialization.Serializable

/**
 * Emitted by position-service whenever a trade is blocked by a HARD limit
 * breach during pre-trade checks. Consumed by notification-service to drive
 * an in-app alert and any downstream escalation. Synchronous booking
 * semantics are unchanged — the booking call still fails with HTTP 422 —
 * but the breach is now durable and observable.
 *
 * One event per breach: when a single trade trips multiple HARD limits the
 * publisher emits one event per breach (carrying the limitType / severity
 * pair) so consumers can route per-rule.
 */
@Serializable
data class LimitBreachEvent(
    val eventId: String,
    val bookId: String,
    val limitType: String,
    val severity: String,
    val currentValue: String,
    val limitValue: String,
    val message: String,
    val breachedAt: String,
    val tradeId: String? = null,
    val correlationId: String? = null,
)
