package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.GreeksResultSummary
import kotlinx.serialization.Serializable

@Serializable
data class GreeksResponse(
    val bookId: String,
    val assetClassGreeks: List<GreekValuesDto>,
    val theta: String,
    val rho: String,
    val calculatedAt: String,
)

fun GreeksResultSummary.toResponse(): GreeksResponse = GreeksResponse(
    bookId = bookId,
    assetClassGreeks = assetClassGreeks.map { it.toDto() },
    theta = "%.6f".format(theta),
    rho = "%.6f".format(rho),
    calculatedAt = calculatedAt.toString(),
)
