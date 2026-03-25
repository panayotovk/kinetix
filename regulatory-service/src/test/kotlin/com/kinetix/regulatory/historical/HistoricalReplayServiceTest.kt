package com.kinetix.regulatory.historical

import com.kinetix.regulatory.client.RiskOrchestratorClient
import com.kinetix.regulatory.historical.dto.PositionReplayImpact
import com.kinetix.regulatory.historical.dto.ReplayRequest
import com.kinetix.regulatory.historical.dto.ReplayResultResponse
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.math.BigDecimal

class HistoricalReplayServiceTest : FunSpec({

    val repository = mockk<HistoricalScenarioRepository>()
    val client = mockk<RiskOrchestratorClient>()
    val service = HistoricalReplayService(repository, client)

    beforeTest {
        clearMocks(repository, client)
    }

    val aPeriod = HistoricalScenarioPeriod(
        periodId = "GFC_OCT_2008",
        name = "GFC_OCT_2008",
        description = "Global Financial Crisis",
        startDate = "2008-09-15",
        endDate = "2009-03-15",
        assetClassFocus = "EQUITY",
        severityLabel = "SEVERE",
    )

    test("runs replay by loading period returns and delegating to risk-orchestrator") {
        val returns = listOf(
            HistoricalScenarioReturn("GFC_OCT_2008", "AAPL", "2008-09-15", BigDecimal("-0.035")),
            HistoricalScenarioReturn("GFC_OCT_2008", "AAPL", "2008-09-16", BigDecimal("-0.055")),
            HistoricalScenarioReturn("GFC_OCT_2008", "MSFT", "2008-09-15", BigDecimal("-0.030")),
            HistoricalScenarioReturn("GFC_OCT_2008", "MSFT", "2008-09-16", BigDecimal("-0.040")),
        )
        val expectedResponse = ReplayResultResponse(
            periodId = "GFC_OCT_2008",
            scenarioName = "GFC_OCT_2008",
            bookId = "BOOK-1",
            totalPnlImpact = "-12500.00",
            positionImpacts = listOf(
                PositionReplayImpact("AAPL", "EQUITY", "50000.00", "-4500.00", listOf("-1750.00", "-2750.00"), false),
                PositionReplayImpact("MSFT", "EQUITY", "50000.00", "-3500.00", listOf("-1500.00", "-2000.00"), false),
            ),
            windowStart = "2008-09-15",
            windowEnd = "2009-03-15",
            calculatedAt = "2026-03-25T12:00:00Z",
        )

        coEvery { repository.findPeriodById("GFC_OCT_2008") } returns aPeriod
        coEvery { repository.findAllReturns("GFC_OCT_2008") } returns returns
        coEvery { client.runHistoricalReplay(any(), any(), any(), any()) } returns expectedResponse

        val result = service.runReplay("GFC_OCT_2008", ReplayRequest(bookId = "BOOK-1"))

        result.periodId shouldBe "GFC_OCT_2008"
        result.bookId shouldBe "BOOK-1"
        result.totalPnlImpact shouldBe "-12500.00"
        result.positionImpacts shouldHaveSize 2

        coVerify(exactly = 1) { repository.findAllReturns("GFC_OCT_2008") }
        coVerify(exactly = 1) {
            client.runHistoricalReplay("BOOK-1", any(), "2008-09-15", "2009-03-15")
        }
    }

    test("groups returns by instrument id before passing to risk-orchestrator") {
        val returns = listOf(
            HistoricalScenarioReturn("GFC_OCT_2008", "AAPL", "2008-09-15", BigDecimal("-0.035")),
            HistoricalScenarioReturn("GFC_OCT_2008", "AAPL", "2008-09-16", BigDecimal("-0.055")),
            HistoricalScenarioReturn("GFC_OCT_2008", "MSFT", "2008-09-15", BigDecimal("-0.030")),
        )

        var capturedReturns: Map<String, List<Double>>? = null
        coEvery { repository.findPeriodById("GFC_OCT_2008") } returns aPeriod
        coEvery { repository.findAllReturns("GFC_OCT_2008") } returns returns
        coEvery {
            client.runHistoricalReplay(any(), any(), any(), any())
        } answers {
            @Suppress("UNCHECKED_CAST")
            capturedReturns = secondArg<Map<String, List<Double>>>()
            aReplayResultResponse("GFC_OCT_2008", "BOOK-1")
        }

        service.runReplay("GFC_OCT_2008", ReplayRequest(bookId = "BOOK-1"))

        capturedReturns!!["AAPL"] shouldBe listOf(-0.035, -0.055)
        capturedReturns!!["MSFT"] shouldBe listOf(-0.030)
    }

    test("propagates period window dates from the stored period") {
        coEvery { repository.findPeriodById("GFC_OCT_2008") } returns aPeriod
        coEvery { repository.findAllReturns("GFC_OCT_2008") } returns emptyList()
        coEvery { client.runHistoricalReplay(any(), any(), any(), any()) } returns
            aReplayResultResponse("GFC_OCT_2008", "BOOK-1")

        service.runReplay("GFC_OCT_2008", ReplayRequest(bookId = "BOOK-1"))

        coVerify(exactly = 1) {
            client.runHistoricalReplay("BOOK-1", any(), "2008-09-15", "2009-03-15")
        }
    }

    test("passes an empty instrument returns map when no returns are stored for the period") {
        coEvery { repository.findPeriodById("GFC_OCT_2008") } returns aPeriod
        coEvery { repository.findAllReturns("GFC_OCT_2008") } returns emptyList()

        var capturedReturns: Map<String, List<Double>>? = null
        coEvery {
            client.runHistoricalReplay(any(), any(), any(), any())
        } answers {
            @Suppress("UNCHECKED_CAST")
            capturedReturns = secondArg<Map<String, List<Double>>>()
            aReplayResultResponse("GFC_OCT_2008", "BOOK-1")
        }

        service.runReplay("GFC_OCT_2008", ReplayRequest(bookId = "BOOK-1"))

        capturedReturns shouldBe emptyMap()
    }

    test("throws NoSuchElementException when period does not exist") {
        coEvery { repository.findPeriodById("UNKNOWN_PERIOD") } returns null

        shouldThrow<NoSuchElementException> {
            service.runReplay("UNKNOWN_PERIOD", ReplayRequest(bookId = "BOOK-1"))
        }
    }
})

private fun aReplayResultResponse(periodId: String, bookId: String) = ReplayResultResponse(
    periodId = periodId,
    scenarioName = periodId,
    bookId = bookId,
    totalPnlImpact = "0.00",
    positionImpacts = emptyList(),
    windowStart = null,
    windowEnd = null,
    calculatedAt = "2026-03-25T12:00:00Z",
)
