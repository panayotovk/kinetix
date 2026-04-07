package com.kinetix.gateway.dtos

import com.kinetix.common.model.Money
import kotlinx.serialization.Serializable

@Serializable
data class MoneyDto(
    val amount: String,
    val currency: String,
)

fun Money.toDto(): MoneyDto = MoneyDto(
    amount = amount.toPlainString(),
    currency = currency.currencyCode,
)
