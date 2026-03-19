package com.kinetix.position.reconciliation

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal

class ExposedReconciliationRepository(private val db: Database? = null) : ReconciliationRepository {

    override suspend fun findTradeQuantityByPosition(): List<PositionQuantity> =
        newSuspendedTransaction(db = db) {
            val rows = mutableListOf<PositionQuantity>()
            exec(
                """
                SELECT portfolio_id, instrument_id,
                       SUM(CASE WHEN side = 'BUY' THEN quantity ELSE -quantity END) AS net_quantity
                FROM trade_events
                WHERE status = 'LIVE'
                GROUP BY portfolio_id, instrument_id
                """.trimIndent()
            ) { rs ->
                while (rs.next()) {
                    rows.add(
                        PositionQuantity(
                            portfolioId = rs.getString("portfolio_id"),
                            instrumentId = rs.getString("instrument_id"),
                            quantity = rs.getBigDecimal("net_quantity"),
                        )
                    )
                }
            }
            rows
        }

    override suspend fun findCurrentPositionQuantities(): List<PositionQuantity> =
        newSuspendedTransaction(db = db) {
            val rows = mutableListOf<PositionQuantity>()
            exec(
                """
                SELECT portfolio_id, instrument_id, quantity
                FROM positions
                """.trimIndent()
            ) { rs ->
                while (rs.next()) {
                    rows.add(
                        PositionQuantity(
                            portfolioId = rs.getString("portfolio_id"),
                            instrumentId = rs.getString("instrument_id"),
                            quantity = rs.getBigDecimal("quantity"),
                        )
                    )
                }
            }
            rows
        }
}
