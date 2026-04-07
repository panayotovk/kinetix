package com.kinetix.regulatory.historical

import com.kinetix.regulatory.historical.dtos.HistoricalScenarioPeriodResponse
import com.kinetix.regulatory.historical.dtos.ReplayRequest
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.historicalScenarioPeriodRoutes(
    repository: HistoricalScenarioRepository,
    replayService: HistoricalReplayService? = null,
) {
    route("/api/v1/historical-periods") {
        get({
            summary = "List all historical scenario periods"
            tags = listOf("Historical Scenarios")
        }) {
            val periods = repository.findAllPeriods()
            call.respond(periods.map { it.toResponse() })
        }

        get("/{periodId}", {
            summary = "Get a historical scenario period by ID"
            tags = listOf("Historical Scenarios")
            request {
                pathParameter<String>("periodId") { description = "Period identifier" }
            }
        }) {
            val periodId = call.parameters["periodId"]
                ?: throw IllegalArgumentException("Missing required path parameter: periodId")
            val period = repository.findPeriodById(periodId)
            if (period == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(period.toResponse())
            }
        }

        post("/{periodId}/replay", {
            summary = "Run a historical scenario replay against a live book"
            tags = listOf("Historical Scenarios")
            request {
                pathParameter<String>("periodId") { description = "Period identifier" }
                body<ReplayRequest>()
            }
        }) {
            val periodId = call.parameters["periodId"]
                ?: throw IllegalArgumentException("Missing required path parameter: periodId")
            val service = replayService
                ?: throw IllegalStateException("HistoricalReplayService not configured")
            val request = call.receive<ReplayRequest>()
            val result = service.runReplay(periodId, request)
            call.respond(HttpStatusCode.OK, result)
        }
    }
}

private fun HistoricalScenarioPeriod.toResponse() = HistoricalScenarioPeriodResponse(
    periodId = periodId,
    name = name,
    description = description,
    startDate = startDate,
    endDate = endDate,
    assetClassFocus = assetClassFocus,
    severityLabel = severityLabel,
)
