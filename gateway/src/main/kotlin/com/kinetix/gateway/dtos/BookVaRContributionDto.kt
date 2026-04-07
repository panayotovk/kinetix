package com.kinetix.gateway.dtos

import kotlinx.serialization.Serializable

@Serializable
data class BookVaRContributionDto(
    val bookId: String,
    val varContribution: String,
    val percentageOfTotal: String,
    val standaloneVar: String,
    val diversificationBenefit: String,
    val marginalVar: String,
    val incrementalVar: String,
)
