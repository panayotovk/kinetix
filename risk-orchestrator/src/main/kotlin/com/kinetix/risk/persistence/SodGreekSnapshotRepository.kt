package com.kinetix.risk.persistence

import com.kinetix.common.model.BookId
import com.kinetix.common.model.InstrumentId
import com.kinetix.risk.model.SodGreekSnapshot
import java.time.LocalDate

interface SodGreekSnapshotRepository {
    /** Saves a batch of per-instrument Greek snapshots for the given book and date. */
    suspend fun saveAll(snapshots: List<SodGreekSnapshot>)

    /** Returns all Greek snapshots for the given book on the given date. */
    suspend fun findByBookIdAndDate(bookId: BookId, date: LocalDate): List<SodGreekSnapshot>

    /** Returns a single Greek snapshot for the given instrument on the given date, or null. */
    suspend fun findByInstrumentAndDate(
        bookId: BookId,
        instrumentId: InstrumentId,
        date: LocalDate,
    ): SodGreekSnapshot?

    /**
     * Locks all snapshots for the given book and date, recording [lockedBy] and the lock timestamp.
     * Once locked, these rows must never be modified for the trading day.
     */
    suspend fun lockAll(bookId: BookId, date: LocalDate, lockedBy: String)
}
