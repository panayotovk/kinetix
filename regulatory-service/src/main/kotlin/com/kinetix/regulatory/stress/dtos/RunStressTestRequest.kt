package com.kinetix.regulatory.stress.dtos

import kotlinx.serialization.Serializable

@Serializable
data class RunStressTestRequest(
    val bookId: String,
    val modelVersion: String? = null,
)
