package com.kinetix.risk.service

import com.kinetix.common.model.AssetClass
import com.kinetix.common.model.BookId
import com.kinetix.common.model.InstrumentId
import com.kinetix.common.model.Money
import com.kinetix.common.model.Position
import com.kinetix.risk.model.FactorDecompositionSnapshot
import com.kinetix.risk.model.TimeSeriesMarketData
import com.kinetix.risk.model.TimeSeriesPoint
import com.kinetix.risk.persistence.FactorDecompositionRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeWithinPercentageOf
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.math.BigDecimal
import java.time.Instant
import java.util.Currency

private val USD = Currency.getInstance("USD")

private fun position(
    instrumentId: String = "AAPL",
    assetClass: AssetClass = AssetClass.EQUITY,
    marketPrice: String = "170.00",
    quantity: String = "100",
) = Position(
    bookId = BookId("BOOK-1"),
    instrumentId = InstrumentId(instrumentId),
    assetClass = assetClass,
    quantity = BigDecimal(quantity),
    averageCost = Money(BigDecimal("150.00"), USD),
    marketPrice = Money(BigDecimal(marketPrice), USD),
)

private fun timeSeriesData(
    instrumentId: String,
    prices: List<Double> = List(301) { 100.0 + it * 0.01 },
) = TimeSeriesMarketData(
    dataType = "HISTORICAL_PRICES",
    instrumentId = instrumentId,
    assetClass = "EQUITY",
    points = prices.mapIndexed { i, p ->
        TimeSeriesPoint(
            timestamp = Instant.parse("2025-05-29T00:00:00Z").plusSeconds(i * 86_400L),
            value = p,
        )
    },
)

private fun sampleDecompositionSnapshot(bookId: String = "BOOK-1") = FactorDecompositionSnapshot(
    bookId = bookId,
    calculatedAt = Instant.parse("2026-03-24T10:00:00Z"),
    totalVar = 50_000.0,
    systematicVar = 38_000.0,
    idiosyncraticVar = 12_000.0,
    rSquared = 0.576,
    concentrationWarning = false,
    factors = emptyList(),
)

class FactorRiskServiceTest : FunSpec({

    val riskEngineClient = mockk<com.kinetix.risk.client.RiskEngineClient>()
    val repository = mockk<FactorDecompositionRepository>(relaxed = true)

    val service = FactorRiskService(
        riskEngineClient = riskEngineClient,
        repository = repository,
    )

    beforeEach {
        clearMocks(riskEngineClient, repository)
    }

    val bookId = BookId("BOOK-1")
    val positions = listOf(position("AAPL", AssetClass.EQUITY))

    test("returns null when positions list is empty") {
        val result = service.decompose(bookId, emptyList(), emptyMap(), totalVar = 50_000.0)

        result.shouldBeNull()
        coVerify(exactly = 0) { riskEngineClient.decomposeFactorRisk(any(), any(), any(), any()) }
        coVerify(exactly = 0) { repository.save(any()) }
    }

    test("calls gRPC DecomposeFactorRisk with the book id") {
        val marketData = mapOf(
            "AAPL" to timeSeriesData("AAPL"),
            "IDX-SPX" to timeSeriesData("IDX-SPX"),
        )
        val snapshot = sampleDecompositionSnapshot()
        coEvery { riskEngineClient.decomposeFactorRisk(bookId, positions, marketData, 50_000.0) } returns snapshot

        service.decompose(bookId, positions, marketData, totalVar = 50_000.0)

        coVerify { riskEngineClient.decomposeFactorRisk(bookId, positions, marketData, 50_000.0) }
    }

    test("saves the decomposition snapshot to the repository") {
        val marketData = mapOf("AAPL" to timeSeriesData("AAPL"))
        val snapshot = sampleDecompositionSnapshot()
        coEvery { riskEngineClient.decomposeFactorRisk(any(), any(), any(), any()) } returns snapshot

        service.decompose(bookId, positions, marketData, totalVar = 50_000.0)

        coVerify { repository.save(snapshot) }
    }

    test("returns the snapshot from the gRPC call") {
        val marketData = mapOf("AAPL" to timeSeriesData("AAPL"))
        val expected = sampleDecompositionSnapshot()
        coEvery { riskEngineClient.decomposeFactorRisk(any(), any(), any(), any()) } returns expected

        val result = service.decompose(bookId, positions, marketData, totalVar = 50_000.0)

        result shouldBe expected
    }

    test("returns null and skips save when gRPC call fails") {
        val marketData = mapOf("AAPL" to timeSeriesData("AAPL"))
        coEvery { riskEngineClient.decomposeFactorRisk(any(), any(), any(), any()) } throws
            RuntimeException("gRPC unavailable")

        val result = service.decompose(bookId, positions, marketData, totalVar = 50_000.0)

        result.shouldBeNull()
        coVerify(exactly = 0) { repository.save(any()) }
    }

    test("does not skip when totalVar is zero — zero portfolio is a valid degenerate case") {
        val marketData = mapOf("AAPL" to timeSeriesData("AAPL"))
        val snapshot = sampleDecompositionSnapshot().copy(totalVar = 0.0)
        coEvery { riskEngineClient.decomposeFactorRisk(any(), any(), any(), any()) } returns snapshot

        val result = service.decompose(bookId, positions, marketData, totalVar = 0.0)

        result.shouldNotBeNull()
        coVerify { repository.save(any()) }
    }
})
