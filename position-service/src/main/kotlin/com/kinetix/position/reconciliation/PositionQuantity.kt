package com.kinetix.position.reconciliation

import java.math.BigDecimal

data class PositionQuantity(
    val bookId: String,
    val instrumentId: String,
    val quantity: BigDecimal,
)
