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

class NotificationErrorResponseContractAcceptanceTest : FunSpec({

    val db = DatabaseTestSetup.startAndMigrate()

    beforeEach {
        transaction(db) {
            AlertAcknowledgementsTable.deleteAll()
            AlertEventsTable.deleteAll()
            AlertRulesTable.deleteAll()
        }
    }

    test("notification-service error responses — POST /api/v1/notifications/rules with invalid type — returns 400 with { error, message } shape") {
        val ruleRepo = ExposedAlertRuleRepository(db)
        val eventRepo = ExposedAlertEventRepository(db)
        val ackRepo = ExposedAlertAcknowledgementRepository(db)
        testApplication {
            application {
                val rulesEngine = RulesEngine(ruleRepo)
                val inAppDelivery = InAppDeliveryService(eventRepo)
                module(rulesEngine, inAppDelivery, ackRepo)
            }
            val response = client.post("/api/v1/notifications/rules") {
                contentType(ContentType.Application.Json)
                setBody("""{"name":"Test","type":"INVALID_TYPE","threshold":1000.0,"operator":"GREATER_THAN","severity":"INFO","channels":["IN_APP"]}""")
            }
            response.status shouldBe HttpStatusCode.BadRequest
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body.containsKey("error") shouldBe true
            body.containsKey("message") shouldBe true
        }
    }
})
