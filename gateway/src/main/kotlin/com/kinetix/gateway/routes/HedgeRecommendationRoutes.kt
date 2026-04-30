package com.kinetix.gateway.routes

import com.kinetix.gateway.client.RiskServiceClient
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.JsonObject

fun Route.hedgeRecommendationRoutes(riskClient: RiskServiceClient) {

    post("/api/v1/risk/hedge-suggest/{bookId}") {
        val bookId = call.requirePathParam("bookId")
        val body = call.receive<JsonObject>()
        val result = riskClient.suggestHedge(bookId, body)
        call.respond(HttpStatusCode.Created, result)
    }

    get("/api/v1/risk/hedge-suggest/{bookId}") {
        val bookId = call.requirePathParam("bookId")
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
        val results = riskClient.getLatestHedgeRecommendations(bookId, limit)
        call.respond(results)
    }

    get("/api/v1/risk/hedge-suggest/{bookId}/{id}") {
        val bookId = call.requirePathParam("bookId")
        val id = call.requirePathParam("id")
        val result = riskClient.getHedgeRecommendation(bookId, id)
        if (result == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(result)
        }
    }

    post("/api/v1/risk/hedge-suggest/{bookId}/{id}/accept") {
        val bookId = call.requirePathParam("bookId")
        val id = call.requirePathParam("id")
        val body = call.receive<JsonObject>()
        val result = riskClient.acceptHedgeRecommendation(bookId, id, body)
        if (result == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(result)
        }
    }

    post("/api/v1/risk/hedge-suggest/{bookId}/{id}/reject") {
        val bookId = call.requirePathParam("bookId")
        val id = call.requirePathParam("id")
        val result = riskClient.rejectHedgeRecommendation(bookId, id)
        if (result == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(result)
        }
    }
}
