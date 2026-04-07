package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.ChartDataSummary
import kotlinx.serialization.Serializable

@Serializable
data class ChartDataGatewayResponse(
    val points: List<ChartDataPointResponse>,
    val bucketSizeMs: Long,
)

fun ChartDataSummary.toResponse(): ChartDataGatewayResponse = ChartDataGatewayResponse(
    points = points.map { it.toResponse() },
    bucketSizeMs = bucketSizeMs,
)
