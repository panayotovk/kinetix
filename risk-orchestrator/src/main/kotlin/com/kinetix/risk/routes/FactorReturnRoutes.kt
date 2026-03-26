package com.kinetix.risk.routes

import com.kinetix.risk.model.FactorReturn
import com.kinetix.risk.persistence.FactorReturnRepository
import com.kinetix.risk.routes.dtos.FactorReturnRequest
import com.kinetix.risk.routes.dtos.FactorReturnResponse
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDate
import java.time.format.DateTimeParseException

fun Route.factorReturnRoutes(repository: FactorReturnRepository) {

    get("/api/v1/factor-returns/{name}/{date}") {
        val name = call.requirePathParam("name")
        val dateParam = call.parameters["date"] ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing date parameter")
            return@get
        }
        val asOfDate = try {
            LocalDate.parse(dateParam)
        } catch (_: DateTimeParseException) {
            call.respond(HttpStatusCode.BadRequest, "Invalid date format — expected YYYY-MM-DD")
            return@get
        }

        val factorReturn = repository.findByFactorAndDate(name, asOfDate)
        if (factorReturn == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(factorReturn.toResponse())
        }
    }

    get("/api/v1/factor-returns/{name}") {
        val name = call.requirePathParam("name")
        val fromParam = call.request.queryParameters["from"]
        val toParam = call.request.queryParameters["to"]

        if (fromParam == null || toParam == null) {
            call.respond(HttpStatusCode.BadRequest, "Both 'from' and 'to' query parameters are required")
            return@get
        }

        val from = try {
            LocalDate.parse(fromParam)
        } catch (_: DateTimeParseException) {
            call.respond(HttpStatusCode.BadRequest, "Invalid 'from' date format — expected YYYY-MM-DD")
            return@get
        }
        val to = try {
            LocalDate.parse(toParam)
        } catch (_: DateTimeParseException) {
            call.respond(HttpStatusCode.BadRequest, "Invalid 'to' date format — expected YYYY-MM-DD")
            return@get
        }

        val results = repository.findByFactorAndDateRange(name, from, to)
        call.respond(results.map { it.toResponse() })
    }

    put("/api/v1/factor-returns") {
        val request = call.receive<FactorReturnRequest>()
        val asOfDate = try {
            LocalDate.parse(request.asOfDate)
        } catch (_: DateTimeParseException) {
            call.respond(HttpStatusCode.BadRequest, "Invalid 'asOfDate' format — expected YYYY-MM-DD")
            return@put
        }

        repository.save(
            FactorReturn(
                factorName = request.factorName,
                asOfDate = asOfDate,
                returnValue = request.returnValue,
                source = request.source,
            )
        )
        call.respond(HttpStatusCode.NoContent)
    }
}

private fun FactorReturn.toResponse() = FactorReturnResponse(
    factorName = factorName,
    asOfDate = asOfDate.toString(),
    returnValue = returnValue,
    source = source,
)
