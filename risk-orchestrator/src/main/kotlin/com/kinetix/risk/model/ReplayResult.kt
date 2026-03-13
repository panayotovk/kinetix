package com.kinetix.risk.model

sealed interface ReplayResult {
    data object ManifestNotFound : ReplayResult
    data class Error(val message: String) : ReplayResult
    data class Success(
        val manifest: RunManifest,
        val replayResult: ValuationResult,
        val inputDigestMatch: Boolean,
        val originalInputDigest: String,
        val replayInputDigest: String,
    ) : ReplayResult
}
