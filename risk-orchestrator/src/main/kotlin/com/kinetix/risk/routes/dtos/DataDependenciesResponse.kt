package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class DataDependenciesResponse(
    val bookId: String,
    val dependencies: List<MarketDataDependencyDto>,
)
