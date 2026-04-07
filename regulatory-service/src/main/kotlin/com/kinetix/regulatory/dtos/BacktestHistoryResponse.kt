package com.kinetix.regulatory.dtos

import kotlinx.serialization.Serializable

@Serializable
data class BacktestHistoryResponse(
    val results: List<BacktestResultResponse>,
    val total: Int,
    val limit: Int,
    val offset: Int,
)
