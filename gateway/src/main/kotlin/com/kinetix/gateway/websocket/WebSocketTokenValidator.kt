package com.kinetix.gateway.websocket

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.kinetix.common.security.Role
import com.kinetix.common.security.UserPrincipal
import com.kinetix.gateway.auth.JwtConfig
import io.ktor.server.application.*
import io.ktor.websocket.*

/**
 * Validates the JWT token passed as the `?token=` query parameter on a WebSocket
 * upgrade request. Returns the decoded [UserPrincipal] on success, or null if the
 * token is absent, expired, or has an invalid signature.
 */
fun ApplicationCall.validateWebSocketToken(config: JwtConfig): UserPrincipal? {
    val rawToken = request.queryParameters["token"] ?: return null
    return try {
        val verifier = JWT.require(Algorithm.HMAC256(config.secret))
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .build()
        val decoded = verifier.verify(rawToken)
        if (!decoded.audience.contains(config.audience)) return null
        val userId = decoded.subject ?: return null
        val username = decoded.getClaim("preferred_username")?.asString() ?: userId
        val rolesClaim = decoded.getClaim("roles")?.asList(String::class.java) ?: emptyList()
        val roles = rolesClaim.mapNotNull { runCatching { Role.valueOf(it) }.getOrNull() }.toSet()
        UserPrincipal(userId, username, roles)
    } catch (_: JWTVerificationException) {
        null
    }
}

/** Close code sent when a WebSocket connection is rejected due to auth failure. */
val WEBSOCKET_UNAUTHORIZED_CLOSE = CloseReason(4001, "Unauthorized")
