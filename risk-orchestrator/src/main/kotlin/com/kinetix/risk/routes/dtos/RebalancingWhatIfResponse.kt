package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class RebalancingWhatIfResponse(
    val baseVar: String,
    val rebalancedVar: String,
    val varChange: String,
    val varChangePct: String,
    val baseExpectedShortfall: String,
    val rebalancedExpectedShortfall: String,
    val esChange: String,
    val baseGreeks: GreeksResponse?,
    val rebalancedGreeks: GreeksResponse?,
    val greeksChange: GreeksChangeDto,
    val tradeContributions: List<TradeVarContributionDto>,
    val estimatedExecutionCost: String,
    val calculatedAt: String,
)
