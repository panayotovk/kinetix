package com.kinetix.regulatory.historical.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReplayRequest(
    val bookId: String,
)
