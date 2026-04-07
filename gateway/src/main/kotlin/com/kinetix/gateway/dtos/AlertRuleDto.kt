package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.AlertRuleItem
import kotlinx.serialization.Serializable

@Serializable
data class AlertRuleDto(
    val id: String,
    val name: String,
    val type: String,
    val threshold: Double,
    val operator: String,
    val severity: String,
    val channels: List<String>,
    val enabled: Boolean,
)

fun AlertRuleItem.toDto(): AlertRuleDto = AlertRuleDto(
    id = id,
    name = name,
    type = type,
    threshold = threshold,
    operator = operator,
    severity = severity,
    channels = channels,
    enabled = enabled,
)
