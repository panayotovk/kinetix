package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.AssetClassImpactItem
import kotlinx.serialization.Serializable

@Serializable
data class AssetClassImpactDto(
    val assetClass: String,
    val baseExposure: String,
    val stressedExposure: String,
    val pnlImpact: String,
)

fun AssetClassImpactItem.toDto(): AssetClassImpactDto = AssetClassImpactDto(
    assetClass = assetClass,
    baseExposure = "%.2f".format(baseExposure),
    stressedExposure = "%.2f".format(stressedExposure),
    pnlImpact = "%.2f".format(pnlImpact),
)
