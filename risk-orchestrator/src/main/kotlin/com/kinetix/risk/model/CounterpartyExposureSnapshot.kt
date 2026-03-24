package com.kinetix.risk.model

import java.time.Instant

data class ExposureAtTenor(
    val tenor: String,
    val tenorYears: Double,
    val expectedExposure: Double,
    val pfe95: Double,
    val pfe99: Double,
)

data class CounterpartyExposureSnapshot(
    val id: Long? = null,
    val counterpartyId: String,
    val calculatedAt: Instant,
    val pfeProfile: List<ExposureAtTenor>,
    val currentNetExposure: Double,
    val peakPfe: Double,
    val cva: Double?,
    val cvaEstimated: Boolean,
    val currency: String = "USD",
)
