package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.SodBaselineStatusSummary
import kotlinx.serialization.Serializable

@Serializable
data class SodBaselineStatusResponse(
    val exists: Boolean,
    val baselineDate: String? = null,
    val snapshotType: String? = null,
    val createdAt: String? = null,
    val sourceJobId: String? = null,
    val calculationType: String? = null,
)

fun SodBaselineStatusSummary.toResponse(): SodBaselineStatusResponse = SodBaselineStatusResponse(
    exists = exists,
    baselineDate = baselineDate,
    snapshotType = snapshotType,
    createdAt = createdAt,
    sourceJobId = sourceJobId,
    calculationType = calculationType,
)
