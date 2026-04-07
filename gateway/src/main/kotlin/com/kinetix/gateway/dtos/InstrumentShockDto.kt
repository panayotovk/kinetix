package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.InstrumentShockSummary
import kotlinx.serialization.Serializable

@Serializable
data class InstrumentShockDto(
    val instrumentId: String,
    val shock: String,
)

fun InstrumentShockSummary.toDto(): InstrumentShockDto = InstrumentShockDto(
    instrumentId = instrumentId,
    shock = shock,
)
