package com.kinetix.risk.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date

object InstrumentFactorLoadingsTable : Table("instrument_factor_loadings") {
    val instrumentId = varchar("instrument_id", 64)
    val factorName = varchar("factor_name", 64)
    val loading = double("loading")
    val rSquared = double("r_squared").nullable()
    val method = varchar("method", 32)
    val estimationDate = date("estimation_date")
    val estimationWindow = integer("estimation_window")

    override val primaryKey = PrimaryKey(instrumentId, factorName)
}
