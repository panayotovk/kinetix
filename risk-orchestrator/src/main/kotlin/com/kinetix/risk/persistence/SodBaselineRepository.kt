package com.kinetix.risk.persistence

import com.kinetix.common.model.BookId
import com.kinetix.risk.model.SodBaseline
import java.time.LocalDate

interface SodBaselineRepository {
    suspend fun save(baseline: SodBaseline)
    suspend fun findByBookIdAndDate(bookId: BookId, date: LocalDate): SodBaseline?
    suspend fun deleteByBookIdAndDate(bookId: BookId, date: LocalDate)
}
