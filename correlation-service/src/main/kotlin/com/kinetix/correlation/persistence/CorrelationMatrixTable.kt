package com.kinetix.correlation.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object CorrelationMatrixTable : Table("correlation_matrices") {
    val id = long("id").autoIncrement()
    val labelsJson = jsonb("labels", { it }, { it })
    val valuesJson = jsonb("values", { it }, { it })
    val windowDays = integer("window_days")
    val asOfDate = timestampWithTimeZone("as_of_date")
    val method = varchar("method", 50)
    val createdAt = timestampWithTimeZone("created_at")
    val labelsHash = varchar("labels_hash", 32)

    override val primaryKey = PrimaryKey(id)
}
