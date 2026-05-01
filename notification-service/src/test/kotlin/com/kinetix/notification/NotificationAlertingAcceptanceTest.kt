package com.kinetix.notification

import com.kinetix.common.kafka.events.RiskResultEvent
import com.kinetix.notification.delivery.DeliveryRouter
import com.kinetix.notification.delivery.EmailDeliveryService
import com.kinetix.notification.delivery.InAppDeliveryService
import com.kinetix.notification.delivery.WebhookDeliveryService
import com.kinetix.notification.engine.RulesEngine
import com.kinetix.notification.model.AlertRule
import com.kinetix.notification.model.AlertType
import com.kinetix.notification.model.ComparisonOperator
import com.kinetix.notification.model.DeliveryChannel
import com.kinetix.notification.model.Severity
import com.kinetix.notification.persistence.AlertAcknowledgementsTable
import com.kinetix.notification.persistence.AlertEventsTable
import com.kinetix.notification.persistence.AlertRulesTable
import com.kinetix.notification.persistence.DatabaseTestSetup
import com.kinetix.notification.persistence.ExposedAlertEventRepository
import com.kinetix.notification.persistence.ExposedAlertRuleRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction

class NotificationAlertingAcceptanceTest : FunSpec({

    val db = DatabaseTestSetup.startAndMigrate()

    beforeEach {
        transaction(db) {
            AlertAcknowledgementsTable.deleteAll()
            AlertEventsTable.deleteAll()
            AlertRulesTable.deleteAll()
        }
    }

    test("a portfolio with VaR breach alert rule configured — VaR calculation result exceeds the threshold — alert event is generated with CRITICAL severity, message contains book id and value, and is delivered to in-app and email channels") {
        val ruleRepo = ExposedAlertRuleRepository(db)
        val eventRepo = ExposedAlertEventRepository(db)
        val rulesEngine = RulesEngine(ruleRepo, eventRepository = eventRepo)
        val inApp = InAppDeliveryService(eventRepo)
        val email = EmailDeliveryService()
        val webhook = WebhookDeliveryService()
        val router = DeliveryRouter(listOf(inApp, email, webhook))

        rulesEngine.addRule(
            AlertRule(
                id = "rule-var",
                name = "VaR Critical Limit",
                type = AlertType.VAR_BREACH,
                threshold = 100_000.0,
                operator = ComparisonOperator.GREATER_THAN,
                severity = Severity.CRITICAL,
                channels = listOf(DeliveryChannel.IN_APP, DeliveryChannel.EMAIL),
            ),
        )

        val event = RiskResultEvent(
            bookId = "port-alert-1",
            varValue = "150000.0",
            expectedShortfall = "180000.0",
            calculationType = "PARAMETRIC",
            calculatedAt = "2025-01-15T10:00:00Z",
        )
        val alerts = rulesEngine.evaluate(event)
        for (alert in alerts) {
            val rule = rulesEngine.listRules().find { it.id == alert.ruleId }!!
            router.route(alert, rule.channels)
        }

        alerts shouldHaveSize 1
        alerts[0].severity shouldBe Severity.CRITICAL
        alerts[0].message shouldContain "port-alert-1"
        alerts[0].message shouldContain "150000"

        inApp.getRecentAlerts() shouldHaveSize 1
        inApp.getRecentAlerts()[0].bookId shouldBe "port-alert-1"

        email.sentEmails shouldHaveSize 1
        email.sentEmails[0].bookId shouldBe "port-alert-1"
    }

    test("a portfolio with VaR breach alert rule configured — VaR calculation result is below the threshold — no alert is generated") {
        val ruleRepo = ExposedAlertRuleRepository(db)
        val eventRepo = ExposedAlertEventRepository(db)
        val rulesEngine = RulesEngine(ruleRepo, eventRepository = eventRepo)

        rulesEngine.addRule(
            AlertRule(
                id = "rule-var-2",
                name = "VaR Critical Limit",
                type = AlertType.VAR_BREACH,
                threshold = 100_000.0,
                operator = ComparisonOperator.GREATER_THAN,
                severity = Severity.CRITICAL,
                channels = listOf(DeliveryChannel.IN_APP),
            ),
        )

        val event = RiskResultEvent(
            bookId = "port-alert-1",
            varValue = "50000.0",
            expectedShortfall = "60000.0",
            calculationType = "PARAMETRIC",
            calculatedAt = "2025-01-15T10:05:00Z",
        )
        val alerts = rulesEngine.evaluate(event)

        alerts.shouldBeEmpty()
    }

    test("multiple alert rules configured — risk result triggers some but not all rules — only matching rules produce alerts and disabled rules are skipped") {
        val ruleRepo = ExposedAlertRuleRepository(db)
        val eventRepo = ExposedAlertEventRepository(db)
        val rulesEngine = RulesEngine(ruleRepo, eventRepository = eventRepo)
        val inApp = InAppDeliveryService(eventRepo)
        val router = DeliveryRouter(listOf(inApp))

        rulesEngine.addRule(
            AlertRule(
                id = "rule-1",
                name = "VaR Limit",
                type = AlertType.VAR_BREACH,
                threshold = 100_000.0,
                operator = ComparisonOperator.GREATER_THAN,
                severity = Severity.CRITICAL,
                channels = listOf(DeliveryChannel.IN_APP),
            ),
        )
        rulesEngine.addRule(
            AlertRule(
                id = "rule-2",
                name = "ES Warning",
                type = AlertType.PNL_THRESHOLD,
                threshold = 500_000.0,
                operator = ComparisonOperator.GREATER_THAN,
                severity = Severity.WARNING,
                channels = listOf(DeliveryChannel.IN_APP),
            ),
        )
        rulesEngine.addRule(
            AlertRule(
                id = "rule-3",
                name = "Disabled Rule",
                type = AlertType.RISK_LIMIT,
                threshold = 10_000.0,
                operator = ComparisonOperator.GREATER_THAN,
                severity = Severity.INFO,
                channels = listOf(DeliveryChannel.IN_APP),
                enabled = false,
            ),
        )

        val event = RiskResultEvent(
            bookId = "port-multi",
            varValue = "150000.0",
            expectedShortfall = "200000.0",
            calculationType = "PARAMETRIC",
            calculatedAt = "2025-01-15T11:00:00Z",
        )
        val alerts = rulesEngine.evaluate(event)
        for (alert in alerts) {
            val rule = rulesEngine.listRules().find { it.id == alert.ruleId }!!
            router.route(alert, rule.channels)
        }

        alerts shouldHaveSize 1
        alerts[0].ruleId shouldBe "rule-1"
        alerts[0].type shouldBe AlertType.VAR_BREACH

        val disabledAlerts = alerts.filter { it.ruleId == "rule-3" }
        disabledAlerts.shouldBeEmpty()
    }
})
