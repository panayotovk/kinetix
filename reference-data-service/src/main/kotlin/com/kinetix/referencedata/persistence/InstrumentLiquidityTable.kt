package com.kinetix.referencedata.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object InstrumentLiquidityTable : Table("instrument_liquidity") {
    val instrumentId = varchar("instrument_id", 255)
    val adv = decimal("adv", precision = 24, scale = 6)
    val bidAskSpreadBps = decimal("bid_ask_spread_bps", precision = 10, scale = 4)
    val assetClass = varchar("asset_class", 50)
    val liquidityTier = varchar("liquidity_tier", 20).default("ILLIQUID")
    val advUpdatedAt = timestampWithTimeZone("adv_updated_at")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    val advShares = decimal("adv_shares", precision = 24, scale = 6).nullable()
    val marketDepthScore = decimal("market_depth_score", precision = 10, scale = 4).nullable()
    val dataSource = varchar("source", 50).default("unknown")

    override val primaryKey = PrimaryKey(instrumentId)
}
