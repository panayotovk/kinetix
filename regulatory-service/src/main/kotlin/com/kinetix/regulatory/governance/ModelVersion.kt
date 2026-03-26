package com.kinetix.regulatory.governance

import java.time.Instant
import java.time.LocalDate

data class ModelVersion(
    val id: String,
    val modelName: String,
    val version: String,
    val status: ModelVersionStatus,
    val parameters: String,
    val registeredBy: String,
    val approvedBy: String?,
    val approvedAt: Instant?,
    val createdAt: Instant,
    val modelTier: String? = null,
    val validationReportUrl: String? = null,
    val knownLimitations: String? = null,
    val approvedUseCases: String? = null,
    val nextValidationDate: LocalDate? = null,
)
