package com.kinetix.risk.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ComputePFERequest(
    val positions: List<PFEPositionInputDto>,
    val numSimulations: Int = 0,
    val seed: Long = 0,
)
