package com.kinetix.risk.client.dtos

import com.kinetix.common.model.instrument.InstrumentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

private val json = Json { ignoreUnknownKeys = true }

@Serializable
data class InstrumentDto(
    val instrumentId: String,
    val instrumentType: String,
    val displayName: String,
    val assetClass: String,
    val currency: String,
    val attributes: JsonObject,
    val createdAt: String,
    val updatedAt: String,
) {
    fun toInstrumentType(): InstrumentType =
        json.decodeFromString(InstrumentType.serializer(), attributes.toString())
}
