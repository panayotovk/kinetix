package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.PositionRiskSummaryItem
import kotlinx.serialization.Serializable

@Serializable
data class WhatIfGatewayPositionRiskDto(
    val instrumentId: String,
    val assetClass: String,
    val marketValue: String,
    val delta: String? = null,
    val gamma: String? = null,
    val vega: String? = null,
    val varContribution: String,
    val esContribution: String,
    val percentageOfTotal: String,
)

fun PositionRiskSummaryItem.toPositionRiskResponse(): WhatIfGatewayPositionRiskDto =
    toWhatIfDto()

fun PositionRiskSummaryItem.toWhatIfDto(): WhatIfGatewayPositionRiskDto =
    WhatIfGatewayPositionRiskDto(
        instrumentId = instrumentId,
        assetClass = assetClass,
        marketValue = marketValue,
        delta = delta,
        gamma = gamma,
        vega = vega,
        varContribution = varContribution,
        esContribution = esContribution,
        percentageOfTotal = percentageOfTotal,
    )
