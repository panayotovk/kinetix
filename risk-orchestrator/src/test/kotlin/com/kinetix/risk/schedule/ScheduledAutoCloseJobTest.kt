package com.kinetix.risk.schedule

import com.kinetix.common.model.BookId
import com.kinetix.risk.model.*
import com.kinetix.risk.service.EodPromotionService
import com.kinetix.risk.service.VaRCalculationService
import com.kinetix.risk.service.ValuationJobRecorder
import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

private val BOOK = BookId("equity-growth")
private val BOOK_2 = BookId("tech-momentum")
private val JOB_ID = UUID.fromString("11111111-1111-1111-1111-111111111111")
private val WEEKDAY = LocalDate.of(2026, 3, 30) // Monday
private val SATURDAY = LocalDate.of(2026, 3, 28)
private val SUNDAY = LocalDate.of(2026, 3, 29)

private fun varResult(jobId: UUID = JOB_ID) = ValuationResult(
    bookId = BOOK,
    calculationType = CalculationType.PARAMETRIC,
    confidenceLevel = ConfidenceLevel.CL_99,
    varValue = 245_000.0,
    expectedShortfall = 298_000.0,
    componentBreakdown = emptyList(),
    greeks = null,
    calculatedAt = Instant.now(),
    computedOutputs = setOf(ValuationOutput.VAR, ValuationOutput.EXPECTED_SHORTFALL),
    jobId = jobId,
)

private fun officialEodJob() = ValuationJob(
    jobId = UUID.randomUUID(),
    bookId = BOOK.value,
    triggerType = TriggerType.AUTO_CLOSE,
    status = RunStatus.COMPLETED,
    startedAt = Instant.parse("2026-03-30T17:30:00Z"),
    valuationDate = WEEKDAY,
    triggeredBy = "AUTO_CLOSE",
    runLabel = RunLabel.OFFICIAL_EOD,
    promotedAt = Instant.parse("2026-03-30T17:30:30Z"),
    promotedBy = "AUTO_CLOSE",
)

