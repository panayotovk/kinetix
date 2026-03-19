package com.kinetix.gateway.dto

import kotlinx.serialization.Serializable

@Serializable
data class StressedCrossBookVaRRequestDto(
    val bookIds: List<String>,
    val portfolioGroupId: String,
    val stressCorrelation: Double? = null,
    val calculationType: String? = null,
    val confidenceLevel: String? = null,
    val timeHorizonDays: String? = null,
    val numSimulations: String? = null,
)

data class StressedCrossBookVaRParams(
    val bookIds: List<String>,
    val portfolioGroupId: String,
    val stressCorrelation: Double,
    val calculationType: String,
    val confidenceLevel: String,
    val timeHorizonDays: Int,
    val numSimulations: Int,
)

private val validCalculationTypes = setOf("HISTORICAL", "PARAMETRIC", "MONTE_CARLO")
private val validConfidenceLevels = setOf("CL_95", "CL_975", "CL_99")

fun StressedCrossBookVaRRequestDto.toParams(): StressedCrossBookVaRParams {
    require(bookIds.isNotEmpty()) { "bookIds must not be empty" }
    require(portfolioGroupId.isNotBlank()) { "portfolioGroupId must not be blank" }
    val calcType = calculationType ?: "PARAMETRIC"
    require(calcType in validCalculationTypes) {
        "Invalid calculationType: $calcType. Must be one of $validCalculationTypes"
    }
    val confLevel = confidenceLevel ?: "CL_95"
    require(confLevel in validConfidenceLevels) {
        "Invalid confidenceLevel: $confLevel. Must be one of $validConfidenceLevels"
    }
    return StressedCrossBookVaRParams(
        bookIds = bookIds,
        portfolioGroupId = portfolioGroupId,
        stressCorrelation = stressCorrelation ?: 0.9,
        calculationType = calcType,
        confidenceLevel = confLevel,
        timeHorizonDays = timeHorizonDays?.toInt() ?: 1,
        numSimulations = numSimulations?.toInt() ?: 10_000,
    )
}
