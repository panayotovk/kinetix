package com.kinetix.gateway.routes

import com.kinetix.gateway.client.RiskServiceClient
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.croReportRoutes(riskClient: RiskServiceClient) {
    post("/api/v1/risk/reports/cro") {
        val report = riskClient.triggerCroReport()

        if (report == null) {
            call.respond(HttpStatusCode.ServiceUnavailable, "CRO report unavailable")
        } else {
            call.respond(report)
        }
    }
}
