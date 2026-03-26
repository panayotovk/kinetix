package com.kinetix.risk.schedule

import com.kinetix.common.model.BookId
import com.kinetix.risk.service.FactorRiskService
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.slf4j.LoggerFactory
import java.time.LocalTime
import kotlin.coroutines.coroutineContext

/**
 * Daily scheduled job that runs factor loading re-estimation and factor risk decomposition
 * for every active book.
 *
 * The job runs once per day at or after [runAtTime].  For each book it calls
 * [FactorRiskService.decomposeForBook] with a [totalVar] of zero — the service fetches
 * live prices and positions, so no prior VaR run is required.  The factor model internally
 * scales its output relative to the portfolio's own variance.
 *
 * Per-book failures are isolated: an error on one book does not abort the rest of the batch.
 */
class ScheduledFactorDecomposition(
    private val factorRiskService: FactorRiskService,
    private val bookIds: suspend () -> List<BookId>,
    private val runAtTime: LocalTime = LocalTime.of(4, 0),
    private val intervalMillis: Long = 60_000,
    private val nowProvider: () -> LocalTime = { LocalTime.now() },
    private val lock: DistributedLock = NoOpDistributedLock(),
) {
    private val logger = LoggerFactory.getLogger(ScheduledFactorDecomposition::class.java)

    suspend fun start() {
        while (coroutineContext.isActive) {
            lock.withLock("scheduled-factor-decomposition", ttlSeconds = intervalMillis / 1000) {
                try {
                    tick()
                } catch (e: Exception) {
                    logger.error("Daily factor decomposition batch failed unexpectedly", e)
                }
            }
            delay(intervalMillis)
        }
    }

    /**
     * Runs factor decomposition for all books if [nowProvider] is at or after [runAtTime].
     *
     * @return the number of books successfully decomposed.
     */
    suspend fun tick(): Int {
        val now = nowProvider()
        if (now.isBefore(runAtTime)) {
            return 0
        }

        val books = bookIds()
        logger.info("Starting daily factor decomposition batch for {} books", books.size)

        var successCount = 0
        for (bookId in books) {
            try {
                factorRiskService.decomposeForBook(bookId, totalVar = 0.0)
                successCount++
                logger.debug("Factor decomposition complete for book {}", bookId.value)
            } catch (e: Exception) {
                logger.error(
                    "Daily factor decomposition failed for book {}: {}",
                    bookId.value, e.message, e,
                )
            }
        }

        logger.info(
            "Daily factor decomposition batch complete: {}/{} books succeeded",
            successCount, books.size,
        )
        return successCount
    }
}
