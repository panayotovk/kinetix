package com.kinetix.risk.service

import com.kinetix.common.model.BookId
import com.kinetix.risk.model.AttributionDataQuality
import com.kinetix.risk.model.PnlAttribution
import com.kinetix.risk.model.PositionPnlAttribution
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate

class PnlAttributionService {

    private val tradingDaysPerYear = BigDecimal(252)
    private val half = BigDecimal("0.5")
    private val mc = MathContext(20, RoundingMode.HALF_UP)

    fun attribute(
        bookId: BookId,
        positions: List<PositionPnlInput>,
        date: LocalDate = LocalDate.now(),
    ): PnlAttribution {
        val dt = BigDecimal.ONE.divide(tradingDaysPerYear, mc)

        val positionAttributions = positions.map { pos ->
            // First-order Taylor terms
            val deltaPnl = pos.delta.multiply(pos.priceChange, mc)
            val gammaPnl = half.multiply(pos.gamma, mc).multiply(pos.priceChange.pow(2), mc)
            val vegaPnl = pos.vega.multiply(pos.volChange, mc)
            val thetaPnl = pos.theta.multiply(dt, mc)
            val rhoPnl = pos.rho.multiply(pos.rateChange, mc)

            // Cross-Greek (second-order mixed) terms
            // vanna_pnl = vanna * dS * dvol
            val vannaPnl = pos.vanna.multiply(pos.priceChange, mc).multiply(pos.volChange, mc)
            // volga_pnl = 0.5 * volga * dvol^2
            val volgaPnl = half.multiply(pos.volga, mc).multiply(pos.volChange.pow(2), mc)
            // charm_pnl = charm * dS * dT  (decay of delta per unit time per unit price move)
            val charmPnl = pos.charm.multiply(pos.priceChange, mc).multiply(dt, mc)
            // cross_gamma_pnl = 0 for single-asset books (multi-asset cross-gamma deferred)
            val crossGammaPnl = BigDecimal.ZERO

            val explained = deltaPnl + gammaPnl + vegaPnl + thetaPnl + rhoPnl +
                vannaPnl + volgaPnl + charmPnl + crossGammaPnl
            val unexplainedPnl = pos.totalPnl.subtract(explained, mc)

            PositionPnlAttribution(
                instrumentId = pos.instrumentId,
                assetClass = pos.assetClass,
                totalPnl = pos.totalPnl,
                deltaPnl = deltaPnl,
                gammaPnl = gammaPnl,
                vegaPnl = vegaPnl,
                thetaPnl = thetaPnl,
                rhoPnl = rhoPnl,
                vannaPnl = vannaPnl,
                volgaPnl = volgaPnl,
                charmPnl = charmPnl,
                crossGammaPnl = crossGammaPnl,
                unexplainedPnl = unexplainedPnl,
            )
        }

        val totalPnl = positionAttributions.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.totalPnl, mc) }
        val deltaPnl = positionAttributions.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.deltaPnl, mc) }
        val gammaPnl = positionAttributions.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.gammaPnl, mc) }
        val vegaPnl = positionAttributions.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.vegaPnl, mc) }
        val thetaPnl = positionAttributions.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.thetaPnl, mc) }
        val rhoPnl = positionAttributions.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.rhoPnl, mc) }
        val vannaPnl = positionAttributions.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.vannaPnl, mc) }
        val volgaPnl = positionAttributions.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.volgaPnl, mc) }
        val charmPnl = positionAttributions.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.charmPnl, mc) }
        val crossGammaPnl = positionAttributions.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.crossGammaPnl, mc) }
        val unexplainedPnl = positionAttributions.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.unexplainedPnl, mc) }

        val dataQualityFlag = deriveDataQuality(positions)

        return PnlAttribution(
            bookId = bookId,
            date = date,
            totalPnl = totalPnl,
            deltaPnl = deltaPnl,
            gammaPnl = gammaPnl,
            vegaPnl = vegaPnl,
            thetaPnl = thetaPnl,
            rhoPnl = rhoPnl,
            vannaPnl = vannaPnl,
            volgaPnl = volgaPnl,
            charmPnl = charmPnl,
            crossGammaPnl = crossGammaPnl,
            unexplainedPnl = unexplainedPnl,
            positionAttributions = positionAttributions,
            dataQualityFlag = dataQualityFlag,
            calculatedAt = Instant.now(),
        )
    }

    /**
     * Derives the data quality flag from the set of positions.
     *
     * FULL_ATTRIBUTION: at least one position carries non-zero vanna or volga or charm —
     * meaning pricing Greeks were available and cross-Greek terms have been computed.
     *
     * PRICE_ONLY: every position has zero vanna, volga, and charm — no cross-Greek snapshot
     * was available, so the attribution only covers first-order terms.
     *
     * STALE_GREEKS is set externally by the calling service when it detects the SOD snapshot
     * was locked too long before market open; this method never returns it.
     */
    private fun deriveDataQuality(positions: List<PositionPnlInput>): AttributionDataQuality {
        val hasCrossGreeks = positions.any { pos ->
            pos.vanna.compareTo(BigDecimal.ZERO) != 0 ||
                pos.volga.compareTo(BigDecimal.ZERO) != 0 ||
                pos.charm.compareTo(BigDecimal.ZERO) != 0
        }
        return if (hasCrossGreeks) AttributionDataQuality.FULL_ATTRIBUTION else AttributionDataQuality.PRICE_ONLY
    }
}
