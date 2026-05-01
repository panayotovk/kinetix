package com.kinetix.notification

import com.kinetix.notification.delivery.InAppDeliveryService
import com.kinetix.notification.engine.RulesEngine
import com.kinetix.notification.model.AlertEvent
import com.kinetix.notification.model.AlertStatus
import com.kinetix.notification.model.AlertType
import com.kinetix.notification.model.Severity
import com.kinetix.notification.persistence.AlertAcknowledgementsTable
import com.kinetix.notification.persistence.AlertEventsTable
import com.kinetix.notification.persistence.AlertRulesTable
import com.kinetix.notification.persistence.DatabaseTestSetup
import com.kinetix.notification.persistence.ExposedAlertAcknowledgementRepository
import com.kinetix.notification.persistence.ExposedAlertEventRepository
import com.kinetix.notification.persistence.ExposedAlertRuleRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class AlertEscalationAcceptanceTest : FunSpec({

    val db = DatabaseTestSetup.startAndMigrate()

    beforeEach {
        transaction(db) {
            AlertAcknowledgementsTable.deleteAll()
            AlertEventsTable.deleteAll()
            AlertRulesTable.deleteAll()
        }
    }

    fun newRepos(): Triple<ExposedAlertEventRepository, ExposedAlertAcknowledgementRepository, RulesEngine> {
        val eventRepo = ExposedAlertEventRepository(db)
        val ackRepo = ExposedAlertAcknowledgementRepository(db)
        val ruleRepo = ExposedAlertRuleRepository(db)
        val rulesEngine = RulesEngine(ruleRepo, eventRepository = eventRepo)
        return Triple(eventRepo, ackRepo, rulesEngine)
    }

    test("escalated alerts exist in the repository — GET /api/v1/notifications/alerts/escalated — returns only escalated alerts") {
        val (eventRepo, ackRepo, rulesEngine) = newRepos()

        val escalatedAlert = AlertEvent(
            id = "esc-1",
            ruleId = "r1",
            ruleName = "VaR Critical Limit",
            type = AlertType.VAR_BREACH,
            severity = Severity.CRITICAL,
            message = "VaR breach not acknowledged in time",
            currentValue = 250_000.0,
            threshold = 100_000.0,
            bookId = "book-1",
            triggeredAt = Instant.parse("2025-01-15T09:00:00Z"),
            status = AlertStatus.ESCALATED,
            acknowledgedAt = Instant.parse("2025-01-15T09:05:00Z"),
            escalatedAt = Instant.parse("2025-01-15T09:35:00Z"),
            escalatedTo = "risk-manager,cro",
        )
        val triggeredAlert = AlertEvent(
            id = "trig-1",
            ruleId = "r2",
            ruleName = "P&L Warning",
            type = AlertType.PNL_THRESHOLD,
            severity = Severity.WARNING,
            message = "P&L threshold exceeded",
            currentValue = 200_000.0,
            threshold = 150_000.0,
            bookId = "book-2",
            triggeredAt = Instant.parse("2025-01-15T10:00:00Z"),
            status = AlertStatus.TRIGGERED,
        )
        eventRepo.save(escalatedAlert)
        eventRepo.save(triggeredAlert)

        testApplication {
            application {
                module(rulesEngine, InAppDeliveryService(eventRepo), ackRepo)
            }
            val response = client.get("/api/v1/notifications/alerts/escalated")
            response.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            body.size shouldBe 1
            body[0].jsonObject["id"]?.jsonPrimitive?.content shouldBe "esc-1"
            body[0].jsonObject["status"]?.jsonPrimitive?.content shouldBe "ESCALATED"
        }
    }

    test("escalated alerts exist in the repository — GET /api/v1/notifications/alerts/escalated with no escalated alerts — returns an empty list") {
        val (eventRepo, ackRepo, rulesEngine) = newRepos()

        val triggeredAlert = AlertEvent(
            id = "trig-1",
            ruleId = "r2",
            ruleName = "P&L Warning",
            type = AlertType.PNL_THRESHOLD,
            severity = Severity.WARNING,
            message = "P&L threshold exceeded",
            currentValue = 200_000.0,
            threshold = 150_000.0,
            bookId = "book-2",
            triggeredAt = Instant.parse("2025-01-15T10:00:00Z"),
            status = AlertStatus.TRIGGERED,
        )
        eventRepo.save(triggeredAlert)

        testApplication {
            application {
                module(rulesEngine, InAppDeliveryService(eventRepo), ackRepo)
            }
            val response = client.get("/api/v1/notifications/alerts/escalated")
            response.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            body.size shouldBe 0
        }
    }

    test("escalated alert response includes escalatedTo and escalatedAt fields — GET /api/v1/notifications/alerts returns all alerts — escalated alert includes escalatedTo and escalatedAt in response") {
        val (eventRepo, ackRepo, rulesEngine) = newRepos()

        val escalatedAlert = AlertEvent(
            id = "esc-2",
            ruleId = "r1",
            ruleName = "VaR Critical Limit",
            type = AlertType.VAR_BREACH,
            severity = Severity.CRITICAL,
            message = "VaR breach",
            currentValue = 250_000.0,
            threshold = 100_000.0,
            bookId = "book-1",
            triggeredAt = Instant.parse("2025-01-15T09:00:00Z"),
            status = AlertStatus.ESCALATED,
            acknowledgedAt = Instant.parse("2025-01-15T09:05:00Z"),
            escalatedAt = Instant.parse("2025-01-15T09:35:00Z"),
            escalatedTo = "risk-manager,cro",
        )
        eventRepo.save(escalatedAlert)

        testApplication {
            application {
                module(rulesEngine, InAppDeliveryService(eventRepo), ackRepo)
            }
            val response = client.get("/api/v1/notifications/alerts")
            response.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            val alert = body.first { it.jsonObject["id"]?.jsonPrimitive?.content == "esc-2" }.jsonObject
            alert["escalatedTo"]?.jsonPrimitive?.content shouldBe "risk-manager,cro"
            alert["escalatedAt"]?.jsonPrimitive?.content shouldBe "2025-01-15T09:35:00Z"
        }
    }

    test("attempting to acknowledge an already ESCALATED alert — POST /alerts/{alertId}/acknowledge on ESCALATED alert — returns 409 Conflict, ESCALATED alerts must be resolved, not re-acknowledged") {
        val (eventRepo, ackRepo, rulesEngine) = newRepos()

        val escalatedAlert = AlertEvent(
            id = "esc-3",
            ruleId = "r1",
            ruleName = "VaR Critical Limit",
            type = AlertType.VAR_BREACH,
            severity = Severity.CRITICAL,
            message = "VaR breach",
            currentValue = 250_000.0,
            threshold = 100_000.0,
            bookId = "book-1",
            triggeredAt = Instant.parse("2025-01-15T09:00:00Z"),
            status = AlertStatus.ESCALATED,
            acknowledgedAt = Instant.parse("2025-01-15T09:05:00Z"),
            escalatedAt = Instant.parse("2025-01-15T09:35:00Z"),
            escalatedTo = "risk-manager,cro",
        )
        eventRepo.save(escalatedAlert)

        testApplication {
            application {
                module(rulesEngine, InAppDeliveryService(eventRepo), ackRepo)
            }
            val response = client.post("/api/v1/notifications/alerts/esc-3/acknowledge") {
                contentType(ContentType.Application.Json)
                setBody("""{"acknowledgedBy":"trader-1"}""")
            }
            response.status shouldBe HttpStatusCode.Conflict
        }
    }

    test("GET /api/v1/notifications/alerts with status=ESCALATED filter — GET /api/v1/notifications/alerts?status=ESCALATED — returns only escalated alerts") {
        val (eventRepo, ackRepo, rulesEngine) = newRepos()

        val escalatedAlert = AlertEvent(
            id = "esc-4",
            ruleId = "r1",
            ruleName = "VaR Breach",
            type = AlertType.VAR_BREACH,
            severity = Severity.CRITICAL,
            message = "breach",
            currentValue = 200_000.0,
            threshold = 100_000.0,
            bookId = "book-1",
            triggeredAt = Instant.parse("2025-01-15T09:00:00Z"),
            status = AlertStatus.ESCALATED,
            escalatedAt = Instant.parse("2025-01-15T09:35:00Z"),
            escalatedTo = "risk-manager,cro",
        )
        val triggeredAlert = AlertEvent(
            id = "trig-2",
            ruleId = "r2",
            ruleName = "P&L Warning",
            type = AlertType.PNL_THRESHOLD,
            severity = Severity.WARNING,
            message = "P&L exceeded",
            currentValue = 200_000.0,
            threshold = 150_000.0,
            bookId = "book-2",
            triggeredAt = Instant.parse("2025-01-15T10:00:00Z"),
            status = AlertStatus.TRIGGERED,
        )
        eventRepo.save(escalatedAlert)
        eventRepo.save(triggeredAlert)

        testApplication {
            application {
                module(rulesEngine, InAppDeliveryService(eventRepo), ackRepo)
            }
            val response = client.get("/api/v1/notifications/alerts?status=ESCALATED")
            response.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            body.size shouldBe 1
            body[0].jsonObject["id"]?.jsonPrimitive?.content shouldBe "esc-4"
        }
    }
})
