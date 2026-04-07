package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.HistoricalReplayParams
import com.kinetix.gateway.client.InstrumentDailyReturnsParam
import kotlinx.serialization.Serializable

@Serializable
data class HistoricalReplayRequest(
    val instrumentReturns: List<InstrumentDailyReturnsRequest> = emptyList(),
    val scenarioName: String? = null,
    val windowStart: String? = null,
    val windowEnd: String? = null,
)

fun HistoricalReplayRequest.toParams(bookId: String): HistoricalReplayParams = HistoricalReplayParams(
    bookId = bookId,
    instrumentReturns = instrumentReturns.map {
        InstrumentDailyReturnsParam(instrumentId = it.instrumentId, dailyReturns = it.dailyReturns)
    },
    scenarioName = scenarioName,
    windowStart = windowStart,
    windowEnd = windowEnd,
)
