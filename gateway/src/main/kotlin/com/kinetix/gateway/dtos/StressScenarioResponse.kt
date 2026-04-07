package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.StressScenarioItem
import kotlinx.serialization.Serializable

@Serializable
data class StressScenarioResponse(
    val id: String,
    val name: String,
    val description: String,
    val shocks: String,
    val status: String,
    val createdBy: String,
    val approvedBy: String?,
    val approvedAt: String?,
    val createdAt: String,
    val scenarioType: String = "PARAMETRIC",
)

fun StressScenarioItem.toResponse(): StressScenarioResponse = StressScenarioResponse(
    id = id,
    name = name,
    description = description,
    shocks = shocks,
    status = status,
    createdBy = createdBy,
    approvedBy = approvedBy,
    approvedAt = approvedAt,
    createdAt = createdAt,
    scenarioType = scenarioType,
)
