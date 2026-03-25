package com.kinetix.gateway.routes

import com.kinetix.gateway.client.RiskServiceClient
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.benchmarkAttributionRoutes(riskClient: RiskServiceClient) {

    get("/api/v1/books/{bookId}/attribution") {
        val bookId = call.requirePathParam("bookId")
        val benchmarkId = call.request.queryParameters["benchmarkId"]
            ?: run {
                call.respond(HttpStatusCode.BadRequest, "Query parameter 'benchmarkId' is required")
                return@get
            }
        val asOfDate = call.request.queryParameters["asOfDate"]

        val result = riskClient.getBrinsonAttribution(bookId, benchmarkId, asOfDate)
        if (result == null) {
            call.respond(HttpStatusCode.BadRequest, "Attribution could not be computed")
        } else {
            call.respond(result)
        }
    }
}
