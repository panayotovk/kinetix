package com.kinetix.regulatory.submission.dtos

import kotlinx.serialization.Serializable

/**
 * Body for `PATCH /api/v1/submissions/{id}/acknowledge`.
 *
 * The regulator confirms receipt of a submitted report at a specific time;
 * that timestamp must be persisted verbatim, not stamped from the receiving
 * service's clock. Spec: regulatory.allium AcknowledgeSubmission (354-356).
 */
@Serializable
data class AcknowledgeSubmissionRequest(
    val acknowledgedAt: String,
)
