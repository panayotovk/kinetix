package com.kinetix.gateway.dtos

import kotlinx.serialization.Serializable

@Serializable
data class GreeksChangeResponse(
    val deltaChange: String,
    val gammaChange: String,
    val vegaChange: String,
    val thetaChange: String,
    val rhoChange: String,
)
