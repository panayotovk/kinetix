package com.kinetix.notification.contract

import com.kinetix.notification.delivery.InAppDeliveryService
import com.kinetix.notification.engine.RulesEngine
import com.kinetix.notification.module
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

class NotificationApiContractAcceptanceTest : FunSpec({

    val db = DatabaseTestSetup.startAndMigrate()

    beforeEach {
        transaction(db) {
            AlertAcknowledgementsTable.deleteAll()
            AlertEventsTable.deleteAll()
            AlertRulesTable.deleteAll()
        }
    }

    test("the notification-service running — POST /api/v1/notifications/rules with valid body — returns 201 with rule shape matching gateway expectations") {
        val ruleRepo = ExposedAlertRuleRepository(db)
        val eventRepo = ExposedAlertEventRepository(db)
        val ackRepo = ExposedAlertAcknowledgementRepository(db)
        testApplication {
            application {
                module(
                    RulesEngine(ruleRepo),
                    InAppDeliveryService(eventRepo),
                    ackRepo,
                )
            }
            val response = client.post("/api/v1/notifications/rules") {
                contentType(ContentType.Application.Json)
                setBody("""{"name":"VaR Limit","type":"VAR_BREACH","threshold":100000.0,"operator":"GREATER_THAN","severity":"CRITICAL","channels":["IN_APP","EMAIL"]}""")
            }
            response.status shouldBe HttpStatusCode.Created
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body.containsKey("id") shouldBe true
            body["name"]?.jsonPrimitive?.content shouldBe "VaR Limit"
            body["type"]?.jsonPrimitive?.content shouldBe "VAR_BREACH"
            body["threshold"]?.jsonPrimitive?.double shouldBe 100000.0
            body["operator"]?.jsonPrimitive?.content shouldBe "GREATER_THAN"
            body["severity"]?.jsonPrimitive?.content shouldBe "CRITICAL"
            body["channels"]?.jsonArray?.map { it.jsonPrimitive.content } shouldBe listOf("IN_APP", "EMAIL")
            body["enabled"]?.jsonPrimitive?.boolean shouldBe true
        }
    }

    test("the notification-service running — GET /api/v1/notifications/rules after creating a rule — returns 200 with array shape") {
        val ruleRepo = ExposedAlertRuleRepository(db)
        val eventRepo = ExposedAlertEventRepository(db)
        val ackRepo = ExposedAlertAcknowledgementRepository(db)
        testApplication {
            application {
                module(
                    RulesEngine(ruleRepo),
                    InAppDeliveryService(eventRepo),
                    ackRepo,
                )
            }
            client.post("/api/v1/notifications/rules") {
                contentType(ContentType.Application.Json)
                setBody("""{"name":"Test","type":"VAR_BREACH","threshold":1000.0,"operator":"GREATER_THAN","severity":"INFO","channels":["IN_APP"]}""")
            }
            val response = client.get("/api/v1/notifications/rules")
            response.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            body.size shouldBe 1
            body[0].jsonObject.containsKey("id") shouldBe true
            body[0].jsonObject.containsKey("name") shouldBe true
            body[0].jsonObject.containsKey("type") shouldBe true
        }
    }

    test("the notification-service running — DELETE /api/v1/notifications/rules/{ruleId} for nonexistent rule — returns 404") {
        val ruleRepo = ExposedAlertRuleRepository(db)
        val eventRepo = ExposedAlertEventRepository(db)
        val ackRepo = ExposedAlertAcknowledgementRepository(db)
        testApplication {
            application {
                module(
                    RulesEngine(ruleRepo),
                    InAppDeliveryService(eventRepo),
                    ackRepo,
                )
            }
            val response = client.delete("/api/v1/notifications/rules/nonexistent")
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    test("the notification-service running — GET /api/v1/notifications/alerts with no alerts — returns 200 with empty array") {
        val ruleRepo = ExposedAlertRuleRepository(db)
        val eventRepo = ExposedAlertEventRepository(db)
        val ackRepo = ExposedAlertAcknowledgementRepository(db)
        testApplication {
            application {
                module(
                    RulesEngine(ruleRepo),
                    InAppDeliveryService(eventRepo),
                    ackRepo,
                )
            }
            val response = client.get("/api/v1/notifications/alerts")
            response.status shouldBe HttpStatusCode.OK
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            body.size shouldBe 0
        }
    }
})
