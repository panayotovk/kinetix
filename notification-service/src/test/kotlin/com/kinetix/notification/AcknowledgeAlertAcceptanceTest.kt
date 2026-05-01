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

class AcknowledgeAlertAcceptanceTest : FunSpec({

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

    test("a TRIGGERED alert exists — POST /alerts/{alertId}/acknowledge with valid body — returns 200 with ACKNOWLEDGED status and persists acknowledgement") {
        val (eventRepo, ackRepo, rulesEngine) = newRepos()
        val alert = AlertEvent(
            id = "alert-ack-1",
            ruleId = "r1",
            ruleName = "VaR Breach",
            type = AlertType.VAR_BREACH,
            severity = Severity.CRITICAL,
            message = "VaR exceeded threshold",
            currentValue = 150_000.0,
            threshold = 100_000.0,
            bookId = "book-1",
            triggeredAt = Instant.parse("2025-01-15T10:00:00Z"),
        )
        eventRepo.save(alert)

        testApplication {
            application {
                module(rulesEngine, InAppDeliveryService(eventRepo), ackRepo)
            }
            val response = client.post("/api/v1/notifications/alerts/alert-ack-1/acknowledge") {
                contentType(ContentType.Application.Json)
                setBody("""{"acknowledgedBy":"trader-1","notes":"Reviewing position"}""")
            }
            response.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["status"]?.jsonPrimitive?.content shouldBe "ACKNOWLEDGED"
            body["id"]?.jsonPrimitive?.content shouldBe "alert-ack-1"
        }

        val ack = ackRepo.findByAlertId("alert-ack-1")
        ack?.acknowledgedBy shouldBe "trader-1"
        ack?.notes shouldBe "Reviewing position"

        val persistedAlert = eventRepo.findById("alert-ack-1")
        persistedAlert?.status shouldBe AlertStatus.ACKNOWLEDGED
        persistedAlert?.acknowledgedAt shouldBe ack?.acknowledgedAt
    }

    test("a TRIGGERED alert exists — POST /alerts/{alertId}/acknowledge for nonexistent alert — returns 404") {
        val (eventRepo, ackRepo, rulesEngine) = newRepos()
        testApplication {
            application {
                module(rulesEngine, InAppDeliveryService(eventRepo), ackRepo)
            }
            val response = client.post("/api/v1/notifications/alerts/nonexistent/acknowledge") {
                contentType(ContentType.Application.Json)
                setBody("""{"acknowledgedBy":"trader-1"}""")
            }
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    test("a TRIGGERED alert exists — POST /alerts/{alertId}/acknowledge for already resolved alert — returns 409 Conflict") {
        val (eventRepo, ackRepo, rulesEngine) = newRepos()
        val resolvedAlert = AlertEvent(
            id = "alert-resolved-1",
            ruleId = "r1",
            ruleName = "VaR Breach",
            type = AlertType.VAR_BREACH,
            severity = Severity.CRITICAL,
            message = "VaR exceeded threshold",
            currentValue = 150_000.0,
            threshold = 100_000.0,
            bookId = "book-1",
            triggeredAt = Instant.parse("2025-01-15T10:00:00Z"),
            status = AlertStatus.RESOLVED,
            resolvedAt = Instant.parse("2025-01-15T10:30:00Z"),
            resolvedReason = "AUTO_CLEARED",
        )
        eventRepo.save(resolvedAlert)

        testApplication {
            application {
                module(rulesEngine, InAppDeliveryService(eventRepo), ackRepo)
            }
            val response = client.post("/api/v1/notifications/alerts/alert-resolved-1/acknowledge") {
                contentType(ContentType.Application.Json)
                setBody("""{"acknowledgedBy":"trader-1"}""")
            }
            response.status shouldBe HttpStatusCode.Conflict
        }
    }

    test("a TRIGGERED alert exists — POST /alerts/{alertId}/acknowledge for already acknowledged alert — returns 409 Conflict") {
        val (eventRepo, ackRepo, rulesEngine) = newRepos()
        val ackedAlert = AlertEvent(
            id = "alert-acked-1",
            ruleId = "r1",
            ruleName = "VaR Breach",
            type = AlertType.VAR_BREACH,
            severity = Severity.CRITICAL,
            message = "VaR exceeded threshold",
            currentValue = 150_000.0,
            threshold = 100_000.0,
            bookId = "book-1",
            triggeredAt = Instant.parse("2025-01-15T10:00:00Z"),
            status = AlertStatus.ACKNOWLEDGED,
        )
        eventRepo.save(ackedAlert)

        testApplication {
            application {
                module(rulesEngine, InAppDeliveryService(eventRepo), ackRepo)
            }
            val response = client.post("/api/v1/notifications/alerts/alert-acked-1/acknowledge") {
                contentType(ContentType.Application.Json)
                setBody("""{"acknowledgedBy":"trader-2"}""")
            }
            response.status shouldBe HttpStatusCode.Conflict
        }
    }
})
