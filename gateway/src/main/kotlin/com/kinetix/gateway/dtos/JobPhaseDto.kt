package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.JobPhaseItem
import kotlinx.serialization.Serializable

@Serializable
data class JobPhaseDto(
    val name: String,
    val status: String,
    val startedAt: String,
    val completedAt: String? = null,
    val durationMs: Long? = null,
    val details: Map<String, String> = emptyMap(),
    val error: String? = null,
)

fun JobPhaseItem.toDto(): JobPhaseDto = JobPhaseDto(
    name = name,
    status = status,
    startedAt = startedAt.toString(),
    completedAt = completedAt?.toString(),
    durationMs = durationMs,
    details = details,
    error = error,
)
