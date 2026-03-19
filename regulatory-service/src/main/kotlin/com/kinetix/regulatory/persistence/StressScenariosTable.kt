package com.kinetix.regulatory.persistence

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object StressScenariosTable : Table("stress_scenarios") {
    val id = varchar("id", 255)
    val name = varchar("name", 255)
    val description = text("description")
    val shocks = jsonb<JsonElement>("shocks", Json)
    val status = varchar("status", 30)
    val createdBy = varchar("created_by", 255)
    val approvedBy = varchar("approved_by", 255).nullable()
    val approvedAt = timestampWithTimeZone("approved_at").nullable()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}
