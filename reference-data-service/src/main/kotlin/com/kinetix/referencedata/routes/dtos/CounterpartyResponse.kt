package com.kinetix.referencedata.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class CounterpartyResponse(
    val counterpartyId: String,
    val legalName: String,
    val shortName: String,
    val lei: String?,
    val ratingSp: String?,
    val ratingMoodys: String?,
    val ratingFitch: String?,
    val sector: String,
    val country: String?,
    val isFinancial: Boolean,
    val pd1y: Double?,
    val lgd: Double,
    val cdsSpreadBps: Double?,
    val createdAt: String,
    val updatedAt: String,
)
