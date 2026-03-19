package com.kinetix.risk.cache

import com.kinetix.risk.model.ValuationResult

interface VaRCache {
    fun put(bookId: String, result: ValuationResult)
    fun get(bookId: String): ValuationResult?
}
