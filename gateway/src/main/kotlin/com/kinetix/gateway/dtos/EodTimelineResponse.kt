package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.EodTimelineSummary
import kotlinx.serialization.Serializable

@Serializable
data class EodTimelineResponse(
    val bookId: String,
    val from: String,
    val to: String,
    val entries: List<EodTimelineEntryResponse>,
)

fun EodTimelineSummary.toResponse() = EodTimelineResponse(
    bookId = bookId,
    from = from,
    to = to,
    entries = entries.map { it.toResponse() },
)
