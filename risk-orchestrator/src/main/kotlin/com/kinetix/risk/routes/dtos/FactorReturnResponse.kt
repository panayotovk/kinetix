package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class FactorReturnResponse(
    val factorName: String,
    val asOfDate: String,
    val returnValue: Double,
    val source: String,
)

@Serializable
data class FactorReturnRequest(
    val factorName: String,
    val asOfDate: String,
    val returnValue: Double,
    val source: String,
)
