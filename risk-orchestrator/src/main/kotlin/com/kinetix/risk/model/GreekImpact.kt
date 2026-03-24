package com.kinetix.risk.model

data class GreekImpact(
    val deltaBefore: Double,
    val deltaAfter: Double,
    val gammaBefore: Double,
    val gammaAfter: Double,
    val vegaBefore: Double,
    val vegaAfter: Double,
    val thetaBefore: Double,
    val thetaAfter: Double,
    val rhoBefore: Double,
    val rhoAfter: Double,
)
