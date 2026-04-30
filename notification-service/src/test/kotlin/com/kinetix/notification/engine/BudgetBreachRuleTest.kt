package com.kinetix.notification.engine

import com.kinetix.common.kafka.events.ConcentrationItem
import com.kinetix.common.kafka.events.RiskResultEvent
import com.kinetix.notification.engine.extractors.BudgetBreachExtractor
import com.kinetix.notification.model.AlertRule
import com.kinetix.notification.model.AlertType
import com.kinetix.notification.model.ComparisonOperator
import com.kinetix.notification.model.DeliveryChannel
import com.kinetix.notification.model.Severity
import com.kinetix.notification.persistence.InMemoryAlertRuleRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

private fun budgetBreachEvent(
    bookId: String = "DESK:desk-rates",
    utilisationPct: Double = 110.0,
) = RiskResultEvent(
    bookId = bookId,
    varValue = "5500000.0",
    expectedShortfall = "0.0",
    calculationType = "BUDGET_BREACH",
    calculatedAt = "2026-04-30T12:00:00Z",
    concentrationByInstrument = listOf(
        ConcentrationItem(
            instrumentId = BudgetBreachExtractor.BUDGET_BREACH_SENTINEL_ID,
            percentage = utilisationPct,
        ),
    ),
)

private fun budgetBreachRule(threshold: Double = 100.0) = AlertRule(
    id = "budget-1",
    name = "Risk Budget Exceeded",
    type = AlertType.RISK_BUDGET_EXCEEDED,
    threshold = threshold,
    operator = ComparisonOperator.GREATER_THAN,
    severity = Severity.WARNING,
    channels = listOf(DeliveryChannel.IN_APP),
)

class BudgetBreachRuleTest : FunSpec({

    test("RISK_BUDGET_EXCEEDED rule fires when utilisation exceeds the threshold") {
        val engine = RulesEngine(InMemoryAlertRuleRepository())
        engine.addRule(budgetBreachRule())

        val alerts = engine.evaluate(budgetBreachEvent(utilisationPct = 110.0))

        alerts shouldHaveSize 1
        alerts[0].type shouldBe AlertType.RISK_BUDGET_EXCEEDED
        alerts[0].severity shouldBe Severity.WARNING
        alerts[0].bookId shouldBe "DESK:desk-rates"
    }

    test("RISK_BUDGET_EXCEEDED rule does not fire when sentinel is absent") {
        val engine = RulesEngine(InMemoryAlertRuleRepository())
        engine.addRule(budgetBreachRule())

        val event = RiskResultEvent(
            bookId = "DESK:desk-rates",
            varValue = "100000.0",
            expectedShortfall = "0.0",
            calculationType = "PARAMETRIC",
            calculatedAt = "2026-04-30T12:00:00Z",
        )

        engine.evaluate(event).shouldBeEmpty()
    }

    test("BudgetBreachExtractor returns the utilisation percentage from the sentinel") {
        BudgetBreachExtractor().extract(budgetBreachEvent(utilisationPct = 125.5)) shouldBe 125.5
    }

    test("BudgetBreachExtractor returns null when no VAR_BUDGET sentinel item is present") {
        val event = RiskResultEvent(
            bookId = "BOOK-1",
            varValue = "0.0",
            expectedShortfall = "0.0",
            calculationType = "PARAMETRIC",
            calculatedAt = "2026-04-30T12:00:00Z",
        )

        BudgetBreachExtractor().extract(event) shouldBe null
    }
})
