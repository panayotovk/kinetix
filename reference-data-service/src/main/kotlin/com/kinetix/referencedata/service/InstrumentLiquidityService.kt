package com.kinetix.referencedata.service

import com.kinetix.common.model.LiquidityTier
import com.kinetix.referencedata.model.InstrumentLiquidity
import com.kinetix.referencedata.persistence.InstrumentLiquidityRepository
import java.time.Instant

private const val ADV_MAX_STALENESS_DAYS = 2L

private const val HIGH_LIQUID_ADV_FLOOR = 50_000_000.0
private const val HIGH_LIQUID_SPREAD_CAP = 5.0
private const val LIQUID_ADV_FLOOR = 10_000_000.0
private const val LIQUID_SPREAD_CAP = 20.0
private const val SEMI_LIQUID_ADV_FLOOR = 1_000_000.0

class InstrumentLiquidityService(
    private val repository: InstrumentLiquidityRepository,
) {

    companion object {
        /**
         * Classifies an instrument into a liquidity tier based on ADV and bid-ask spread.
         *
         * HIGH_LIQUID: adv >= 50M and spread <= 5bps
         * LIQUID:      adv >= 10M and spread <= 20bps
         * SEMI_LIQUID: adv >= 1M
         * ILLIQUID:    everything else
         *
         * Matches specs/liquidity.allium:210-214 and specs/core.allium:143.
         */
        fun classifyTier(adv: Double, bidAskSpreadBps: Double): LiquidityTier = when {
            adv >= HIGH_LIQUID_ADV_FLOOR && bidAskSpreadBps <= HIGH_LIQUID_SPREAD_CAP -> LiquidityTier.HIGH_LIQUID
            adv >= LIQUID_ADV_FLOOR && bidAskSpreadBps <= LIQUID_SPREAD_CAP -> LiquidityTier.LIQUID
            adv >= SEMI_LIQUID_ADV_FLOOR -> LiquidityTier.SEMI_LIQUID
            else -> LiquidityTier.ILLIQUID
        }
    }

    suspend fun findById(instrumentId: String): InstrumentLiquidity? =
        repository.findById(instrumentId)

    suspend fun findByIds(instrumentIds: List<String>): List<InstrumentLiquidity> =
        repository.findByIds(instrumentIds)

    suspend fun upsert(liquidity: InstrumentLiquidity) {
        repository.upsert(liquidity)
    }

    suspend fun findAll(): List<InstrumentLiquidity> = repository.findAll()

    fun isStale(liquidity: InstrumentLiquidity, now: Instant = Instant.now()): Boolean {
        val ageSeconds = now.epochSecond - liquidity.advUpdatedAt.epochSecond
        return ageSeconds > ADV_MAX_STALENESS_DAYS * 24 * 3600
    }

    fun staleDays(liquidity: InstrumentLiquidity, now: Instant = Instant.now()): Int {
        val ageSeconds = now.epochSecond - liquidity.advUpdatedAt.epochSecond
        return (ageSeconds / (24 * 3600)).toInt()
    }
}
