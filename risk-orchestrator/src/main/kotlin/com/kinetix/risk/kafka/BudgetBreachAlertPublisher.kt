package com.kinetix.risk.kafka

import com.kinetix.risk.model.BudgetUtilisation

/**
 * Publishes an alert when a hierarchy entity's VaR exceeds its allocated risk
 * budget. Implementations emit a downstream event so the notification-service
 * can fire a `RISK_BUDGET_EXCEEDED` alert against any matching alert rules.
 *
 * Spec: hierarchy-risk.allium AlertOnBudgetBreach.
 */
interface BudgetBreachAlertPublisher {
    suspend fun publishBreach(utilisation: BudgetUtilisation)
}

/** No-op publisher for tests and contexts where Kafka is not configured. */
object NoOpBudgetBreachAlertPublisher : BudgetBreachAlertPublisher {
    override suspend fun publishBreach(utilisation: BudgetUtilisation) = Unit
}
