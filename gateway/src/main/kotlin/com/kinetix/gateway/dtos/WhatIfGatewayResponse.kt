package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.WhatIfResultSummary
import kotlinx.serialization.Serializable

@Serializable
data class WhatIfGatewayResponse(
    val baseVaR: String,
    val baseExpectedShortfall: String,
    val baseGreeks: GreeksResponse? = null,
    val basePositionRisk: List<WhatIfGatewayPositionRiskDto>,
    val hypotheticalVaR: String,
    val hypotheticalExpectedShortfall: String,
    val hypotheticalGreeks: GreeksResponse? = null,
    val hypotheticalPositionRisk: List<WhatIfGatewayPositionRiskDto>,
    val varChange: String,
    val esChange: String,
    val calculatedAt: String,
)

fun WhatIfResultSummary.toResponse(): WhatIfGatewayResponse =
    WhatIfGatewayResponse(
        baseVaR = baseVaR,
        baseExpectedShortfall = baseExpectedShortfall,
        baseGreeks = baseGreeks?.toResponse(),
        basePositionRisk = basePositionRisk.map { it.toWhatIfDto() },
        hypotheticalVaR = hypotheticalVaR,
        hypotheticalExpectedShortfall = hypotheticalExpectedShortfall,
        hypotheticalGreeks = hypotheticalGreeks?.toResponse(),
        hypotheticalPositionRisk = hypotheticalPositionRisk.map { it.toWhatIfDto() },
        varChange = varChange,
        esChange = esChange,
        calculatedAt = calculatedAt,
    )
