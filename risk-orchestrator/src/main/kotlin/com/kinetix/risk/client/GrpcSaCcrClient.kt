package com.kinetix.risk.client

import com.kinetix.proto.risk.CalculateSaCcrRequest
import com.kinetix.proto.risk.SaCcrPositionInput as ProtoSaCcrPositionInput
import com.kinetix.proto.risk.SaCcrServiceGrpcKt.SaCcrServiceCoroutineStub
import java.util.concurrent.TimeUnit

class GrpcSaCcrClient(
    private val stub: SaCcrServiceCoroutineStub,
    private val deadlineMs: Long = 60_000,
) : SaCcrClient {

    override suspend fun calculateSaCcr(
        nettingSetId: String,
        counterpartyId: String,
        positions: List<SaCcrPositionInput>,
        collateralNet: Double,
    ): SaCcrResult {
        val request = CalculateSaCcrRequest.newBuilder()
            .setNettingSetId(nettingSetId)
            .setCounterpartyId(counterpartyId)
            .addAllPositions(positions.map { it.toProto() })
            .setCollateralNet(collateralNet)
            .build()

        val response = stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
            .calculateSaCcr(request)

        return SaCcrResult(
            nettingSetId = response.nettingSetId,
            counterpartyId = response.counterpartyId,
            replacementCost = response.replacementCost,
            pfeAddon = response.pfeAddon,
            multiplier = response.multiplier,
            ead = response.ead,
            alpha = response.alpha,
        )
    }
}

private fun SaCcrPositionInput.toProto(): ProtoSaCcrPositionInput =
    ProtoSaCcrPositionInput.newBuilder()
        .setInstrumentId(instrumentId)
        .setAssetClass(assetClass)
        .setMarketValue(marketValue)
        .setNotional(notional)
        .setCurrency(currency)
        .setPayReceive(payReceive)
        .setMaturityDate(maturityDate)
        .setIsOption(isOption)
        .setSpotPrice(spotPrice)
        .setStrike(strike)
        .setImpliedVol(impliedVol)
        .setExpiryDays(expiryDays)
        .setOptionType(optionType)
        .setQuantity(quantity)
        .build()
