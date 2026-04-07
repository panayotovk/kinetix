package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.DependenciesParams
import kotlinx.serialization.Serializable

private val validCalculationTypes = setOf("HISTORICAL", "PARAMETRIC", "MONTE_CARLO")
private val validConfidenceLevels = setOf("CL_95", "CL_975", "CL_99")

@Serializable
data class DependenciesRequest(
    val calculationType: String? = null,
    val confidenceLevel: String? = null,
)

fun DependenciesRequest.toParams(bookId: String): DependenciesParams {
    val calcType = calculationType ?: "PARAMETRIC"
    require(calcType in validCalculationTypes) {
        "Invalid calculationType: $calcType. Must be one of $validCalculationTypes"
    }
    val confLevel = confidenceLevel ?: "CL_95"
    require(confLevel in validConfidenceLevels) {
        "Invalid confidenceLevel: $confLevel. Must be one of $validConfidenceLevels"
    }
    return DependenciesParams(
        bookId = bookId,
        calculationType = calcType,
        confidenceLevel = confLevel,
    )
}
