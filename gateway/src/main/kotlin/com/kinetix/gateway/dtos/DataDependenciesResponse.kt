package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.DataDependenciesSummary
import kotlinx.serialization.Serializable

@Serializable
data class DataDependenciesResponse(
    val bookId: String,
    val dependencies: List<MarketDataDependencyResponse>,
)

fun DataDependenciesSummary.toResponse(): DataDependenciesResponse = DataDependenciesResponse(
    bookId = bookId,
    dependencies = dependencies.map { it.toDto() },
)
