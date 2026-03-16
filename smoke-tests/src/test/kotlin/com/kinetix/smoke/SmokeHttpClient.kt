package com.kinetix.smoke

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.util.UUID

object SmokeHttpClient {

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun create(): HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        engine {
            requestTimeout = SmokeTestConfig.timeoutMs
        }
    }

    fun correlationId(scenario: String): String =
        "smoke-$scenario-${UUID.randomUUID().toString().substring(0, 8)}"

    suspend fun HttpClient.smokeGet(path: String, scenario: String, token: String? = null) =
        get("${SmokeTestConfig.baseUrl}$path") {
            header("X-Correlation-ID", correlationId(scenario))
            if (token != null) header("Authorization", "Bearer $token")
        }

    suspend fun HttpClient.smokePost(path: String, scenario: String, body: String, token: String? = null) =
        post("${SmokeTestConfig.baseUrl}$path") {
            header("X-Correlation-ID", correlationId(scenario))
            header("Content-Type", "application/json")
            if (token != null) header("Authorization", "Bearer $token")
            setBody(body)
        }

    suspend fun HttpClient.smokePut(path: String, scenario: String, body: String, token: String? = null) =
        put("${SmokeTestConfig.baseUrl}$path") {
            header("X-Correlation-ID", correlationId(scenario))
            header("Content-Type", "application/json")
            if (token != null) header("Authorization", "Bearer $token")
            setBody(body)
        }
}
