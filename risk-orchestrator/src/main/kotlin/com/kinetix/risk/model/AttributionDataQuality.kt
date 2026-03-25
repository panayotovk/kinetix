package com.kinetix.risk.model

/**
 * Indicates the completeness of a P&L attribution calculation.
 *
 * - [FULL_ATTRIBUTION]: all first- and second-order cross-Greek terms (vanna, volga, charm)
 *   were available and have been computed.
 * - [PRICE_ONLY]: no cross-Greek data was available; only first-order delta attribution
 *   could be computed and the unexplained residual is large.
 * - [STALE_GREEKS]: the SOD Greek snapshot was locked more than 2 hours before market open,
 *   so sensitivities may not reflect the current market regime.
 */
enum class AttributionDataQuality {
    FULL_ATTRIBUTION,
    PRICE_ONLY,
    STALE_GREEKS,
}
