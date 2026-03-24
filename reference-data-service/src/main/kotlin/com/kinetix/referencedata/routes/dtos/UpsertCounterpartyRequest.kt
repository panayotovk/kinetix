package com.kinetix.referencedata.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class UpsertCounterpartyRequest(
    val counterpartyId: String,
    val legalName: String,
    val shortName: String,
    val lei: String? = null,
    val ratingSp: String? = null,
    val ratingMoodys: String? = null,
    val ratingFitch: String? = null,
    val sector: String = "OTHER",
    val country: String? = null,
    val isFinancial: Boolean = false,
    val pd1y: Double? = null,
    val lgd: Double = 0.40,
    val cdsSpreadBps: Double? = null,
)
