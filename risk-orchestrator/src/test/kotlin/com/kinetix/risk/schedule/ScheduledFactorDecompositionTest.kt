package com.kinetix.risk.schedule

import com.kinetix.common.model.BookId
import com.kinetix.risk.model.FactorDecompositionSnapshot
import com.kinetix.risk.service.FactorRiskService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.time.LocalTime

private fun snapshot(bookId: String) = FactorDecompositionSnapshot(
    bookId = bookId,
    calculatedAt = Instant.parse("2026-03-19T04:00:00Z"),
    totalVar = 100_000.0,
    systematicVar = 75_000.0,
    idiosyncraticVar = 25_000.0,
    rSquared = 0.5625,
    concentrationWarning = false,
    factors = emptyList(),
)

class ScheduledFactorDecompositionTest : FunSpec({

    val factorRiskService = mockk<FactorRiskService>()

    beforeEach {
        clearMocks(factorRiskService)
    }

    test("decomposes each book after the scheduled time") {
        coEvery { factorRiskService.decomposeForBook(BookId("BOOK-1"), any()) } returns snapshot("BOOK-1")
        coEvery { factorRiskService.decomposeForBook(BookId("BOOK-2"), any()) } returns snapshot("BOOK-2")

        val job = ScheduledFactorDecomposition(
            factorRiskService = factorRiskService,
            bookIds = { listOf(BookId("BOOK-1"), BookId("BOOK-2")) },
            runAtTime = LocalTime.of(4, 0),
            nowProvider = { LocalTime.of(4, 30) },
        )

        job.tick()

        coVerify(exactly = 1) { factorRiskService.decomposeForBook(BookId("BOOK-1"), 0.0) }
        coVerify(exactly = 1) { factorRiskService.decomposeForBook(BookId("BOOK-2"), 0.0) }
    }

    test("skips all books before the scheduled time") {
        val job = ScheduledFactorDecomposition(
            factorRiskService = factorRiskService,
            bookIds = { listOf(BookId("BOOK-1")) },
            runAtTime = LocalTime.of(4, 0),
            nowProvider = { LocalTime.of(3, 59) },
        )

        job.tick()

        coVerify(exactly = 0) { factorRiskService.decomposeForBook(any(), any()) }
    }

    test("continues decomposing remaining books when one fails") {
        coEvery { factorRiskService.decomposeForBook(BookId("BOOK-1"), any()) } throws RuntimeException("gRPC error")
        coEvery { factorRiskService.decomposeForBook(BookId("BOOK-2"), any()) } returns snapshot("BOOK-2")

        val job = ScheduledFactorDecomposition(
            factorRiskService = factorRiskService,
            bookIds = { listOf(BookId("BOOK-1"), BookId("BOOK-2")) },
            runAtTime = LocalTime.of(4, 0),
            nowProvider = { LocalTime.of(4, 30) },
        )

        job.tick()

        coVerify(exactly = 1) { factorRiskService.decomposeForBook(BookId("BOOK-2"), 0.0) }
    }

    test("runs at exactly the scheduled time") {
        coEvery { factorRiskService.decomposeForBook(BookId("BOOK-X"), any()) } returns snapshot("BOOK-X")

        val job = ScheduledFactorDecomposition(
            factorRiskService = factorRiskService,
            bookIds = { listOf(BookId("BOOK-X")) },
            runAtTime = LocalTime.of(4, 0),
            nowProvider = { LocalTime.of(4, 0) },
        )

        job.tick()

        coVerify(exactly = 1) { factorRiskService.decomposeForBook(BookId("BOOK-X"), 0.0) }
    }

    test("returns count of successfully decomposed books") {
        coEvery { factorRiskService.decomposeForBook(BookId("BOOK-1"), any()) } returns snapshot("BOOK-1")
        coEvery { factorRiskService.decomposeForBook(BookId("BOOK-2"), any()) } throws RuntimeException("fail")
        coEvery { factorRiskService.decomposeForBook(BookId("BOOK-3"), any()) } returns snapshot("BOOK-3")

        val job = ScheduledFactorDecomposition(
            factorRiskService = factorRiskService,
            bookIds = { listOf(BookId("BOOK-1"), BookId("BOOK-2"), BookId("BOOK-3")) },
            runAtTime = LocalTime.of(4, 0),
            nowProvider = { LocalTime.of(4, 30) },
        )

        val count = job.tick()

        count shouldBe 2
    }
})
