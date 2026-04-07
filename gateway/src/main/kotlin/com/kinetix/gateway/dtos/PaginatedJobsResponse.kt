package com.kinetix.gateway.dtos

import kotlinx.serialization.Serializable

@Serializable
data class PaginatedJobsResponse(
    val items: List<ValuationJobSummaryResponse>,
    val totalCount: Long,
)
