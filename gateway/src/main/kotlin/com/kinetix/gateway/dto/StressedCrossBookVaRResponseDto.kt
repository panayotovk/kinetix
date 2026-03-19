package com.kinetix.gateway.dto

import com.kinetix.gateway.client.StressedCrossBookVaRResultSummary
import kotlinx.serialization.Serializable

@Serializable
data class StressedCrossBookVaRResponseDto(
    val baseVaR: String,
    val stressedVaR: String,
    val baseDiversificationBenefit: String,
    val stressedDiversificationBenefit: String,
    val benefitErosion: String,
    val benefitErosionPct: String,
    val stressCorrelation: String,
)

fun StressedCrossBookVaRResultSummary.toResponse(): StressedCrossBookVaRResponseDto =
    StressedCrossBookVaRResponseDto(
        baseVaR = baseVaR,
        stressedVaR = stressedVaR,
        baseDiversificationBenefit = baseDiversificationBenefit,
        stressedDiversificationBenefit = stressedDiversificationBenefit,
        benefitErosion = benefitErosion,
        benefitErosionPct = benefitErosionPct,
        stressCorrelation = stressCorrelation,
    )
