package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.GreekValuesItem
import kotlinx.serialization.Serializable

@Serializable
data class GreekValuesDto(
    val assetClass: String,
    val delta: String,
    val gamma: String,
    val vega: String,
)

fun GreekValuesItem.toDto(): GreekValuesDto = GreekValuesDto(
    assetClass = assetClass,
    delta = "%.6f".format(delta),
    gamma = "%.6f".format(gamma),
    vega = "%.6f".format(vega),
)
