package com.kinetix.gateway.routes

import com.kinetix.gateway.client.RiskServiceClient
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.hierarchyRiskRoutes(riskClient: RiskServiceClient) {
    get("/api/v1/risk/hierarchy/{level}/{entityId}") {
        val level = call.requirePathParam("level")
        val entityId = call.requirePathParam("entityId")

        val result = riskClient.getHierarchyRisk(level, entityId)

        if (result == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(result)
        }
    }
}
