package com.kinetix.gateway.dtos

import kotlinx.serialization.Serializable

@Serializable
data class InstrumentDailyReturnsRequest(
    val instrumentId: String,
    val dailyReturns: List<Double>,
)
