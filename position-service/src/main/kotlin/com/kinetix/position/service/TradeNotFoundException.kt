package com.kinetix.position.service

class TradeNotFoundException(
    val tradeId: String,
) : RuntimeException("Trade not found: $tradeId")
