package com.kinetix.risk.model

data class HedgeConstraints(
    val maxNotional: Double?,
    val maxSuggestions: Int,
    val respectPositionLimits: Boolean,
    val instrumentUniverse: String?,
    val allowedSides: List<String>?,
)
