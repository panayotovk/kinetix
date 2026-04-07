package com.kinetix.regulatory.submission.dtos

import kotlinx.serialization.Serializable

@Serializable
data class CreateSubmissionRequest(
    val reportType: String,
    val preparerId: String,
    val deadline: String,
)
