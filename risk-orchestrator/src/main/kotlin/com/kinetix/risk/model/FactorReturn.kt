package com.kinetix.risk.model

import java.time.LocalDate

data class FactorReturn(
    val factorName: String,
    val asOfDate: LocalDate,
    val returnValue: Double,
    val source: String,
)
