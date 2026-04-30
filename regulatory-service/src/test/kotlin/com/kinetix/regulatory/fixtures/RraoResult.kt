package com.kinetix.regulatory.fixtures

/**
 * Test-only stub-shaped result type used by [RegulatoryReportingAcceptanceTest].
 */
internal data class RraoResult(
    val exoticNotional: Double,
    val otherNotional: Double,
    val totalRrao: Double,
)
