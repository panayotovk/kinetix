package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.RiskClassChargeItem
import kotlinx.serialization.Serializable

@Serializable
data class RiskClassChargeDto(
    val riskClass: String,
    val deltaCharge: String,
    val vegaCharge: String,
    val curvatureCharge: String,
    val totalCharge: String,
)

fun RiskClassChargeItem.toDto(): RiskClassChargeDto = RiskClassChargeDto(
    riskClass = riskClass,
    deltaCharge = "%.2f".format(deltaCharge),
    vegaCharge = "%.2f".format(vegaCharge),
    curvatureCharge = "%.2f".format(curvatureCharge),
    totalCharge = "%.2f".format(totalCharge),
)
