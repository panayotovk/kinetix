package com.kinetix.risk.schedule

import com.kinetix.common.model.BookId
import com.kinetix.risk.cache.InMemoryVaRCache
import com.kinetix.risk.model.VaRCalculationRequest
import com.kinetix.risk.model.ValuationResult
import com.kinetix.risk.service.FactorRiskService
import com.kinetix.risk.service.VaRCalculationService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ScheduledVaRCalculatorTest : FunSpec({

    test("triggers VaR calculation at regular intervals") {
        val varService = mockk<VaRCalculationService>()
        val varCache = InMemoryVaRCache()

        var callCount = 0
        coEvery { varService.calculateVaR(any(), any()) } answers {
            callCount++
            null
        }

        val calculator = ScheduledVaRCalculator(
            varCalculationService = varService,
            varCache = varCache,
            bookIds = { listOf(BookId("port-1")) },
            intervalMillis = 200,
        )

        val job = launch { calculator.start() }

        delay(650)
        job.cancel()

        callCount shouldBeGreaterThanOrEqual 2
    }

    test("calculates VaR for all configured portfolios") {
        val varService = mockk<VaRCalculationService>()
        val varCache = InMemoryVaRCache()

        val portfoliosCalculated = mutableSetOf<String>()
        coEvery { varService.calculateVaR(any(), any()) } answers {
            portfoliosCalculated.add(firstArg<VaRCalculationRequest>().bookId.value)
            null
        }

        val calculator = ScheduledVaRCalculator(
            varCalculationService = varService,
            varCache = varCache,
            bookIds = { listOf(BookId("port-1"), BookId("port-2"), BookId("port-3")) },
            intervalMillis = 200,
        )

        val job = launch { calculator.start() }

        delay(350)
        job.cancel()

        portfoliosCalculated shouldBe setOf("port-1", "port-2", "port-3")
    }

    test("continues running even if calculation fails") {
        val varService = mockk<VaRCalculationService>()
        val varCache = InMemoryVaRCache()

        var callCount = 0
        coEvery { varService.calculateVaR(any(), any()) } answers {
            callCount++
            if (callCount == 1) throw RuntimeException("Simulated failure")
            null
        }

        val calculator = ScheduledVaRCalculator(
            varCalculationService = varService,
            varCache = varCache,
            bookIds = { listOf(BookId("port-1")) },
            intervalMillis = 200,
        )

        val job = launch { calculator.start() }

        delay(650)
        job.cancel()

        callCount shouldBeGreaterThanOrEqual 2
    }

    test("caches VaR results when calculation succeeds") {
        val varService = mockk<VaRCalculationService>()
        val varCache = InMemoryVaRCache()
        val mockResult = mockk<ValuationResult>()

        coEvery { varService.calculateVaR(any(), any()) } returns mockResult

        val calculator = ScheduledVaRCalculator(
            varCalculationService = varService,
            varCache = varCache,
            bookIds = { listOf(BookId("port-1")) },
            intervalMillis = 200,
        )

        val job = launch { calculator.start() }

        delay(100)
        job.cancel()

        varCache.get("port-1").shouldNotBeNull()
        varCache.get("port-1") shouldBe mockResult
    }

    test("triggers factor decomposition with the book id and total VaR after a successful calculation") {
        val varService = mockk<VaRCalculationService>()
        val varCache = InMemoryVaRCache()
        val factorRiskService = mockk<FactorRiskService>()
        val bookId = BookId("port-1")

        val result = mockk<ValuationResult> {
            every { varValue } returns 42_000.0
        }
        coEvery { varService.calculateVaR(any(), any()) } returns result
        coEvery { factorRiskService.decomposeForBook(any(), any()) } returns null

        val calculator = ScheduledVaRCalculator(
            varCalculationService = varService,
            varCache = varCache,
            bookIds = { listOf(bookId) },
            intervalMillis = 200,
            factorRiskService = factorRiskService,
        )

        val job = launch { calculator.start() }

        delay(100)
        job.cancel()

        coVerify { factorRiskService.decomposeForBook(bookId, 42_000.0) }
    }

    test("skips factor decomposition when VaR calculation returns null") {
        val varService = mockk<VaRCalculationService>()
        val varCache = InMemoryVaRCache()
        val factorRiskService = mockk<FactorRiskService>()

        coEvery { varService.calculateVaR(any(), any()) } returns null

        val calculator = ScheduledVaRCalculator(
            varCalculationService = varService,
            varCache = varCache,
            bookIds = { listOf(BookId("port-1")) },
            intervalMillis = 200,
            factorRiskService = factorRiskService,
        )

        val job = launch { calculator.start() }

        delay(100)
        job.cancel()

        coVerify(exactly = 0) { factorRiskService.decomposeForBook(any(), any()) }
    }
})
