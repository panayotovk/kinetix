package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.StressTestBatchParams
import kotlinx.serialization.Serializable

private val validCalculationTypes = setOf("HISTORICAL", "PARAMETRIC", "MONTE_CARLO")
private val validConfidenceLevels = setOf("CL_95", "CL_975", "CL_99")

@Serializable
data class StressTestBatchRequest(
    val scenarioNames: List<String>,
    val calculationType: String? = null,
    val confidenceLevel: String? = null,
    val timeHorizonDays: String? = null,
)

fun StressTestBatchRequest.toParams(bookId: String): StressTestBatchParams {
    val calcType = calculationType ?: "PARAMETRIC"
    require(calcType in validCalculationTypes) {
        "Invalid calculationType: $calcType. Must be one of $validCalculationTypes"
    }
    val confLevel = confidenceLevel ?: "CL_95"
    require(confLevel in validConfidenceLevels) {
        "Invalid confidenceLevel: $confLevel. Must be one of $validConfidenceLevels"
    }
    return StressTestBatchParams(
        bookId = bookId,
        scenarioNames = scenarioNames,
        calculationType = calcType,
        confidenceLevel = confLevel,
        timeHorizonDays = timeHorizonDays?.toInt() ?: 1,
    )
}
