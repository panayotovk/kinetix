package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.BatchScenarioFailureItem
import kotlinx.serialization.Serializable

@Serializable
data class BatchScenarioFailureDto(
    val scenarioName: String,
    val errorMessage: String,
)

fun BatchScenarioFailureItem.toDto() = BatchScenarioFailureDto(
    scenarioName = scenarioName,
    errorMessage = errorMessage,
)
