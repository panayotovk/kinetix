package com.kinetix.schema

import com.kinetix.common.kafka.events.AlertLifecycleEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class AlertLifecycleEventSchemaCompatibilityTest : FunSpec({

    val json = Json { ignoreUnknownKeys = true }

    test("AlertLifecycleEvent round-trips all fields for an ACKNOWLEDGED event") {
        val event = AlertLifecycleEvent(
            alertId = "alert-1",
            eventType = "ACKNOWLEDGED",
            bookId = "book-001",
            userId = "user-42",
            userRole = "TRADER",
            note = "Acknowledged during morning session",
            occurredAt = "2025-01-15T10:00:00Z",
            correlationId = "corr-1",
        )
        val serialized = Json.encodeToString(AlertLifecycleEvent.serializer(), event)

        val deserialized = json.decodeFromString<AlertLifecycleEvent>(serialized)
        deserialized.alertId shouldBe "alert-1"
        deserialized.eventType shouldBe "ACKNOWLEDGED"
        deserialized.bookId shouldBe "book-001"
        deserialized.userId shouldBe "user-42"
        deserialized.userRole shouldBe "TRADER"
        deserialized.note shouldBe "Acknowledged during morning session"
        deserialized.occurredAt shouldBe "2025-01-15T10:00:00Z"
        deserialized.correlationId shouldBe "corr-1"
    }

    test("AlertLifecycleEvent round-trips a RESOLVED event without a note") {
        val event = AlertLifecycleEvent(
            alertId = "alert-2",
            eventType = "RESOLVED",
            bookId = "book-002",
            userId = "user-99",
            userRole = "RISK_OFFICER",
            occurredAt = "2025-01-15T11:30:00Z",
        )
        val serialized = Json.encodeToString(AlertLifecycleEvent.serializer(), event)

        val deserialized = json.decodeFromString<AlertLifecycleEvent>(serialized)
        deserialized.alertId shouldBe "alert-2"
        deserialized.eventType shouldBe "RESOLVED"
        deserialized.note shouldBe null
        deserialized.correlationId shouldBe null
    }

    test("backward compatibility: minimal JSON with only required fields deserializes with null optionals") {
        val minimalJson = """
            {
                "alertId": "alert-3",
                "eventType": "TRIGGERED",
                "bookId": "book-003",
                "occurredAt": "2025-01-15T12:00:00Z"
            }
        """.trimIndent()

        val event = json.decodeFromString<AlertLifecycleEvent>(minimalJson)
        event.alertId shouldBe "alert-3"
        event.eventType shouldBe "TRIGGERED"
        event.bookId shouldBe "book-003"
        event.userId shouldBe null
        event.userRole shouldBe null
        event.note shouldBe null
        event.correlationId shouldBe null
    }

    test("forward compatibility: unknown fields added by a newer producer are ignored") {
        val newerJson = """
            {
                "alertId": "alert-4",
                "eventType": "ESCALATED",
                "bookId": "book-004",
                "occurredAt": "2025-01-15T13:00:00Z",
                "severity": "CRITICAL",
                "escalationChannel": "PAGERDUTY"
            }
        """.trimIndent()

        val event = json.decodeFromString<AlertLifecycleEvent>(newerJson)
        event.alertId shouldBe "alert-4"
        event.eventType shouldBe "ESCALATED"
        event.bookId shouldBe "book-004"
    }
})
