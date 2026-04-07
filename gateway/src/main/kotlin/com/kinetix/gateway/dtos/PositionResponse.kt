package com.kinetix.gateway.dtos

import com.kinetix.common.model.Position
import com.kinetix.gateway.client.InstrumentSummary
import kotlinx.serialization.Serializable

@Serializable
data class PositionResponse(
    val bookId: String,
    val instrumentId: String,
    val assetClass: String,
    val quantity: String,
    val averageCost: MoneyDto,
    val marketPrice: MoneyDto,
    val marketValue: MoneyDto,
    val unrealizedPnl: MoneyDto,
    val realizedPnl: MoneyDto,
    val instrumentType: String? = null,
    val displayName: String? = null,
    val strategyId: String? = null,
    val strategyType: String? = null,
    val strategyName: String? = null,
)

fun Position.toResponse(): PositionResponse = toResponse(emptyMap())

fun Position.toResponse(instruments: Map<String, InstrumentSummary>): PositionResponse {
    val instrument = instruments[instrumentId.value]
    return PositionResponse(
        bookId = bookId.value,
        instrumentId = instrumentId.value,
        assetClass = assetClass.name,
        quantity = quantity.toPlainString(),
        averageCost = averageCost.toDto(),
        marketPrice = marketPrice.toDto(),
        marketValue = marketValue.toDto(),
        unrealizedPnl = unrealizedPnl.toDto(),
        realizedPnl = realizedPnl.toDto(),
        instrumentType = instrument?.instrumentType ?: instrumentType,
        displayName = instrument?.displayName,
        strategyId = strategyId,
        strategyType = strategyType,
        strategyName = strategyName,
    )
}
