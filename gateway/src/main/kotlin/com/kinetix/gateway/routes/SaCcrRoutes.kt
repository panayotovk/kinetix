package com.kinetix.gateway.routes

import com.kinetix.gateway.client.RiskServiceClient
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.saCcrRoutes(riskClient: RiskServiceClient) {

    get("/api/v1/counterparty/{counterpartyId}/sa-ccr") {
        val counterpartyId = call.requirePathParam("counterpartyId")
        val collateral = call.request.queryParameters["collateral"]?.toDoubleOrNull() ?: 0.0
        val result = riskClient.getCounterpartySaCcr(counterpartyId, collateral)
        if (result == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(result)
        }
    }
}
