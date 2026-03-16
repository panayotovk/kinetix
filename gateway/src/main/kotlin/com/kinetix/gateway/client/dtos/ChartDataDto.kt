package com.kinetix.gateway.client.dtos

import com.kinetix.gateway.client.ChartDataPointItem
import com.kinetix.gateway.client.ChartDataSummary
import kotlinx.serialization.Serializable

@Serializable
data class ChartDataPointClientDto(
    val bucket: String,
    val varValue: Double? = null,
    val expectedShortfall: Double? = null,
    val confidenceLevel: String? = null,
    val delta: Double? = null,
    val gamma: Double? = null,
    val vega: Double? = null,
    val theta: Double? = null,
    val rho: Double? = null,
    val pvValue: Double? = null,
    val jobCount: Int = 0,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val runningCount: Int = 0,
)

@Serializable
data class ChartDataClientDto(
    val points: List<ChartDataPointClientDto>,
    val bucketSizeMs: Long,
)

fun ChartDataPointClientDto.toDomain() = ChartDataPointItem(
    bucket = bucket,
    varValue = varValue,
    expectedShortfall = expectedShortfall,
    confidenceLevel = confidenceLevel,
    delta = delta,
    gamma = gamma,
    vega = vega,
    theta = theta,
    rho = rho,
    pvValue = pvValue,
    jobCount = jobCount,
    completedCount = completedCount,
    failedCount = failedCount,
    runningCount = runningCount,
)

fun ChartDataClientDto.toDomain() = ChartDataSummary(
    points = points.map { it.toDomain() },
    bucketSizeMs = bucketSizeMs,
)
