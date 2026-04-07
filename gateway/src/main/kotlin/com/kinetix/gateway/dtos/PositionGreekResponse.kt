package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.PositionGreekSummary
import kotlinx.serialization.Serializable

@Serializable
data class PositionGreekResponse(
    val instrumentId: String,
    val delta: String,
    val gamma: String,
    val vega: String,
    val theta: String,
    val rho: String,
)

fun PositionGreekSummary.toResponse(): PositionGreekResponse = PositionGreekResponse(
    instrumentId = instrumentId,
    delta = "%.6f".format(delta),
    gamma = "%.6f".format(gamma),
    vega = "%.6f".format(vega),
    theta = "%.6f".format(theta),
    rho = "%.6f".format(rho),
)
