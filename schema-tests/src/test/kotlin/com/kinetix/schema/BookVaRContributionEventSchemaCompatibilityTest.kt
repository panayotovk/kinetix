package com.kinetix.schema

import com.kinetix.common.kafka.events.BookVaRContributionEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class BookVaRContributionEventSchemaCompatibilityTest : FunSpec({

    val json = Json { ignoreUnknownKeys = true }

    test("BookVaRContributionEvent round-trips all fields") {
        val event = BookVaRContributionEvent(
            bookId = "book-001",
            varContribution = "125000.50",
            percentageOfTotal = "35.25",
            standaloneVar = "150000.00",
            diversificationBenefit = "24999.50",
            marginalVar = "42000.00",
            incrementalVar = "38500.00",
        )
        val serialized = Json.encodeToString(BookVaRContributionEvent.serializer(), event)

        val deserialized = json.decodeFromString<BookVaRContributionEvent>(serialized)
        deserialized.bookId shouldBe "book-001"
        deserialized.varContribution shouldBe "125000.50"
        deserialized.percentageOfTotal shouldBe "35.25"
        deserialized.standaloneVar shouldBe "150000.00"
        deserialized.diversificationBenefit shouldBe "24999.50"
        deserialized.marginalVar shouldBe "42000.00"
        deserialized.incrementalVar shouldBe "38500.00"
    }

    test("backward compatibility: marginalVar and incrementalVar default to 0.0 when absent") {
        val legacyJson = """
            {
                "bookId": "book-002",
                "varContribution": "50000.00",
                "percentageOfTotal": "20.00",
                "standaloneVar": "60000.00",
                "diversificationBenefit": "10000.00"
            }
        """.trimIndent()

        val event = json.decodeFromString<BookVaRContributionEvent>(legacyJson)
        event.bookId shouldBe "book-002"
        event.varContribution shouldBe "50000.00"
        event.marginalVar shouldBe "0.0"
        event.incrementalVar shouldBe "0.0"
    }

    test("forward compatibility: unknown fields from a newer producer are ignored") {
        val newerJson = """
            {
                "bookId": "book-003",
                "varContribution": "75000.00",
                "percentageOfTotal": "25.00",
                "standaloneVar": "85000.00",
                "diversificationBenefit": "10000.00",
                "marginalVar": "30000.00",
                "incrementalVar": "28000.00",
                "esContribution": "95000.00",
                "componentShapley": "0.15"
            }
        """.trimIndent()

        val event = json.decodeFromString<BookVaRContributionEvent>(newerJson)
        event.bookId shouldBe "book-003"
        event.varContribution shouldBe "75000.00"
        event.marginalVar shouldBe "30000.00"
    }
})
