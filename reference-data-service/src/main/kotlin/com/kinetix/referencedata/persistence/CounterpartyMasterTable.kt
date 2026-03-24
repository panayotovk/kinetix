package com.kinetix.referencedata.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object CounterpartyMasterTable : Table("counterparty_master") {
    val counterpartyId = varchar("counterparty_id", 255)
    val legalName = varchar("legal_name", 500)
    val shortName = varchar("short_name", 100)
    val lei = varchar("lei", 20).nullable()
    val ratingSp = varchar("rating_sp", 10).nullable()
    val ratingMoodys = varchar("rating_moodys", 10).nullable()
    val ratingFitch = varchar("rating_fitch", 10).nullable()
    val sector = varchar("sector", 100).default("OTHER")
    val country = varchar("country", 3).nullable()
    val isFinancial = bool("is_financial").default(false)
    val pd1y = decimal("pd_1y", 12, 8).nullable()
    val lgd = decimal("lgd", 8, 6).default(java.math.BigDecimal("0.400000"))
    val cdsSpreadBps = decimal("cds_spread_bps", 12, 4).nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(counterpartyId)
}
