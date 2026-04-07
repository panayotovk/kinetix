package com.kinetix.regulatory.stress.dtos

import kotlinx.serialization.Serializable

@Serializable
data class InstrumentShock(
    val instrumentId: String,
    val shock: String,
)
