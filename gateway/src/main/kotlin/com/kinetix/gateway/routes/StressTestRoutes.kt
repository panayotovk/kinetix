package com.kinetix.gateway.routes

import com.kinetix.gateway.client.RiskServiceClient
import com.kinetix.gateway.dtos.HistoricalReplayRequest
import com.kinetix.gateway.dtos.ReverseStressRequest
import com.kinetix.gateway.dtos.BatchStressRunResultResponse
import com.kinetix.gateway.dtos.StressTestBatchRequest
import com.kinetix.gateway.dtos.StressTestRequest
import com.kinetix.gateway.dtos.VaRCalculationRequest
import com.kinetix.gateway.dtos.toParams
import com.kinetix.gateway.dtos.toResponse
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.stressTestRoutes(client: RiskServiceClient) {
    route("/api/v1/risk/stress/{bookId}") {
        post({
            summary = "Run stress test"
            tags = listOf("Stress Tests")
            request {
                pathParameter<String>("bookId") { description = "Book identifier" }
            }
        }) {
            val bookId = call.requirePathParam("bookId")
            val request = call.receive<StressTestRequest>()
            val params = request.toParams(bookId)
            val result = client.runStressTest(params)
            if (result != null) {
                call.respond(result.toResponse())
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }

    post("/api/v1/risk/stress/{bookId}/batch", {
        summary = "Run all stress tests"
        tags = listOf("Stress Tests")
        request {
            pathParameter<String>("bookId") { description = "Book identifier" }
        }
    }) {
        val bookId = call.requirePathParam("bookId")
        val request = call.receive<StressTestBatchRequest>()
        val params = request.toParams(bookId)
        val result = client.runBatchStressTest(params)
        call.respond(result.toResponse())
    }

    get("/api/v1/risk/stress/scenarios", {
        summary = "List stress test scenarios"
        tags = listOf("Stress Tests")
    }) {
        val scenarios = client.listScenarios()
        call.respond(scenarios)
    }

    post("/api/v1/risk/stress/{bookId}/historical-replay", {
        summary = "Run historical scenario replay"
        tags = listOf("Stress Tests")
        request {
            pathParameter<String>("bookId") { description = "Book identifier" }
        }
    }) {
        val bookId = call.requirePathParam("bookId")
        val request = call.receive<HistoricalReplayRequest>()
        val params = request.toParams(bookId)
        val result = client.runHistoricalReplay(params)
        call.respond(result.toResponse())
    }

    post("/api/v1/risk/stress/{bookId}/reverse", {
        summary = "Run reverse stress test"
        tags = listOf("Stress Tests")
        request {
            pathParameter<String>("bookId") { description = "Book identifier" }
        }
    }) {
        val bookId = call.requirePathParam("bookId")
        val request = call.receive<ReverseStressRequest>()
        val params = request.toParams(bookId)
        val result = client.runReverseStress(params)
        call.respond(result.toResponse())
    }

    route("/api/v1/risk/greeks/{bookId}") {
        post({
            summary = "Calculate Greeks"
            tags = listOf("Greeks")
            request {
                pathParameter<String>("bookId") { description = "Book identifier" }
            }
        }) {
            val bookId = call.requirePathParam("bookId")
            val request = call.receive<VaRCalculationRequest>()
            val params = request.toParams(bookId).copy(
                requestedOutputs = listOf("VAR", "EXPECTED_SHORTFALL", "GREEKS")
            )
            val result = client.calculateVaR(params)
            val greeks = result?.greeks
            if (greeks != null) {
                call.respond(greeks.toResponse())
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
