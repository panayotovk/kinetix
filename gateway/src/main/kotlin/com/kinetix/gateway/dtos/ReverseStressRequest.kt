package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.ReverseStressParams
import kotlinx.serialization.Serializable

@Serializable
data class ReverseStressRequest(
    val targetLoss: Double,
    val maxShock: Double = -1.0,
)

fun ReverseStressRequest.toParams(bookId: String): ReverseStressParams = ReverseStressParams(
    bookId = bookId,
    targetLoss = targetLoss,
    maxShock = maxShock,
)
