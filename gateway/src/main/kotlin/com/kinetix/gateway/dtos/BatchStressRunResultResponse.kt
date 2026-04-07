package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.BatchStressRunSummary
import kotlinx.serialization.Serializable

@Serializable
data class BatchStressRunResultResponse(
    val results: List<BatchScenarioResultDto>,
    val failedScenarios: List<BatchScenarioFailureDto>,
    val worstScenarioName: String?,
    val worstPnlImpact: String?,
)

fun BatchStressRunSummary.toResponse() = BatchStressRunResultResponse(
    results = results.map { it.toDto() },
    failedScenarios = failedScenarios.map { it.toDto() },
    worstScenarioName = worstScenarioName,
    worstPnlImpact = worstPnlImpact,
)
