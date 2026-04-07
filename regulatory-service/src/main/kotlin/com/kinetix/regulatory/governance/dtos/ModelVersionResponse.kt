package com.kinetix.regulatory.governance.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ModelVersionResponse(
    val id: String,
    val modelName: String,
    val version: String,
    val status: String,
    val parameters: String,
    val registeredBy: String,
    val approvedBy: String?,
    val approvedAt: String?,
    val createdAt: String,
    val modelTier: String? = null,
    val validationReportUrl: String? = null,
    val knownLimitations: String? = null,
    val approvedUseCases: String? = null,
    val nextValidationDate: String? = null,
)
