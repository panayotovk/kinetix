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
data class NettingSetExposureResponse(
    val nettingSetId: String,
    val agreementType: String,
    val netExposure: Double,
    val peakPfe: Double,
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
    val nettingSetExposures: List<NettingSetExposureResponse> = emptyList(),
    val collateralHeld: Double = 0.0,
    val collateralPosted: Double = 0.0,
    val netNetExposure: Double? = null,
    val wrongWayRiskFlags: List<String> = emptyList(),
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
