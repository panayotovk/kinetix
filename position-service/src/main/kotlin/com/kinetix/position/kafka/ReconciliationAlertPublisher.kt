package com.kinetix.position.kafka

import com.kinetix.position.fix.PrimeBrokerReconciliation
import com.kinetix.position.fix.ReconciliationBreak

/**
 * Publishes a RECONCILIATION_BREAK alert per break that exceeds the
 * manual-review notional threshold, per `execution.allium:437-448`
 * (`AlertOnReconciliationBreaks` rule iterates breaks and applies a
 * per-break filter rather than a single bundled alert).
 */
interface ReconciliationAlertPublisher {
    suspend fun publishBreakAlert(reconciliation: PrimeBrokerReconciliation, break_: ReconciliationBreak)
}
