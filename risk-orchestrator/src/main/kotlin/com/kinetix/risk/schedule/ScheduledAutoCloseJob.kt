package com.kinetix.risk.schedule

import com.kinetix.common.model.BookId
import com.kinetix.risk.model.*
import com.kinetix.risk.service.EodPromotionService
import com.kinetix.risk.service.VaRCalculationService
import com.kinetix.risk.service.ValuationJobRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.slf4j.LoggerFactory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.coroutines.coroutineContext

class ScheduledAutoCloseJob(
    private val varCalculationService: VaRCalculationService,
    private val eodPromotionService: EodPromotionService,
    private val jobRecorder: ValuationJobRecorder,
    private val bookIds: suspend () -> List<BookId>,
    private val closeTime: LocalTime = LocalTime.of(17, 30),
    private val intervalMillis: Long = 60_000,
    private val nowProvider: () -> LocalTime = { LocalTime.now() },
    private val dateProvider: () -> LocalDate = { LocalDate.now() },
    private val lock: DistributedLock = NoOpDistributedLock(),
) {
    private val logger = LoggerFactory.getLogger(ScheduledAutoCloseJob::class.java)

    suspend fun start() {
        while (coroutineContext.isActive) {
            lock.withLock("scheduled-auto-close", ttlSeconds = intervalMillis / 1000) {
                try {
                    tick()
                } catch (e: Exception) {
                    logger.error("Failed to fetch book list for scheduled auto-close", e)
                }
            }
            delay(intervalMillis)
        }
    }

    suspend fun tick() {
        val now = nowProvider()
        if (now.isBefore(closeTime)) return

        val today = dateProvider()
        val dayOfWeek = today.dayOfWeek
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) return

        val books = bookIds()

        for (bookId in books) {
            try {
                val existingEod = jobRecorder.findOfficialEodByDate(bookId.value, today)
                if (existingEod != null) continue

                val result = varCalculationService.calculateVaR(
                    request = VaRCalculationRequest(
                        bookId = bookId,
                        calculationType = CalculationType.PARAMETRIC,
                        confidenceLevel = ConfidenceLevel.CL_99,
                        requestedOutputs = setOf(
                            ValuationOutput.VAR,
                            ValuationOutput.EXPECTED_SHORTFALL,
                            ValuationOutput.GREEKS,
                            ValuationOutput.PV,
                        ),
                    ),
                    triggerType = TriggerType.AUTO_CLOSE,
                    runLabel = RunLabel.PRE_CLOSE,
                    triggeredBy = "AUTO_CLOSE",
                )

                val jobId = result?.jobId
                if (jobId != null) {
                    eodPromotionService.promoteToOfficialEodAutomatically(jobId)
                    logger.info("auto_close_eod_promoted book_id={} job_id={} date={}", bookId.value, jobId, today)
                } else {
                    logger.warn("auto_close_var_returned_null book_id={} date={}", bookId.value, today)
                }
            } catch (e: Exception) {
                logger.error("auto_close_failed book_id={} date={}", bookId.value, today, e)
            }
        }
    }
}
