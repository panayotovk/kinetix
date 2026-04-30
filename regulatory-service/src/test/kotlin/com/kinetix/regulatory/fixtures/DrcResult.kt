package com.kinetix.regulatory.fixtures

/**
 * Test-only stub-shaped result type used by [RegulatoryReportingAcceptanceTest].
 */
internal data class DrcResult(
    val grossJtd: Double,
    val hedgeBenefit: Double,
    val netDrc: Double,
)
