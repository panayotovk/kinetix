package com.kinetix.risk.model

import java.time.Instant

data class RebalancingWhatIfResult(
    val baseVar: Double,
    val rebalancedVar: Double,
    val varChange: Double,
    val varChangePct: Double,
    val baseExpectedShortfall: Double,
    val rebalancedExpectedShortfall: Double,
    val esChange: Double,
    val baseGreeks: GreeksResult?,
    val rebalancedGreeks: GreeksResult?,
    val greeksChange: GreeksChange,
    val tradeContributions: List<TradeVarContribution>,
    val estimatedExecutionCost: Double,
    val calculatedAt: Instant,
)