class ScheduledAutoCloseJobTest : FunSpec({

    val varCalcService = mockk<VaRCalculationService>()
    val eodPromotionService = mockk<EodPromotionService>()
    val jobRecorder = mockk<ValuationJobRecorder>()

    beforeEach {
        clearMocks(varCalcService, eodPromotionService, jobRecorder)
    }

    fun job(
        closeTime: LocalTime = LocalTime.of(17, 30),
        nowProvider: () -> LocalTime = { LocalTime.of(17, 30) },
        dateProvider: () -> LocalDate = { WEEKDAY },
        bookIds: suspend () -> List<BookId> = { listOf(BOOK) },
    ) = ScheduledAutoCloseJob(
        varCalculationService = varCalcService,
        eodPromotionService = eodPromotionService,
        jobRecorder = jobRecorder,
        bookIds = bookIds,
        closeTime = closeTime,
        nowProvider = nowProvider,
        dateProvider = dateProvider,
    )

    test("skips all books before close time") {
        val j = job(nowProvider = { LocalTime.of(17, 29) })

        j.tick()

        coVerify(exactly = 0) { jobRecorder.findOfficialEodByDate(any(), any()) }
        coVerify(exactly = 0) { varCalcService.calculateVaR(any(), any(), any(), any(), any()) }
    }

    test("fires VaR calc and promotes when no OFFICIAL_EOD exists for today") {
        coEvery { jobRecorder.findOfficialEodByDate(BOOK.value, WEEKDAY) } returns null
        coEvery { varCalcService.calculateVaR(any(), any(), any(), any(), any()) } returns varResult()
        coEvery { eodPromotionService.promoteToOfficialEodAutomatically(JOB_ID) } returns officialEodJob()

        val j = job()
        j.tick()

        coVerify { varCalcService.calculateVaR(
            match { it.bookId == BOOK },
            triggerType = TriggerType.AUTO_CLOSE,
            runLabel = RunLabel.PRE_CLOSE,
            triggeredBy = "AUTO_CLOSE",
            correlationId = any(),
        ) }
        coVerify { eodPromotionService.promoteToOfficialEodAutomatically(JOB_ID) }
    }

    test("skips book that already has OFFICIAL_EOD for today") {
        coEvery { jobRecorder.findOfficialEodByDate(BOOK.value, WEEKDAY) } returns officialEodJob()

        val j = job()
        j.tick()

        coVerify(exactly = 0) { varCalcService.calculateVaR(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { eodPromotionService.promoteToOfficialEodAutomatically(any()) }
    }

    test("does not fire on Saturday") {
        val j = job(dateProvider = { SATURDAY })
        j.tick()

        coVerify(exactly = 0) { jobRecorder.findOfficialEodByDate(any(), any()) }
    }

    test("does not fire on Sunday") {
        val j = job(dateProvider = { SUNDAY })
        j.tick()

        coVerify(exactly = 0) { jobRecorder.findOfficialEodByDate(any(), any()) }
    }

    test("continues to next book when VaR calc fails for current book") {
        val jobId2 = UUID.fromString("22222222-2222-2222-2222-222222222222")
        coEvery { jobRecorder.findOfficialEodByDate(BOOK.value, WEEKDAY) } returns null
        coEvery { jobRecorder.findOfficialEodByDate(BOOK_2.value, WEEKDAY) } returns null
        coEvery { varCalcService.calculateVaR(match { it.bookId == BOOK }, any(), any(), any(), any()) } throws RuntimeException("Risk engine timeout")
        coEvery { varCalcService.calculateVaR(match { it.bookId == BOOK_2 }, any(), any(), any(), any()) } returns varResult(jobId2).copy(bookId = BOOK_2)
        coEvery { eodPromotionService.promoteToOfficialEodAutomatically(jobId2) } returns officialEodJob()

        val j = job(bookIds = { listOf(BOOK, BOOK_2) })
        j.tick()

        coVerify { eodPromotionService.promoteToOfficialEodAutomatically(jobId2) }
    }

    test("continues to next book when promotion fails for current book") {
        val jobId2 = UUID.fromString("22222222-2222-2222-2222-222222222222")
        coEvery { jobRecorder.findOfficialEodByDate(any(), WEEKDAY) } returns null
        coEvery { varCalcService.calculateVaR(match { it.bookId == BOOK }, any(), any(), any(), any()) } returns varResult()
        coEvery { varCalcService.calculateVaR(match { it.bookId == BOOK_2 }, any(), any(), any(), any()) } returns varResult(jobId2).copy(bookId = BOOK_2)
        coEvery { eodPromotionService.promoteToOfficialEodAutomatically(JOB_ID) } throws RuntimeException("Promotion failed")
        coEvery { eodPromotionService.promoteToOfficialEodAutomatically(jobId2) } returns officialEodJob()

        val j = job(bookIds = { listOf(BOOK, BOOK_2) })
        j.tick()

        coVerify { eodPromotionService.promoteToOfficialEodAutomatically(jobId2) }
    }

    test("processes all books in a single tick") {
        val jobId2 = UUID.fromString("22222222-2222-2222-2222-222222222222")
        coEvery { jobRecorder.findOfficialEodByDate(any(), WEEKDAY) } returns null
        coEvery { varCalcService.calculateVaR(match { it.bookId == BOOK }, any(), any(), any(), any()) } returns varResult()
        coEvery { varCalcService.calculateVaR(match { it.bookId == BOOK_2 }, any(), any(), any(), any()) } returns varResult(jobId2).copy(bookId = BOOK_2)
        coEvery { eodPromotionService.promoteToOfficialEodAutomatically(any()) } returns officialEodJob()

        val j = job(bookIds = { listOf(BOOK, BOOK_2) })
        j.tick()

        coVerify { eodPromotionService.promoteToOfficialEodAutomatically(JOB_ID) }
        coVerify { eodPromotionService.promoteToOfficialEodAutomatically(jobId2) }
    }
})
