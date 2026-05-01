package com.kinetix.schema

import com.kinetix.common.kafka.events.LiquidityRiskEvent
import com.kinetix.common.kafka.events.PositionLiquidityRiskItem
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class LiquidityRiskEventSchemaCompatibilityTest : FunSpec({

    val json = Json { ignoreUnknownKeys = true }

    test("LiquidityRiskEvent round-trips all fields including nested position items") {
        val event = LiquidityRiskEvent(
            bookId = "book-001",
            portfolioLvar = "250000.00",
            dataCompleteness = 0.97,
            portfolioConcentrationStatus = "WARN",
            calculatedAt = "2025-01-15T14:00:00Z",
            positionRisks = listOf(
                PositionLiquidityRiskItem(
                    instrumentId = "AAPL",
                    assetClass = "EQUITY",
                    tier = "HIGH_LIQUID",
                    horizonDays = 1,
                    advMissing = false,
                    advStale = false,
                    concentrationStatus = "OK",
                    lvarContribution = "125000.00",
                ),
                PositionLiquidityRiskItem(
                    instrumentId = "ILLIQUID-CORP",
                    assetClass = "CREDIT",
                    tier = "SEMI_LIQUID",
                    horizonDays = 10,
                    advMissing = true,
                    advStale = false,
                    concentrationStatus = "BREACH",
                    lvarContribution = "125000.00",
                ),
            ),
            correlationId = "corr-liquidity-1",
        )
        val serialized = Json.encodeToString(LiquidityRiskEvent.serializer(), event)

        val deserialized = json.decodeFromString<LiquidityRiskEvent>(serialized)
        deserialized.bookId shouldBe "book-001"
        deserialized.portfolioLvar shouldBe "250000.00"
        deserialized.dataCompleteness shouldBe 0.97
        deserialized.portfolioConcentrationStatus shouldBe "WARN"
        deserialized.calculatedAt shouldBe "2025-01-15T14:00:00Z"
        deserialized.positionRisks.size shouldBe 2
        deserialized.positionRisks[0].instrumentId shouldBe "AAPL"
        deserialized.positionRisks[0].tier shouldBe "HIGH_LIQUID"
        deserialized.positionRisks[1].advMissing shouldBe true
        deserialized.positionRisks[1].concentrationStatus shouldBe "BREACH"
        deserialized.correlationId shouldBe "corr-liquidity-1"
    }

    test("backward compatibility: missing positionRisks and correlationId default to empty list and null") {
        val minimalJson = """
            {
                "bookId": "book-002",
                "portfolioLvar": "50000.00",
                "dataCompleteness": 1.0,
                "portfolioConcentrationStatus": "OK",
                "calculatedAt": "2025-01-15T15:00:00Z"
            }
        """.trimIndent()

        val event = json.decodeFromString<LiquidityRiskEvent>(minimalJson)
        event.bookId shouldBe "book-002"
        event.portfolioLvar shouldBe "50000.00"
        event.dataCompleteness shouldBe 1.0
        event.positionRisks shouldBe emptyList()
        event.correlationId shouldBe null
    }

    test("forward compatibility: unknown top-level and nested fields are ignored") {
        val newerJson = """
            {
                "bookId": "book-003",
                "portfolioLvar": "75000.00",
                "dataCompleteness": 0.85,
                "portfolioConcentrationStatus": "WARN",
                "calculatedAt": "2025-01-15T16:00:00Z",
                "positionRisks": [
                    {
                        "instrumentId": "GOOG",
                        "assetClass": "EQUITY",
                        "tier": "HIGH_LIQUID",
                        "horizonDays": 1,
                        "advMissing": false,
                        "advStale": false,
                        "concentrationStatus": "OK",
                        "lvarContribution": "75000.00",
                        "daysToLiquidate": 2.5,
                        "bidAskSpread": "0.0002"
                    }
                ],
                "regimeIndicator": "STRESSED",
                "stressMultiplier": 1.5
            }
        """.trimIndent()

        val event = json.decodeFromString<LiquidityRiskEvent>(newerJson)
        event.bookId shouldBe "book-003"
        event.positionRisks.size shouldBe 1
        event.positionRisks[0].instrumentId shouldBe "GOOG"
        event.positionRisks[0].horizonDays shouldBe 1
    }
})
