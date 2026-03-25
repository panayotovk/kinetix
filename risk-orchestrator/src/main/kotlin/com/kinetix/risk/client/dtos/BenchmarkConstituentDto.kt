package com.kinetix.risk.client.dtos

import kotlinx.serialization.Serializable

@Serializable
data class BenchmarkConstituentDto(
    val instrumentId: String,
    val weight: String,
    val asOfDate: String,
)
