package com.kinetix.risk.persistence

import com.kinetix.common.model.BookId
import com.kinetix.risk.model.IntradayPnlSnapshot
import java.time.Instant

interface IntradayPnlRepository {
    suspend fun save(snapshot: IntradayPnlSnapshot)
    suspend fun findLatest(bookId: BookId): IntradayPnlSnapshot?
    suspend fun findSeries(bookId: BookId, from: Instant, to: Instant): List<IntradayPnlSnapshot>
}
