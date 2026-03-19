package com.kinetix.risk.cache

import com.kinetix.risk.model.ValuationResult
import java.util.concurrent.ConcurrentHashMap

class InMemoryVaRCache : VaRCache {
    private val cache = ConcurrentHashMap<String, ValuationResult>()

    override fun put(bookId: String, result: ValuationResult) {
        cache[bookId] = result
    }

    override fun get(bookId: String): ValuationResult? = cache[bookId]
}
