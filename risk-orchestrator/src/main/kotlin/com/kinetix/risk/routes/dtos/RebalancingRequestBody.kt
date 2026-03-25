package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class RebalancingRequestBody(
    val trades: List<RebalancingTradeDto>,
    val calculationType: String? = null,
    val confidenceLevel: String? = null,
)
