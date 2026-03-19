package com.kinetix.position.reconciliation

interface ReconciliationRepository {
    suspend fun findTradeQuantityByPosition(): List<PositionQuantity>
    suspend fun findCurrentPositionQuantities(): List<PositionQuantity>
}
