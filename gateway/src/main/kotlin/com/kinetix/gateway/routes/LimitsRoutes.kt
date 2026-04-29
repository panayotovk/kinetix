package com.kinetix.gateway.routes

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*

/**
 * Gateway proxy routes for the position-service limits hierarchy.
 *
 * Limits CRUD lives in position-service (`/api/v1/limits`) and is accessed
 * by traders, risk officers, and the limit-management UI. The gateway forwards
 * requests verbatim — no DTO transformation needed.
 */
fun Route.limitsRoutes(httpClient: HttpClient, positionUrl: String) {
    get("/api/v1/limits") {
        proxyLimitsCall(httpClient, "$positionUrl/api/v1/limits", call)
    }

    post("/api/v1/limits") {
        proxyLimitsCall(httpClient, "$positionUrl/api/v1/limits", call)
    }

    put("/api/v1/limits/{id}") {
        val id = call.requirePathParam("id")
        proxyLimitsCall(httpClient, "$positionUrl/api/v1/limits/$id", call)
    }

    post("/api/v1/limits/{id}/temporary-increase") {
        val id = call.requirePathParam("id")
        proxyLimitsCall(httpClient, "$positionUrl/api/v1/limits/$id/temporary-increase", call)
    }
}

private suspend fun proxyLimitsCall(
    httpClient: HttpClient,
    upstreamUrl: String,
    call: io.ktor.server.application.ApplicationCall,
) {
    val method = call.request.httpMethod
    val requestBody: ByteArray? = if (method == HttpMethod.Post || method == HttpMethod.Put) {
        call.receiveChannel().toByteArray()
    } else null

    val response = httpClient.request(upstreamUrl) {
        this.method = method
        if (requestBody != null) {
            contentType(call.request.contentType())
            setBody(requestBody)
        }
        call.request.headers.forEach { name, values ->
            if (name !in setOf(HttpHeaders.Host, HttpHeaders.ContentLength)) {
                values.forEach { value -> header(name, value) }
            }
        }
    }

    call.respondBytes(
        bytes = response.readRawBytes(),
        contentType = response.contentType(),
        status = response.status,
    )
}
