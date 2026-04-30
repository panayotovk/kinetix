package com.kinetix.position.kafka

import com.kinetix.position.fix.PrimeBrokerReconciliation
import com.kinetix.position.fix.ReconciliationBreak

class NoOpReconciliationAlertPublisher : ReconciliationAlertPublisher {
    override suspend fun publishBreakAlert(reconciliation: PrimeBrokerReconciliation, break_: ReconciliationBreak) {
        // No-op for tests and environments without Kafka configured
    }
}
