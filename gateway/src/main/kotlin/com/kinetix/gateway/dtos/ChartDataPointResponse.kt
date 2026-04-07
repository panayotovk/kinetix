package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.ChartDataPointItem
import kotlinx.serialization.Serializable

@Serializable
data class ChartDataPointResponse(
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
    val jobCount: Int,
    val completedCount: Int,
    val failedCount: Int,
    val runningCount: Int,
)

fun ChartDataPointItem.toResponse(): ChartDataPointResponse = ChartDataPointResponse(
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
