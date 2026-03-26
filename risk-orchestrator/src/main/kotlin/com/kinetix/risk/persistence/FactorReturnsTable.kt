package com.kinetix.risk.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date

object FactorReturnsTable : Table("factor_returns") {
    val factorName = varchar("factor_name", 64)
    val asOfDate = date("as_of_date")
    val returnValue = double("return_value")
    val returnSource = varchar("source", 64)

    override val primaryKey = PrimaryKey(factorName, asOfDate)
}
