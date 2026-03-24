package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ExposureAtTenorResponse(
    val tenor: String,
    val tenorYears: Double,
    val expectedExposure: Double,
    val pfe95: Double,
    val pfe99: Double,
)

@Serializable
data class CounterpartyExposureResponse(
    val counterpartyId: String,
    val calculatedAt: String,
    val currentNetExposure: Double,
    val peakPfe: Double,
    val pfeProfile: List<ExposureAtTenorResponse>,
    val cva: Double?,
    val cvaEstimated: Boolean,
    val currency: String,
)

@Serializable
data class ComputePFERequest(
    val positions: List<PFEPositionInputDto>,
    val numSimulations: Int = 0,
    val seed: Long = 0,
)

@Serializable
data class PFEPositionInputDto(
    val instrumentId: String,
    val marketValue: Double,
    val assetClass: String,
    val volatility: Double = 0.20,
    val sector: String = "",
)

@Serializable
data class CVAResponse(
    val counterpartyId: String,
    val cva: Double,
    val isEstimated: Boolean,
    val hazardRate: Double,
    val pd1y: Double,
)
