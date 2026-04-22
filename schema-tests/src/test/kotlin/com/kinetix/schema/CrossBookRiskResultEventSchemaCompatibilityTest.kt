package com.kinetix.schema

import com.kinetix.common.kafka.events.BookVaRContributionEvent
import com.kinetix.common.kafka.events.ComponentBreakdownEvent
import com.kinetix.common.kafka.events.CrossBookRiskResultEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class CrossBookRiskResultEventSchemaCompatibilityTest : FunSpec({

    val json = Json { ignoreUnknownKeys = true }

    test("CrossBookRiskResultEvent round-trips all fields including nested contributions") {
        val event = CrossBookRiskResultEvent(
            portfolioGroupId = "group-global-equity",
            bookIds = listOf("book-001", "book-002", "book-003"),
            varValue = "500000.00",
            expectedShortfall = "650000.00",
            calculationType = "HISTORICAL",
            confidenceLevel = "0.99",
            componentBreakdown = listOf(
                ComponentBreakdownEvent(
                    assetClass = "EQUITY",
                    varContribution = "300000.00",
                    percentageOfTotal = "60.00",
                ),
                ComponentBreakdownEvent(
                    assetClass = "FX",
                    varContribution = "200000.00",
                    percentageOfTotal = "40.00",
                ),
            ),
            bookContributions = listOf(
                BookVaRContributionEvent(
                    bookId = "book-001",
                    varContribution = "250000.00",
                    percentageOfTotal = "50.00",
                    standaloneVar = "300000.00",
                    diversificationBenefit = "50000.00",
                ),
                BookVaRContributionEvent(
                    bookId = "book-002",
                    varContribution = "150000.00",
                    percentageOfTotal = "30.00",
                    standaloneVar = "180000.00",
                    diversificationBenefit = "30000.00",
                ),
            ),
            totalStandaloneVar = "600000.00",
            diversificationBenefit = "100000.00",
            calculatedAt = "2025-01-15T14:00:00Z",
            correlationId = "corr-crossbook-1",
        )
        val serialized = Json.encodeToString(CrossBookRiskResultEvent.serializer(), event)

        val deserialized = json.decodeFromString<CrossBookRiskResultEvent>(serialized)
        deserialized.portfolioGroupId shouldBe "group-global-equity"
        deserialized.bookIds shouldBe listOf("book-001", "book-002", "book-003")
        deserialized.varValue shouldBe "500000.00"
        deserialized.expectedShortfall shouldBe "650000.00"
        deserialized.calculationType shouldBe "HISTORICAL"
        deserialized.componentBreakdown.size shouldBe 2
        deserialized.componentBreakdown[0].assetClass shouldBe "EQUITY"
        deserialized.bookContributions.size shouldBe 2
        deserialized.bookContributions[0].bookId shouldBe "book-001"
        deserialized.totalStandaloneVar shouldBe "600000.00"
        deserialized.diversificationBenefit shouldBe "100000.00"
        deserialized.correlationId shouldBe "corr-crossbook-1"
    }

    test("backward compatibility: missing componentBreakdown, bookContributions, and correlationId default to empty/null") {
        val minimalJson = """
            {
                "portfolioGroupId": "group-fx",
                "bookIds": ["book-fx-1"],
                "varValue": "75000.00",
                "expectedShortfall": "90000.00",
                "calculationType": "MONTE_CARLO",
                "confidenceLevel": "0.95",
                "totalStandaloneVar": "75000.00",
                "diversificationBenefit": "0.00",
                "calculatedAt": "2025-01-15T15:00:00Z"
            }
        """.trimIndent()

        val event = json.decodeFromString<CrossBookRiskResultEvent>(minimalJson)
        event.portfolioGroupId shouldBe "group-fx"
        event.bookIds shouldBe listOf("book-fx-1")
        event.componentBreakdown shouldBe emptyList()
        event.bookContributions shouldBe emptyList()
        event.correlationId shouldBe null
    }

    test("forward compatibility: unknown top-level and nested fields are ignored") {
        val newerJson = """
            {
                "portfolioGroupId": "group-mixed",
                "bookIds": ["book-1"],
                "varValue": "100000.00",
                "expectedShortfall": "120000.00",
                "calculationType": "PARAMETRIC",
                "confidenceLevel": "0.99",
                "componentBreakdown": [
                    {
                        "assetClass": "RATES",
                        "varContribution": "100000.00",
                        "percentageOfTotal": "100.00",
                        "futureField": "ignored"
                    }
                ],
                "bookContributions": [],
                "totalStandaloneVar": "100000.00",
                "diversificationBenefit": "0.00",
                "calculatedAt": "2025-01-15T16:00:00Z",
                "regimeTag": "HIGH_VOL",
                "scenarioId": "sc-42"
            }
        """.trimIndent()

        val event = json.decodeFromString<CrossBookRiskResultEvent>(newerJson)
        event.portfolioGroupId shouldBe "group-mixed"
        event.componentBreakdown.size shouldBe 1
        event.componentBreakdown[0].assetClass shouldBe "RATES"
    }
})
