package com.kinetix.risk.model

import java.math.BigDecimal

data class PositionInputChange(
    val instrumentId: String,
    val assetClass: String,
    val changeType: PositionInputChangeType,
    val baseQuantity: BigDecimal?,
    val targetQuantity: BigDecimal?,
    val quantityDelta: BigDecimal?,
    val baseMarketPrice: BigDecimal?,
    val targetMarketPrice: BigDecimal?,
    val priceDelta: BigDecimal?,
    val currency: String,
)
