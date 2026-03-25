package com.kinetix.risk.client.dtos

import kotlinx.serialization.Serializable

@Serializable
data class BenchmarkDetailDto(
    val benchmarkId: String,
    val name: String,
    val description: String?,
    val createdAt: String,
    val constituents: List<BenchmarkConstituentDto> = emptyList(),
)
