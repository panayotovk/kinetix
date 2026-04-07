package com.kinetix.risk.routes

import com.kinetix.risk.persistence.CounterpartyExposureRepository
import com.kinetix.risk.seed.DevDataSeeder
import com.kinetix.risk.service.ValuationJobRecorder
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

@Serializable
data class DemoResetResponse(val status: String, val message: String)

fun Route.demoResetRoutes(
    riskDb: Database,
    jobRecorder: ValuationJobRecorder,
    exposureRepository: CounterpartyExposureRepository,
    resetToken: String,
) {
    route("/api/v1/internal/risk") {
        post("/demo-reset") {
            val token = call.request.headers["X-Demo-Reset-Token"]
            if (token != resetToken) {
                call.respond(HttpStatusCode.Forbidden, DemoResetResponse("error", "Invalid reset token"))
                return@post
            }

            jobRecorder.deleteByTriggeredBy("SEED")

            newSuspendedTransaction(db = riskDb) {
                exec("DELETE FROM daily_risk_snapshots WHERE snapshot_date > '2026-02-27'")
                exec("DELETE FROM intraday_pnl_snapshots WHERE snapshot_time > '2026-02-27'")
                exec("DELETE FROM counterparty_exposure_history")
            }

            DevDataSeeder(jobRecorder, exposureRepository).seed()

            call.respond(DemoResetResponse("ok", "Risk data reset and reseeded"))
        }
    }
}
