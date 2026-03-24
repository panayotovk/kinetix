package com.kinetix.position.routes.dtos

import kotlinx.serialization.Serializable

@Serializable
data class FIXSessionResponse(
    val sessionId: String,
    val counterparty: String,
    val status: String,
    val lastMessageAt: String?,
    val inboundSeqNum: Int,
    val outboundSeqNum: Int,
)
