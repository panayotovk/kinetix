package com.kinetix.regulatory.fixtures

/**
 * Test-only stub enum used by [RegulatoryReportingAcceptanceTest] to exercise
 * the FRTB report-generation flow against a deterministic stub calculator.
 *
 * Not a production type: production FRTB risk-class identifiers are represented
 * as [String] in `com.kinetix.regulatory.model.RiskClassCharge`.
 */
internal enum class FrtbRiskClass(val weight: Double, val vegaWeight: Double) {
    GIRR(0.015, 0.01),
    CSR_NON_SEC(0.03, 0.02),
    CSR_SEC_CTP(0.04, 0.03),
    CSR_SEC_NON_CTP(0.06, 0.04),
    EQUITY(0.20, 0.15),
    COMMODITY(0.15, 0.10),
    FX(0.10, 0.08),
}
