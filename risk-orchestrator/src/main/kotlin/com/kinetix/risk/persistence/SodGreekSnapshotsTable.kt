package com.kinetix.risk.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object SodGreekSnapshotsTable : Table("sod_greek_snapshots") {
    val id = long("id").autoIncrement()
    val bookId = varchar("book_id", 64)
    val snapshotDate = date("snapshot_date")
    val instrumentId = varchar("instrument_id", 64)
    // SOD market state
    val sodPrice = decimal("sod_price", 20, 8)
    val sodVol = double("sod_vol").nullable()
    val sodRate = double("sod_rate").nullable()
    // First-order pricing Greeks
    val delta = double("delta").nullable()
    val gamma = double("gamma").nullable()
    val vega = double("vega").nullable()
    val theta = double("theta").nullable()
    val rho = double("rho").nullable()
    // Cross-Greeks
    val vanna = double("vanna").nullable()
    val volga = double("volga").nullable()
    val charm = double("charm").nullable()
    // Fixed-income sensitivities
    val bondDv01 = double("bond_dv01").nullable()
    val swapDv01 = double("swap_dv01").nullable()
    // Immutability lock
    val isLocked = bool("is_locked").default(false)
    val lockedAt = timestampWithTimeZone("locked_at").nullable()
    val lockedBy = varchar("locked_by", 128).nullable()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}
