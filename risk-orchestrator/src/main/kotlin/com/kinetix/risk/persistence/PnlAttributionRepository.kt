package com.kinetix.risk.persistence

import com.kinetix.common.model.BookId
import com.kinetix.risk.model.PnlAttribution
import java.time.LocalDate

interface PnlAttributionRepository {
    suspend fun save(attribution: PnlAttribution)
    suspend fun findByBookIdAndDate(bookId: BookId, date: LocalDate): PnlAttribution?
    suspend fun findLatestByBookId(bookId: BookId): PnlAttribution?
    suspend fun findByBookId(
        bookId: BookId,
        fromDate: LocalDate = LocalDate.now().minusDays(90),
    ): List<PnlAttribution>
}
