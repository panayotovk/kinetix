package com.kinetix.regulatory.historical.dtos

import kotlinx.serialization.Serializable

@Serializable
data class CustomReplayRequest(
    val bookId: String,
    val instrumentIds: List<String>,
    val startDate: String,
    val endDate: String,
)
