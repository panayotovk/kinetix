package com.kinetix.notification.engine

import com.kinetix.common.kafka.events.LimitBreachEvent
import com.kinetix.notification.model.AlertEvent
import com.kinetix.notification.model.AlertType
import com.kinetix.notification.model.Severity
import java.time.Instant
import java.util.UUID

/**
 * Translates a [LimitBreachEvent] into an [AlertEvent].
 *
 * The position-service only emits HARD breaches (which actually block the
 * trade) — those map to CRITICAL alerts. The rule still tolerates SOFT
 * payloads from a future producer and downgrades them to WARNING so we
 * don't drop signal silently.
 */
class LimitBreachRule {

    fun evaluate(event: LimitBreachEvent): AlertEvent {
        val severity = when (event.severity) {
            "HARD" -> Severity.CRITICAL
            "SOFT" -> Severity.WARNING
            else -> Severity.WARNING
        }

        val currentValue = event.currentValue.toDoubleOrNull() ?: 0.0
        val threshold = event.limitValue.toDoubleOrNull() ?: 0.0

        return AlertEvent(
            id = UUID.randomUUID().toString(),
            ruleId = "LIMIT_BREACH",
            ruleName = "Limit Breach",
            type = AlertType.LIMIT_BREACH,
            severity = severity,
            message = event.message,
            currentValue = currentValue,
            threshold = threshold,
            bookId = event.bookId,
            triggeredAt = Instant.now(),
            correlationId = event.correlationId,
        )
    }
}
