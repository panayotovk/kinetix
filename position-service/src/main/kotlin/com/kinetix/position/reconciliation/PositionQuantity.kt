package com.kinetix.position.reconciliation

import java.math.BigDecimal

data class PositionQuantity(
    val portfolioId: String,
    val instrumentId: String,
    val quantity: BigDecimal,
)
