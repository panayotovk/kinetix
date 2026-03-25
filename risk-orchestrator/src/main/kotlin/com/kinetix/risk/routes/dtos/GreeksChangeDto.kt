package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class GreeksChangeDto(
    val deltaChange: String,
    val gammaChange: String,
    val vegaChange: String,
    val thetaChange: String,
    val rhoChange: String,
)
