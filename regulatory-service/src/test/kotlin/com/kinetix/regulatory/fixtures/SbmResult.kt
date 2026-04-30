package com.kinetix.regulatory.fixtures

/**
 * Test-only stub-shaped result type used by [RegulatoryReportingAcceptanceTest].
 */
internal data class SbmResult(
    val riskClassCharges: List<RiskClassCharge>,
    val totalSbmCharge: Double,
)
