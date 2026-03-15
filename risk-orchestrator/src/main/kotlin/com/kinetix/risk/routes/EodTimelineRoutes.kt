package com.kinetix.risk.routes

import com.kinetix.risk.model.ValuationJob
import com.kinetix.risk.routes.dtos.EodTimelineEntryDto
import com.kinetix.risk.routes.dtos.EodTimelineResponse
import com.kinetix.risk.service.ValuationJobRecorder
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val logger = LoggerFactory.getLogger("EodTimelineRoutes")

private const val MAX_RANGE_DAYS = 366L

fun Route.eodTimelineRoutes(jobRecorder: ValuationJobRecorder) {

    get("/api/v1/risk/eod-timeline/{portfolioId}", {
        summary = "Get EOD timeline for a portfolio"
        tags = listOf("EOD Timeline")
        request {
            pathParameter<String>("portfolioId") { description = "Portfolio identifier" }
            queryParameter<String>("from") {
                description = "Start date inclusive (YYYY-MM-DD)"
                required = true
            }
            queryParameter<String>("to") {
                description = "End date inclusive (YYYY-MM-DD)"
                required = true
            }
        }
    }) {
        val portfolioId = call.requirePathParam("portfolioId")

        val fromStr = call.request.queryParameters["from"]
        val toStr = call.request.queryParameters["to"]

        if (fromStr.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "from parameter is required"))
            return@get
        }
        if (toStr.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "to parameter is required"))
            return@get
        }

        val from = try {
            LocalDate.parse(fromStr)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid from date format. Expected YYYY-MM-DD."))
            return@get
        }

        val to = try {
            LocalDate.parse(toStr)
        } catch (_: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid to date format. Expected YYYY-MM-DD."))
            return@get
        }

        if (from.isAfter(to)) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "from must not be after to"))
            return@get
        }

        val rangeDays = ChronoUnit.DAYS.between(from, to)
        if (rangeDays > MAX_RANGE_DAYS) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Date range must not exceed $MAX_RANGE_DAYS days"))
            return@get
        }

        val startMs = System.currentTimeMillis()
        logger.info("eod_timeline_query portfolio_id={} from={} to={}", portfolioId, from, to)

        val jobs = jobRecorder.findOfficialEodRange(portfolioId, from, to)
        val entries = computeTimeline(jobs)

        val durationMs = System.currentTimeMillis() - startMs
        logger.info("eod_timeline_result dates_found={} duration_ms={}", entries.size, durationMs)
        if (durationMs > 2_000) {
            logger.warn("eod_timeline_slow portfolio_id={} duration_ms={}", portfolioId, durationMs)
        }

        call.respond(
            EodTimelineResponse(
                portfolioId = portfolioId,
                from = from.toString(),
                to = to.toString(),
                entries = entries,
            )
        )
    }
}

private fun computeTimeline(jobs: List<ValuationJob>): List<EodTimelineEntryDto> {
    // jobs arrive ASC by valuationDate from the repository
    val entries = mutableListOf<EodTimelineEntryDto>()
    var prevVarValue: Double? = null
    var prevEs: Double? = null

    for (job in jobs) {
        val varChange = if (prevVarValue != null && job.varValue != null) {
            job.varValue - prevVarValue
        } else null

        val varChangePct = if (prevVarValue != null && prevVarValue != 0.0 && job.varValue != null) {
            ((job.varValue - prevVarValue) / prevVarValue) * 100.0
        } else null

        val esChange = if (prevEs != null && job.expectedShortfall != null) {
            job.expectedShortfall - prevEs
        } else null

        entries.add(
            EodTimelineEntryDto(
                valuationDate = job.valuationDate.toString(),
                jobId = job.jobId.toString(),
                varValue = job.varValue,
                expectedShortfall = job.expectedShortfall,
                pvValue = job.pvValue,
                delta = job.delta,
                gamma = job.gamma,
                vega = job.vega,
                theta = job.theta,
                rho = job.rho,
                promotedAt = job.promotedAt?.toString(),
                promotedBy = job.promotedBy,
                varChange = varChange,
                varChangePct = varChangePct,
                esChange = esChange,
                calculationType = job.calculationType,
                confidenceLevel = parseConfidenceLevel(job.confidenceLevel),
            )
        )

        prevVarValue = job.varValue
        prevEs = job.expectedShortfall
    }

    return entries
}

private fun parseConfidenceLevel(raw: String?): Double? {
    if (raw == null) return null
    val digits = raw.removePrefix("CL_").toDoubleOrNull() ?: return null
    return digits / 100.0
}
