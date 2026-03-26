package com.kinetix.regulatory.governance.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterModelRequest(
    val modelName: String,
    val version: String,
    val parameters: String,
    val registeredBy: String,
    val modelTier: String? = null,
    val validationReportUrl: String? = null,
    val knownLimitations: String? = null,
    val approvedUseCases: String? = null,
    val nextValidationDate: String? = null,
)
