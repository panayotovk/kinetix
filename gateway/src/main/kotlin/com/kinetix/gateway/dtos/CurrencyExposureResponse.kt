package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.CurrencyExposureSummary
import kotlinx.serialization.Serializable

@Serializable
data class CurrencyExposureResponse(
    val currency: String,
    val localValue: MoneyDto,
    val baseValue: MoneyDto,
    val fxRate: String,
)

fun CurrencyExposureSummary.toResponse(): CurrencyExposureResponse = CurrencyExposureResponse(
    currency = currency,
    localValue = localValue.toDto(),
    baseValue = baseValue.toDto(),
    fxRate = fxRate.toPlainString(),
)
