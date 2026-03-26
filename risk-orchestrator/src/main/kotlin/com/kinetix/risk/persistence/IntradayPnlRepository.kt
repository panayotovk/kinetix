package com.kinetix.risk.persistence

import com.kinetix.common.model.BookId
import com.kinetix.risk.model.IntradayPnlSnapshot
import java.time.Instant

interface IntradayPnlRepository {
    suspend fun save(snapshot: IntradayPnlSnapshot)
    suspend fun findLatest(bookId: BookId): IntradayPnlSnapshot?
    suspend fun findSeries(bookId: BookId, from: Instant, to: Instant): List<IntradayPnlSnapshot>
}

// TODO(IPNL-07): Add a Redis-backed implementation of findLatest() to serve latency-sensitive
// callers (e.g. the UI live P&L panel) without hitting the database on every poll.
// Pattern: on save(), write the latest snapshot to Redis with a short TTL (e.g. 60s);
// findLatest() reads from Redis first and falls back to the DB on a miss.
// Use the existing Lettuce client plumbing from RedisVaRCache as a reference.
