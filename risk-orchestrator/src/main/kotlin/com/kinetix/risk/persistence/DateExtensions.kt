package com.kinetix.risk.persistence

import java.time.LocalDate

internal fun LocalDate.toKotlinxDate(): kotlinx.datetime.LocalDate =
    kotlinx.datetime.LocalDate(year, monthValue, dayOfMonth)

internal fun kotlinx.datetime.LocalDate.toJavaDate(): LocalDate =
    LocalDate.of(year, monthNumber, dayOfMonth)
