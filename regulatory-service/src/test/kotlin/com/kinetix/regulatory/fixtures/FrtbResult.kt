package com.kinetix.regulatory.fixtures

/**
 * Test-only stub-shaped result type used by [RegulatoryReportingAcceptanceTest].
 */
internal data class FrtbResult(
    val bookId: String,
    val sbm: SbmResult,
    val drc: DrcResult,
    val rrao: RraoResult,
    val totalCapitalCharge: Double,
)
