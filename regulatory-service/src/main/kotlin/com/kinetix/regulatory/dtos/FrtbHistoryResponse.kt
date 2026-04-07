package com.kinetix.regulatory.dtos

import kotlinx.serialization.Serializable

@Serializable
data class FrtbHistoryResponse(
    val calculations: List<FrtbCalculationResponse>,
    val total: Int,
    val limit: Int,
    val offset: Int,
)
