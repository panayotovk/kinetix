package com.kinetix.gateway.routes

import com.kinetix.gateway.dto.DataQualityCheckResponse
import com.kinetix.gateway.dto.DataQualityStatusResponse
import io.github.smiley4.ktoropenapi.get
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

@Serializable
private data class FIXSessionDto(
    val sessionId: String,
    val counterparty: String,
    val status: String,
    val lastMessageAt: String? = null,
    val inboundSeqNum: Int,
    val outboundSeqNum: Int,
)

fun Route.dataQualityRoutes(httpClient: HttpClient? = null, positionUrl: String? = null) {
    route("/api/v1/data-quality") {
        get("/status", {
            summary = "Get data quality status"
            tags = listOf("Data Quality")
        }) {
            val now = Instant.now().toString()

            val fixSessionCheck = buildFixSessionCheck(httpClient, positionUrl, now)

            val checks = listOf(
                DataQualityCheckResponse(
                    name = "Price Freshness",
                    status = "UNKNOWN",
                    message = "not implemented",
                    lastChecked = now,
                ),
                DataQualityCheckResponse(
                    name = "Position Count",
                    status = "UNKNOWN",
                    message = "not implemented",
                    lastChecked = now,
                ),
                DataQualityCheckResponse(
                    name = "Risk Result Completeness",
                    status = "UNKNOWN",
                    message = "not implemented",
                    lastChecked = now,
                ),
                fixSessionCheck,
            )

            val overall = when {
                checks.any { it.status == "CRITICAL" } -> "CRITICAL"
                checks.any { it.status == "WARNING" } -> "WARNING"
                checks.all { it.status == "UNKNOWN" } -> "UNKNOWN"
                else -> "OK"
            }

            call.respond(DataQualityStatusResponse(overall = overall, checks = checks))
        }
    }
}

private val lenientJson = Json { ignoreUnknownKeys = true }

private suspend fun buildFixSessionCheck(
    httpClient: HttpClient?,
    positionUrl: String?,
    now: String,
): DataQualityCheckResponse {
    if (httpClient == null || positionUrl == null) {
        return DataQualityCheckResponse(
            name = "FIX Connectivity",
            status = "UNKNOWN",
            message = "not configured",
            lastChecked = now,
        )
    }

    return try {
        val response = httpClient.get("$positionUrl/api/v1/fix/sessions")
        if (!response.status.isSuccess()) {
            return DataQualityCheckResponse(
                name = "FIX Connectivity",
                status = "CRITICAL",
                message = "Failed to fetch FIX session status: ${response.status.value}",
                lastChecked = now,
            )
        }
        val sessions = lenientJson.decodeFromString<List<FIXSessionDto>>(response.body())
        buildFixCheckFromSessions(sessions, now)
    } catch (e: Exception) {
        DataQualityCheckResponse(
            name = "FIX Connectivity",
            status = "CRITICAL",
            message = "Cannot reach position-service for FIX session status",
            lastChecked = now,
        )
    }
}

private fun buildFixCheckFromSessions(sessions: List<FIXSessionDto>, now: String): DataQualityCheckResponse {
    if (sessions.isEmpty()) {
        return DataQualityCheckResponse(
            name = "FIX Connectivity",
            status = "WARNING",
            message = "No FIX sessions configured",
            lastChecked = now,
        )
    }

    val disconnected = sessions.filter { it.status == "DISCONNECTED" }
    val reconnecting = sessions.filter { it.status == "RECONNECTING" }
    val connected = sessions.filter { it.status == "CONNECTED" }

    return when {
        disconnected.size == sessions.size -> DataQualityCheckResponse(
            name = "FIX Connectivity",
            status = "CRITICAL",
            message = "All ${sessions.size} FIX session(s) are DISCONNECTED",
            lastChecked = now,
        )
        disconnected.isNotEmpty() || reconnecting.isNotEmpty() -> DataQualityCheckResponse(
            name = "FIX Connectivity",
            status = "WARNING",
            message = "${connected.size}/${sessions.size} session(s) connected; " +
                "${disconnected.size} disconnected, ${reconnecting.size} reconnecting",
            lastChecked = now,
        )
        else -> DataQualityCheckResponse(
            name = "FIX Connectivity",
            status = "OK",
            message = "All ${sessions.size} FIX session(s) connected",
            lastChecked = now,
        )
    }
}
