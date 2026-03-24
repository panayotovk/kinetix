package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class RiskContributorResponse(
    val entityId: String,
    val entityName: String,
    val varContribution: String,
    val pctOfTotal: String,
)
