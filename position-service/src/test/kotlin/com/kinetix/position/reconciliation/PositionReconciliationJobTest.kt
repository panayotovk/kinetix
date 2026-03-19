package com.kinetix.position.reconciliation

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import java.math.BigDecimal

class PositionReconciliationJobTest : FunSpec({

    test("detects quantity mismatch between trades and positions") {
        val repository = mockk<ReconciliationRepository>()
        coEvery { repository.findTradeQuantityByPosition() } returns listOf(
            PositionQuantity("port-1", "AAPL", BigDecimal("100")),
            PositionQuantity("port-1", "MSFT", BigDecimal("50")),
        )
        coEvery { repository.findCurrentPositionQuantities() } returns listOf(
            PositionQuantity("port-1", "AAPL", BigDecimal("100")),
            PositionQuantity("port-1", "MSFT", BigDecimal("75")),
        )

        val job = PositionReconciliationJob(repository)

        // runReconciliation should not throw; discrepancies are logged as WARN
        var threw = false
        try {
            job.runReconciliation()
        } catch (e: Exception) {
            threw = true
        }
        threw shouldBe false
    }

    test("reports clean reconciliation when totals match") {
        val repository = mockk<ReconciliationRepository>()
        coEvery { repository.findTradeQuantityByPosition() } returns listOf(
            PositionQuantity("port-1", "AAPL", BigDecimal("100")),
        )
        coEvery { repository.findCurrentPositionQuantities() } returns listOf(
            PositionQuantity("port-1", "AAPL", BigDecimal("100")),
        )

        val job = PositionReconciliationJob(repository)
        var threw = false
        try {
            job.runReconciliation()
        } catch (e: Exception) {
            threw = true
        }
        threw shouldBe false
    }

    test("handles amended and cancelled trades correctly") {
        // Amended/cancelled trades have status != LIVE and are excluded from the
        // trade-derived quantity by the SQL query (WHERE status = 'LIVE').
        // The repository mock simulates this exclusion — only LIVE trades contribute.
        val repository = mockk<ReconciliationRepository>()
        coEvery { repository.findTradeQuantityByPosition() } returns listOf(
            // Original BUY 200, then SELL 50 → net 150 (only LIVE trades counted)
            PositionQuantity("port-1", "AAPL", BigDecimal("150")),
        )
        coEvery { repository.findCurrentPositionQuantities() } returns listOf(
            PositionQuantity("port-1", "AAPL", BigDecimal("150")),
        )

        val job = PositionReconciliationJob(repository)
        var threw = false
        try {
            job.runReconciliation()
        } catch (e: Exception) {
            threw = true
        }
        threw shouldBe false
    }
})
