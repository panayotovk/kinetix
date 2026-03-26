package com.kinetix.risk.model

import com.kinetix.common.model.AssetClass

data class GreekValues(
    val assetClass: AssetClass,
    val delta: Double,
    val gamma: Double,
    val vega: Double,
)

data class GreeksResult(
    val assetClassGreeks: List<GreekValues>,
    val theta: Double,
    val rho: Double,
)

/**
 * RMOD-02: Semantic alias for [GreeksResult] when used in a VaR sensitivity context.
 *
 * [GreeksResult] is the internal calculation type. [VaRSensitivities] names the same
 * structure at the domain level — it represents the first-order sensitivities (delta,
 * gamma, vega, theta, rho) that accompany a VaR calculation and drive VaR attribution,
 * hedging, and limit checks. Callers that work with VaR outputs should prefer this alias
 * so the intent is explicit at the call site.
 *
 * TODO(RMOD-02): When this distinction warrants its own fields (e.g. sensitivity date,
 * model version, or VaR confidence level), promote to a proper data class and migrate
 * call sites incrementally.
 */
typealias VaRSensitivities = GreeksResult
