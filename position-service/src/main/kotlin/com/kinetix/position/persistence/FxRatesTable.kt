package com.kinetix.position.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object FxRatesTable : Table("fx_rates") {
    val fromCurrency = char("from_currency", 3)
    val toCurrency = char("to_currency", 3)
    val rate = decimal("rate", 18, 8)
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(fromCurrency, toCurrency)
}
