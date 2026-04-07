package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.MarketDataDependencyItem
import kotlinx.serialization.Serializable

@Serializable
data class MarketDataDependencyResponse(
    val dataType: String,
    val instrumentId: String,
    val assetClass: String,
    val required: Boolean,
    val description: String,
    val parameters: Map<String, String>,
)

fun MarketDataDependencyItem.toDto(): MarketDataDependencyResponse = MarketDataDependencyResponse(
    dataType = dataType,
    instrumentId = instrumentId,
    assetClass = assetClass,
    required = required,
    description = description,
    parameters = parameters,
)
