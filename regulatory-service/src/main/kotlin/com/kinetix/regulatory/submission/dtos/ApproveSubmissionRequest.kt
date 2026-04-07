package com.kinetix.regulatory.submission.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ApproveSubmissionRequest(
    val approverId: String,
)
