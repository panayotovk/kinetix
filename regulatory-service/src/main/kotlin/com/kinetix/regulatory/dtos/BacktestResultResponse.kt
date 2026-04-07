package com.kinetix.regulatory.dtos

import kotlinx.serialization.Serializable

@Serializable
data class BacktestResultResponse(
    val id: String,
    val bookId: String,
    val calculationType: String,
    val confidenceLevel: String,
    val totalDays: Int,
    val violationCount: Int,
    val violationRate: String,
    val kupiecStatistic: String,
    val kupiecPValue: String,
    val kupiecPass: Boolean,
    val christoffersenStatistic: String,
    val christoffersenPValue: String,
    val christoffersenPass: Boolean,
    val trafficLightZone: String,
    val calculatedAt: String,
)
