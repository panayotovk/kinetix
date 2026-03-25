package com.kinetix.regulatory.stress.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateScenarioRequest(
    val shocks: String? = null,
    val correlationOverride: String? = null,
    val liquidityStressFactors: String? = null,
)
