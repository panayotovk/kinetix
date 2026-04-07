package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.ComponentBreakdownItem
import kotlinx.serialization.Serializable

@Serializable
data class ComponentBreakdownDto(
    val assetClass: String,
    val varContribution: String,
    val percentageOfTotal: String,
)

fun ComponentBreakdownItem.toDto(): ComponentBreakdownDto = ComponentBreakdownDto(
    assetClass = assetClass,
    varContribution = "%.2f".format(varContribution),
    percentageOfTotal = "%.2f".format(percentageOfTotal),
)
