package com.kinetix.schema

import com.kinetix.common.kafka.events.LimitBreachEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class LimitBreachEventSchemaCompatibilityTest : FunSpec({

    val json = Json { ignoreUnknownKeys = true }

    test("LimitBreachEvent round-trips all fields for a HARD breach") {
        val event = LimitBreachEvent(
            eventId = "evt-1",
            tradeId = "t-1",
            bookId = "BOOK-001",
            limitType = "VAR",
            severity = "HARD",
            currentValue = "1500000",
            limitValue = "1000000",
            message = "VaR exceeds firm limit",
            breachedAt = "2026-04-29T10:00:00Z",
            correlationId = "corr-1",
        )
        val serialized = Json.encodeToString(LimitBreachEvent.serializer(), event)

        val deserialized = json.decodeFromString<LimitBreachEvent>(serialized)
        deserialized.eventId shouldBe "evt-1"
        deserialized.tradeId shouldBe "t-1"
        deserialized.bookId shouldBe "BOOK-001"
        deserialized.limitType shouldBe "VAR"
        deserialized.severity shouldBe "HARD"
        deserialized.currentValue shouldBe "1500000"
        deserialized.limitValue shouldBe "1000000"
        deserialized.message shouldBe "VaR exceeds firm limit"
        deserialized.breachedAt shouldBe "2026-04-29T10:00:00Z"
        deserialized.correlationId shouldBe "corr-1"
    }

    test("LimitBreachEvent round-trips when tradeId and correlationId are null") {
        val event = LimitBreachEvent(
            eventId = "evt-2",
            tradeId = null,
            bookId = "BOOK-002",
            limitType = "NOTIONAL",
            severity = "HARD",
            currentValue = "10000000",
            limitValue = "5000000",
            message = "Notional limit exceeded",
            breachedAt = "2026-04-29T10:30:00Z",
        )
        val serialized = Json.encodeToString(LimitBreachEvent.serializer(), event)

        val deserialized = json.decodeFromString<LimitBreachEvent>(serialized)
        deserialized.tradeId shouldBe null
        deserialized.correlationId shouldBe null
    }

    test("backward compatibility: minimal JSON with only required fields deserializes with null optionals") {
        val minimalJson = """
            {
                "eventId": "evt-3",
                "bookId": "BOOK-003",
                "limitType": "POSITION",
                "severity": "HARD",
                "currentValue": "12000",
                "limitValue": "10000",
                "message": "Position limit breached",
                "breachedAt": "2026-04-29T11:00:00Z"
            }
        """.trimIndent()

        val event = json.decodeFromString<LimitBreachEvent>(minimalJson)
        event.eventId shouldBe "evt-3"
        event.bookId shouldBe "BOOK-003"
        event.tradeId shouldBe null
        event.correlationId shouldBe null
    }

    test("forward compatibility: unknown fields added by a newer producer are ignored") {
        val newerJson = """
            {
                "eventId": "evt-4",
                "tradeId": "t-4",
                "bookId": "BOOK-004",
                "limitType": "CONCENTRATION",
                "severity": "HARD",
                "currentValue": "0.55",
                "limitValue": "0.30",
                "message": "Concentration breach",
                "breachedAt": "2026-04-29T12:00:00Z",
                "limitId": "l-1",
                "limitLevel": "FIRM",
                "entityId": "firm-1"
            }
        """.trimIndent()

        val event = json.decodeFromString<LimitBreachEvent>(newerJson)
        event.eventId shouldBe "evt-4"
        event.bookId shouldBe "BOOK-004"
        event.limitType shouldBe "CONCENTRATION"
    }
})
