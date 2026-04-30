package com.kinetix.position.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class SubmitOrderRequest(
    val bookId: String,
    val instrumentId: String,
    val side: String,
    val quantity: String,
    val orderType: String,
    val limitPrice: String? = null,
    val arrivalPrice: String,
    val fixSessionId: String? = null,
    val assetClass: String = "EQUITY",
    val currency: String = "USD",
    /**
     * Optional ISO-8601 instant when the arrival price was observed. When supplied,
     * orders are rejected with 400 if the price is stale per
     * `OrderSubmissionService.ARRIVAL_PRICE_MAX_AGE_MS`. Spec: execution.allium
     * arrival-price staleness check.
     */
    val arrivalPriceTimestamp: String? = null,
)
