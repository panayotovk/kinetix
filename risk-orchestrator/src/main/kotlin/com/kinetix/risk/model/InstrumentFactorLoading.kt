package com.kinetix.risk.model

import java.time.LocalDate

data class InstrumentFactorLoading(
    val instrumentId: String,
    val factorName: String,
    val loading: Double,
    val rSquared: Double?,
    val method: String,
    val estimationDate: LocalDate,
    val estimationWindow: Int,
) {
    /** Returns true when the loading was estimated before the given cutoff date. */
    fun isStale(cutoff: LocalDate): Boolean = estimationDate.isBefore(cutoff)
}
