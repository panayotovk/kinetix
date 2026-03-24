package com.kinetix.position.routes

import com.kinetix.position.fix.FIXSessionRepository
import com.kinetix.position.routes.dtos.FIXSessionResponse
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.fixSessionRoutes(fixSessionRepository: FIXSessionRepository) {
    route("/api/v1/fix") {
        get("/sessions", {
            summary = "List all FIX sessions and their connectivity status"
            tags = listOf("Execution")
            response {
                code(HttpStatusCode.OK) { body<List<FIXSessionResponse>>() }
            }
        }) {
            val sessions = fixSessionRepository.findAll()
            call.respond(sessions.map { session ->
                FIXSessionResponse(
                    sessionId = session.sessionId,
                    counterparty = session.counterparty,
                    status = session.status.name,
                    lastMessageAt = session.lastMessageAt?.toString(),
                    inboundSeqNum = session.inboundSeqNum,
                    outboundSeqNum = session.outboundSeqNum,
                )
            })
        }
    }
}
