package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.RebalancingWhatIfResultSummary
import kotlinx.serialization.Serializable

@Serializable
data class RebalancingWhatIfGatewayResponse(
    val baseVar: String,
    val rebalancedVar: String,
    val varChange: String,
    val varChangePct: String,
    val baseExpectedShortfall: String,
    val rebalancedExpectedShortfall: String,
    val esChange: String,
    val baseGreeks: GreeksResponse? = null,
    val rebalancedGreeks: GreeksResponse? = null,
    val greeksChange: GreeksChangeResponse,
    val tradeContributions: List<TradeVarContributionResponse>,
    val estimatedExecutionCost: String,
    val calculatedAt: String,
)

fun RebalancingWhatIfResultSummary.toRebalancingResponse(): RebalancingWhatIfGatewayResponse =
    RebalancingWhatIfGatewayResponse(
        baseVar = baseVar,
        rebalancedVar = rebalancedVar,
        varChange = varChange,
        varChangePct = varChangePct,
        baseExpectedShortfall = baseExpectedShortfall,
        rebalancedExpectedShortfall = rebalancedExpectedShortfall,
        esChange = esChange,
        baseGreeks = baseGreeks?.toResponse(),
        rebalancedGreeks = rebalancedGreeks?.toResponse(),
        greeksChange = GreeksChangeResponse(
            deltaChange = greeksChange.deltaChange,
            gammaChange = greeksChange.gammaChange,
            vegaChange = greeksChange.vegaChange,
            thetaChange = greeksChange.thetaChange,
            rhoChange = greeksChange.rhoChange,
        ),
        tradeContributions = tradeContributions.map {
            TradeVarContributionResponse(
                instrumentId = it.instrumentId,
                side = it.side,
                quantity = it.quantity,
                marginalVarImpact = it.marginalVarImpact,
                executionCost = it.executionCost,
            )
        },
        estimatedExecutionCost = estimatedExecutionCost,
        calculatedAt = calculatedAt,
    )
