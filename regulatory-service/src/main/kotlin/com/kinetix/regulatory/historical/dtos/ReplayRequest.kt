package com.kinetix.regulatory.historical.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ReplayRequest(
    val bookId: String,
)
