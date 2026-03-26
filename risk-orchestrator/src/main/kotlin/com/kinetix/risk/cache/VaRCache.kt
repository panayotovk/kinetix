package com.kinetix.risk.cache

import com.kinetix.risk.model.ValuationResult

interface VaRCache {
    fun put(bookId: String, result: ValuationResult)
    fun get(bookId: String): ValuationResult?
}

// TODO(HIER-09): After each put(), compare the new varValue against the previous cached value.
// If the absolute change exceeds a configurable threshold (e.g. 10%), emit a VAR_CHANGE_ALERT
// event to the notification-service so risk managers are proactively informed of large intraday
// VaR movements. The previous value is accessible via get() before put() overwrites it.
