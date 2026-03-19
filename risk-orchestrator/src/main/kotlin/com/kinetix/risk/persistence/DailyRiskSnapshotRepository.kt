package com.kinetix.risk.persistence

import com.kinetix.common.model.BookId
import com.kinetix.risk.model.DailyRiskSnapshot
import java.time.LocalDate

interface DailyRiskSnapshotRepository {
    suspend fun save(snapshot: DailyRiskSnapshot)
    suspend fun saveAll(snapshots: List<DailyRiskSnapshot>)
    suspend fun findByBookIdAndDate(bookId: BookId, date: LocalDate): List<DailyRiskSnapshot>
    suspend fun findByBookId(
        bookId: BookId,
        fromDate: LocalDate = LocalDate.now().minusDays(90),
    ): List<DailyRiskSnapshot>
    suspend fun deleteByBookIdAndDate(bookId: BookId, date: LocalDate)
}
