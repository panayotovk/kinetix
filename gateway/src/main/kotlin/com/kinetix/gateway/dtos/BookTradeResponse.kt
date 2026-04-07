package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.BookTradeResult
import com.kinetix.gateway.client.InstrumentSummary
import kotlinx.serialization.Serializable

@Serializable
data class BookTradeResponse(
    val trade: TradeResponse,
    val position: PositionResponse,
)

fun BookTradeResult.toResponse(): BookTradeResponse = toResponse(emptyMap())

fun BookTradeResult.toResponse(instruments: Map<String, InstrumentSummary>): BookTradeResponse = BookTradeResponse(
    trade = trade.toResponse(),
    position = position.toResponse(instruments),
)
