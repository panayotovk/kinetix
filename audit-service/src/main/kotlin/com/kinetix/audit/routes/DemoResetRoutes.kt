package com.kinetix.audit.routes

import com.kinetix.audit.persistence.AuditEventRepository
import com.kinetix.audit.seed.DevDataSeeder
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

@Serializable
data class DemoResetResponse(val status: String, val message: String)

fun Route.demoResetRoutes(
    db: Database,
    repository: AuditEventRepository,
    resetToken: String,
) {
    route("/api/v1/internal/audit") {
        post("/demo-reset") {
            val token = call.request.headers["X-Demo-Reset-Token"]
            if (token != resetToken) {
                call.respond(HttpStatusCode.Forbidden, DemoResetResponse("error", "Invalid reset token"))
                return@post
            }

            newSuspendedTransaction(db = db) {
                exec("ALTER TABLE audit_events DISABLE TRIGGER prevent_audit_update")
                exec("ALTER TABLE audit_events DISABLE TRIGGER prevent_audit_delete")
                exec("TRUNCATE TABLE audit_events RESTART IDENTITY CASCADE")
                exec("ALTER TABLE audit_events ENABLE TRIGGER prevent_audit_update")
                exec("ALTER TABLE audit_events ENABLE TRIGGER prevent_audit_delete")
            }

            DevDataSeeder(repository).seed()

            call.respond(DemoResetResponse("ok", "Audit data reset and reseeded"))
        }
    }
}
