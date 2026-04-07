package com.kinetix.risk.persistence

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.postgresql.util.PGobject
import java.math.BigDecimal

/**
 * Custom column type that stores plain JSON strings in a PostgreSQL JSONB column.
 * Exposed's built-in `text()` sends VARCHAR which PostgreSQL rejects for JSONB columns.
 */
private class JsonbTextColumnType : ColumnType<String>() {
    override fun sqlType(): String = "JSONB"

    override fun valueFromDB(value: Any): String = when (value) {
        is PGobject -> value.value ?: "[]"
        is String -> value
        else -> value.toString()
    }

    override fun notNullValueToDB(value: String): Any = PGobject().apply {
        type = "jsonb"
        this.value = value
    }
}

private fun Table.jsonbText(name: String): Column<String> = registerColumn(name, JsonbTextColumnType())

object CounterpartyExposureHistoryTable : Table("counterparty_exposure_history") {
    val id = long("id").autoIncrement()
    val counterpartyId = varchar("counterparty_id", 255)
    val calculatedAt = timestampWithTimeZone("calculated_at")
    val pfeProfileJson = jsonbText("pfe_profile_json")
    val nettingSetExposuresJson = jsonbText("netting_set_exposures_json")
    val wrongWayRiskFlagsJson = jsonbText("wrong_way_risk_flags_json")
    val currentNetExposure = decimal("current_net_exposure", 24, 6)
    val peakPfe = decimal("peak_pfe", 24, 6)
    val cva = decimal("cva", 24, 6).nullable()
    val cvaEstimated = bool("cva_estimated")
    val currency = varchar("currency", 3)
    val collateralHeld = decimal("collateral_held", 24, 6).default(BigDecimal.ZERO)
    val collateralPosted = decimal("collateral_posted", 24, 6).default(BigDecimal.ZERO)
    val netNetExposure = decimal("net_net_exposure", 24, 6).nullable()

    override val primaryKey = PrimaryKey(id, calculatedAt)
}
