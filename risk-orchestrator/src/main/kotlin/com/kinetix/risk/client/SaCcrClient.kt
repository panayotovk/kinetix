package com.kinetix.risk.client

data class SaCcrPositionInput(
    val instrumentId: String,
    val assetClass: String,
    val marketValue: Double,
    val notional: Double = 0.0,
    val currency: String = "USD",
    val payReceive: String = "PAY_FIXED",
    val maturityDate: String = "",
    val isOption: Boolean = false,
    val spotPrice: Double = 0.0,
    val strike: Double = 0.0,
    val impliedVol: Double = 0.0,
    val expiryDays: Int = 0,
    val optionType: String = "CALL",
    val quantity: Double = 1.0,
)

data class SaCcrResult(
    val nettingSetId: String,
    val counterpartyId: String,
    val replacementCost: Double,
    val pfeAddon: Double,
    val multiplier: Double,
    val ead: Double,
    val alpha: Double,
)

interface SaCcrClient {
    suspend fun calculateSaCcr(
        nettingSetId: String,
        counterpartyId: String,
        positions: List<SaCcrPositionInput>,
        collateralNet: Double = 0.0,
    ): SaCcrResult
}
