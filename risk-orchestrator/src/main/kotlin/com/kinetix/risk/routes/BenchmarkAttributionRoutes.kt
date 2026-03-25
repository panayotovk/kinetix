package com.kinetix.risk.routes

import com.kinetix.common.model.BookId
import com.kinetix.risk.model.BrinsonAttributionResult
import com.kinetix.risk.model.BrinsonSectorAttribution
import com.kinetix.risk.routes.dtos.BrinsonAttributionResponse
import com.kinetix.risk.routes.dtos.BrinsonSectorAttributionResponse
import com.kinetix.risk.service.BenchmarkAttributionService
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import java.time.LocalDate
import java.time.format.DateTimeParseException

fun Route.benchmarkAttributionRoutes(
    benchmarkAttributionService: BenchmarkAttributionService,
) {
    get("/api/v1/books/{bookId}/attribution", {
        summary = "Get Brinson-Hood-Beebower performance attribution for a book against a benchmark"
        tags = listOf("Attribution")
        request {
            pathParameter<String>("bookId") { description = "Portfolio book identifier" }
            queryParameter<String>("benchmarkId") {
                description = "Benchmark identifier (e.g. SP500)"
                required = true
            }
            queryParameter<String>("asOfDate") {
                description = "Attribution date (ISO-8601 local date, e.g. 2026-03-25); defaults to today"
                required = false
            }
        }
    }) {
        val bookId = call.requirePathParam("bookId")

        val benchmarkId = call.request.queryParameters["benchmarkId"]
            ?: run {
                call.respond(HttpStatusCode.BadRequest, "Query parameter 'benchmarkId' is required")
                return@get
            }

        val asOfDateParam = call.request.queryParameters["asOfDate"]
        val asOfDate = if (asOfDateParam != null) {
            try {
                LocalDate.parse(asOfDateParam)
            } catch (_: DateTimeParseException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid 'asOfDate': expected ISO-8601 date (e.g. 2026-03-25)")
                return@get
            }
        } else {
            LocalDate.now()
        }

        val result = benchmarkAttributionService.calculateAttribution(
            bookId = BookId(bookId),
            benchmarkId = benchmarkId,
            asOfDate = asOfDate,
        )

        call.respond(result.toResponse(bookId, benchmarkId, asOfDate.toString()))
    }
}

private fun BrinsonAttributionResult.toResponse(
    bookId: String,
    benchmarkId: String,
    asOfDate: String,
) = BrinsonAttributionResponse(
    bookId = bookId,
    benchmarkId = benchmarkId,
    asOfDate = asOfDate,
    sectors = sectors.map { it.toResponse() },
    totalActiveReturn = totalActiveReturn,
    totalAllocationEffect = totalAllocationEffect,
    totalSelectionEffect = totalSelectionEffect,
    totalInteractionEffect = totalInteractionEffect,
)

private fun BrinsonSectorAttribution.toResponse() = BrinsonSectorAttributionResponse(
    sectorLabel = sectorLabel,
    portfolioWeight = portfolioWeight,
    benchmarkWeight = benchmarkWeight,
    portfolioReturn = portfolioReturn,
    benchmarkReturn = benchmarkReturn,
    allocationEffect = allocationEffect,
    selectionEffect = selectionEffect,
    interactionEffect = interactionEffect,
    totalActiveContribution = totalActiveContribution,
)
