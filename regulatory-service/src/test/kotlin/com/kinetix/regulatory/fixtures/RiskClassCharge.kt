package com.kinetix.regulatory.fixtures

/**
 * Test-only stub-shaped result type used by [RegulatoryReportingAcceptanceTest].
 *
 * Distinct from the production `com.kinetix.regulatory.model.RiskClassCharge`,
 * which uses [java.math.BigDecimal] amounts and a [String] risk-class identifier.
 */
internal data class RiskClassCharge(
    val riskClass: FrtbRiskClass,
    val deltaCharge: Double,
    val vegaCharge: Double,
    val curvatureCharge: Double,
    val totalCharge: Double,
)
