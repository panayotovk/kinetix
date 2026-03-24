package com.kinetix.risk.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object CounterpartyExposureHistoryTable : Table("counterparty_exposure_history") {
    val id = long("id").autoIncrement()
    val counterpartyId = varchar("counterparty_id", 255)
    val calculatedAt = timestampWithTimeZone("calculated_at")
    val pfeProfileJson = text("pfe_profile_json")
    val nettingSetExposuresJson = text("netting_set_exposures_json")
    val wrongWayRiskFlagsJson = text("wrong_way_risk_flags_json")
    val currentNetExposure = decimal("current_net_exposure", 24, 6)
    val peakPfe = decimal("peak_pfe", 24, 6)
    val cva = decimal("cva", 24, 6).nullable()
    val cvaEstimated = bool("cva_estimated")
    val currency = varchar("currency", 3)

    override val primaryKey = PrimaryKey(id, calculatedAt)
}
