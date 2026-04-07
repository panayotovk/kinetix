package com.kinetix.gateway.dtos

import com.kinetix.gateway.client.CreateAlertRuleParams
import kotlinx.serialization.Serializable

@Serializable
data class CreateAlertRuleRequest(
    val name: String,
    val type: String,
    val threshold: Double,
    val operator: String,
    val severity: String,
    val channels: List<String>,
)

fun CreateAlertRuleRequest.toParams(): CreateAlertRuleParams = CreateAlertRuleParams(
    name = name,
    type = type,
    threshold = threshold,
    operator = operator,
    severity = severity,
    channels = channels,
)
