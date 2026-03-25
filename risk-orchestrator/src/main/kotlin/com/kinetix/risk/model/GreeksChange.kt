package com.kinetix.risk.model

data class GreeksChange(
    val deltaChange: Double,
    val gammaChange: Double,
    val vegaChange: Double,
    val thetaChange: Double,
    val rhoChange: Double,
)
