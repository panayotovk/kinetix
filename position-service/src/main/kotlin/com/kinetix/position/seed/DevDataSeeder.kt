package com.kinetix.position.seed

import com.kinetix.common.model.*
import com.kinetix.position.model.LimitDefinition
import com.kinetix.position.model.LimitLevel
import com.kinetix.position.model.LimitType
import com.kinetix.position.persistence.LimitDefinitionRepository
import com.kinetix.position.persistence.PositionRepository
import com.kinetix.position.service.BookTradeCommand
import com.kinetix.position.service.TradeBookingService
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Currency

class DevDataSeeder(
    private val tradeBookingService: TradeBookingService,
    private val positionRepository: PositionRepository,
    private val limitDefinitionRepo: LimitDefinitionRepository? = null,
) {
    private val log = LoggerFactory.getLogger(DevDataSeeder::class.java)

    suspend fun seed() {
        val existing = positionRepository.findDistinctBookIds()
        if (existing.isNotEmpty()) {
            log.info("Seed data already present ({} books), skipping", existing.size)
            return
        }

        log.info("Seeding dev data: {} trades across {} books", TRADES.size, TRADES.map { it.bookId }.distinct().size)

        for (trade in TRADES) {
            tradeBookingService.handle(trade)
        }

        // Update positions with realistic market prices (trades only set averageCost)
        for ((key, marketPrice) in MARKET_PRICES) {
            val position = positionRepository.findByKey(key.first, key.second) ?: continue
            positionRepository.save(position.markToMarket(marketPrice))
        }

        if (limitDefinitionRepo != null) {
            val existingLimits = limitDefinitionRepo.findAll()
            if (existingLimits.none { it.id.startsWith("seed-lim-") }) {
                log.info("Seeding {} limit definitions", LIMIT_DEFINITIONS.size)
                for (limit in LIMIT_DEFINITIONS) {
                    limitDefinitionRepo.save(limit)
                }
            }
        }

        log.info("Dev data seeding complete")
    }

    companion object {
        private val USD = Currency.getInstance("USD")
        private val EUR = Currency.getInstance("EUR")
        private val BASE_TIME = Instant.parse("2026-02-21T14:00:00Z")

        private fun usd(amount: String) = Money(BigDecimal(amount), USD)
        private fun eur(amount: String) = Money(BigDecimal(amount), EUR)
        private fun day(n: Long): Instant = BASE_TIME.plus(n, ChronoUnit.DAYS)

        // Book → Desk mapping (desks seeded in reference-data-service):
        //   equity-growth     → desk: equity-growth      (div: equities)
        //   tech-momentum     → desk: tech-momentum      (div: equities)
        //   emerging-markets  → desk: emerging-markets    (div: equities)
        //   fixed-income      → desk: rates-trading       (div: fixed-income-rates)
        //   multi-asset       → desk: multi-asset-strategies (div: multi-asset)
        //   macro-hedge       → desk: macro-hedge         (div: multi-asset)
        //   balanced-income   → desk: balanced-income     (div: multi-asset)
        //   derivatives-book  → desk: derivatives-trading (div: multi-asset)

        val TRADES: List<BookTradeCommand> = listOf(
            // ── equity-growth book: 5 equity trades ──
            BookTradeCommand(
                tradeId = TradeId("seed-eq-aapl-001"),
                bookId = BookId("equity-growth"),
                instrumentId = InstrumentId("AAPL"),
                assetClass = AssetClass.EQUITY,
                side = Side.BUY,
                quantity = BigDecimal("150"),
                price = usd("185.50"),
                tradedAt = BASE_TIME,
                instrumentType = "CASH_EQUITY",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-eq-googl-001"),
                bookId = BookId("equity-growth"),
                instrumentId = InstrumentId("GOOGL"),
                assetClass = AssetClass.EQUITY,
                side = Side.BUY,
                quantity = BigDecimal("80"),
                price = usd("175.20"),
                tradedAt = BASE_TIME,
                instrumentType = "CASH_EQUITY",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-eq-msft-001"),
                bookId = BookId("equity-growth"),
                instrumentId = InstrumentId("MSFT"),
                assetClass = AssetClass.EQUITY,
                side = Side.BUY,
                quantity = BigDecimal("120"),
                price = usd("420.00"),
                tradedAt = BASE_TIME,
                instrumentType = "CASH_EQUITY",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-eq-amzn-001"),
                bookId = BookId("equity-growth"),
                instrumentId = InstrumentId("AMZN"),
                assetClass = AssetClass.EQUITY,
                side = Side.BUY,
                quantity = BigDecimal("100"),
                price = usd("205.75"),
                tradedAt = BASE_TIME,
                instrumentType = "CASH_EQUITY",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-eq-tsla-001"),
                bookId = BookId("equity-growth"),
                instrumentId = InstrumentId("TSLA"),
                assetClass = AssetClass.EQUITY,
                side = Side.BUY,
                quantity = BigDecimal("200"),
                price = usd("248.30"),
                tradedAt = BASE_TIME,
                instrumentType = "CASH_EQUITY",
            ),

            // ── multi-asset book: 6 trades across asset classes ──
            BookTradeCommand(
                tradeId = TradeId("seed-ma-aapl-001"),
                bookId = BookId("multi-asset"),
                instrumentId = InstrumentId("AAPL"),
                assetClass = AssetClass.EQUITY,
                side = Side.BUY,
                quantity = BigDecimal("50"),
                price = usd("186.00"),
                tradedAt = BASE_TIME,
                instrumentType = "CASH_EQUITY",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-ma-eurusd-001"),
                bookId = BookId("multi-asset"),
                instrumentId = InstrumentId("EURUSD"),
                assetClass = AssetClass.FX,
                side = Side.BUY,
                quantity = BigDecimal("100000"),
                price = usd("1.0842"),
                tradedAt = BASE_TIME,
                instrumentType = "FX_SPOT",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-ma-us10y-001"),
                bookId = BookId("multi-asset"),
                instrumentId = InstrumentId("US10Y"),
                assetClass = AssetClass.FIXED_INCOME,
                side = Side.BUY,
                quantity = BigDecimal("500"),
                price = usd("96.75"),
                tradedAt = BASE_TIME,
                instrumentType = "GOVERNMENT_BOND",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-ma-gc-001"),
                bookId = BookId("multi-asset"),
                instrumentId = InstrumentId("GC"),
                assetClass = AssetClass.COMMODITY,
                side = Side.BUY,
                quantity = BigDecimal("10"),
                price = usd("2045.60"),
                tradedAt = BASE_TIME,
                instrumentType = "COMMODITY_FUTURE",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-ma-spx-put-001"),
                bookId = BookId("multi-asset"),
                instrumentId = InstrumentId("SPX-PUT-4500"),
                assetClass = AssetClass.DERIVATIVE,
                side = Side.BUY,
                quantity = BigDecimal("25"),
                price = usd("32.50"),
                tradedAt = BASE_TIME,
                instrumentType = "EQUITY_OPTION",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-ma-msft-001"),
                bookId = BookId("multi-asset"),
                instrumentId = InstrumentId("MSFT"),
                assetClass = AssetClass.EQUITY,
                side = Side.BUY,
                quantity = BigDecimal("75"),
                price = usd("418.50"),
                tradedAt = BASE_TIME,
                instrumentType = "CASH_EQUITY",
            ),

            // ── fixed-income book: 3 fixed income trades ──
            BookTradeCommand(
                tradeId = TradeId("seed-fi-us2y-001"),
                bookId = BookId("fixed-income"),
                instrumentId = InstrumentId("US2Y"),
                assetClass = AssetClass.FIXED_INCOME,
                side = Side.BUY,
                quantity = BigDecimal("1000"),
                price = usd("99.25"),
                tradedAt = BASE_TIME,
                instrumentType = "GOVERNMENT_BOND",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-fi-us10y-001"),
                bookId = BookId("fixed-income"),
                instrumentId = InstrumentId("US10Y"),
                assetClass = AssetClass.FIXED_INCOME,
                side = Side.BUY,
                quantity = BigDecimal("800"),
                price = usd("96.50"),
                tradedAt = BASE_TIME,
                instrumentType = "GOVERNMENT_BOND",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-fi-us30y-001"),
                bookId = BookId("fixed-income"),
                instrumentId = InstrumentId("US30Y"),
                assetClass = AssetClass.FIXED_INCOME,
                side = Side.BUY,
                quantity = BigDecimal("500"),
                price = usd("92.10"),
                tradedAt = BASE_TIME,
                instrumentType = "GOVERNMENT_BOND",
            ),

            // ── emerging-markets book: 5 positions (EM equities + FX) ──
            BookTradeCommand(
                tradeId = TradeId("seed-em-baba-001"),
                bookId = BookId("emerging-markets"),
                instrumentId = InstrumentId("BABA"),
                assetClass = AssetClass.EQUITY,
                side = Side.BUY,
                quantity = BigDecimal("400"),
                price = usd("83.20"),
                tradedAt = day(1),
                instrumentType = "CASH_EQUITY",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-em-tsla-001"),
                bookId = BookId("emerging-markets"),
                instrumentId = InstrumentId("TSLA"),
                assetClass = AssetClass.EQUITY,
                side = Side.BUY,
                quantity = BigDecimal("60"),
                price = usd("250.10"),
                tradedAt = day(1),
                instrumentType = "CASH_EQUITY",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-em-eurusd-001"),
                bookId = BookId("emerging-markets"),
                instrumentId = InstrumentId("EURUSD"),
                assetClass = AssetClass.FX,
                side = Side.BUY,
                quantity = BigDecimal("75000"),
                price = usd("1.0850"),
                tradedAt = day(1),
                instrumentType = "FX_SPOT",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-em-gbpusd-001"),
                bookId = BookId("emerging-markets"),
                instrumentId = InstrumentId("GBPUSD"),
                assetClass = AssetClass.FX,
                side = Side.BUY,
                quantity = BigDecimal("50000"),
                price = usd("1.2580"),
                tradedAt = day(2),
                instrumentType = "FX_SPOT",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-em-usdjpy-001"),
                bookId = BookId("emerging-markets"),
                instrumentId = InstrumentId("USDJPY"),
                assetClass = AssetClass.FX,
                side = Side.BUY,
                quantity = BigDecimal("80000"),
                price = usd("150.20"),
                tradedAt = day(2),
                instrumentType = "FX_SPOT",
            ),
            // Partial sell after price rise
            BookTradeCommand(
                tradeId = TradeId("seed-em-baba-002"),
                bookId = BookId("emerging-markets"),
                instrumentId = InstrumentId("BABA"),
                assetClass = AssetClass.EQUITY,
                side = Side.SELL,
                quantity = BigDecimal("100"),
                price = usd("86.50"),
                tradedAt = day(4),
                instrumentType = "CASH_EQUITY",
            ),

            // ── macro-hedge book: 6 positions (rates, commodities, FX) ──
            BookTradeCommand(
                tradeId = TradeId("seed-mh-usdjpy-001"),
                bookId = BookId("macro-hedge"),
                instrumentId = InstrumentId("USDJPY"),
                assetClass = AssetClass.FX,
                side = Side.BUY,
                quantity = BigDecimal("120000"),
                price = usd("149.80"),
                tradedAt = BASE_TIME,
                instrumentType = "FX_SPOT",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-mh-gc-001"),
                bookId = BookId("macro-hedge"),
                instrumentId = InstrumentId("GC"),
                assetClass = AssetClass.COMMODITY,
                side = Side.BUY,
                quantity = BigDecimal("15"),
                price = usd("2040.00"),
                tradedAt = BASE_TIME,
                instrumentType = "COMMODITY_FUTURE",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-mh-cl-001"),
                bookId = BookId("macro-hedge"),
                instrumentId = InstrumentId("CL"),
                assetClass = AssetClass.COMMODITY,
                side = Side.BUY,
                quantity = BigDecimal("50"),
                price = usd("76.80"),
                tradedAt = day(1),
                instrumentType = "COMMODITY_FUTURE",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-mh-si-001"),
                bookId = BookId("macro-hedge"),
                instrumentId = InstrumentId("SI"),
                assetClass = AssetClass.COMMODITY,
                side = Side.BUY,
                quantity = BigDecimal("200"),
                price = usd("23.10"),
                tradedAt = day(1),
                instrumentType = "COMMODITY_FUTURE",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-mh-de10y-001"),
                bookId = BookId("macro-hedge"),
                instrumentId = InstrumentId("DE10Y"),
                assetClass = AssetClass.FIXED_INCOME,
                side = Side.BUY,
                quantity = BigDecimal("300"),
                price = eur("97.80"),
                tradedAt = day(2),
                instrumentType = "GOVERNMENT_BOND",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-mh-spx-put-001"),
                bookId = BookId("macro-hedge"),
                instrumentId = InstrumentId("SPX-PUT-4500"),
                assetClass = AssetClass.DERIVATIVE,
                side = Side.BUY,
                quantity = BigDecimal("40"),
                price = usd("31.20"),
                tradedAt = day(2),
                instrumentType = "EQUITY_OPTION",
            ),
            // Partial sell on gold after rally
            BookTradeCommand(
                tradeId = TradeId("seed-mh-gc-002"),
                bookId = BookId("macro-hedge"),
                instrumentId = InstrumentId("GC"),
                assetClass = AssetClass.COMMODITY,
                side = Side.SELL,
                quantity = BigDecimal("5"),
                price = usd("2060.50"),
                tradedAt = day(4),
                instrumentType = "COMMODITY_FUTURE",
            ),

            // ── tech-momentum book: 4 concentrated tech positions ──
            BookTradeCommand(
                tradeId = TradeId("seed-tm-nvda-001"),
                bookId = BookId("tech-momentum"),
                instrumentId = InstrumentId("NVDA"),
                assetClass = AssetClass.EQUITY,
                side = Side.BUY,
                quantity = BigDecimal("90"),
                price = usd("885.00"),
                tradedAt = day(2),
                instrumentType = "CASH_EQUITY",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-tm-meta-001"),
                bookId = BookId("tech-momentum"),
                instrumentId = InstrumentId("META"),
                assetClass = AssetClass.EQUITY,
                side = Side.BUY,
                quantity = BigDecimal("110"),
                price = usd("502.30"),
                tradedAt = day(2),
                instrumentType = "CASH_EQUITY",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-tm-msft-001"),
                bookId = BookId("tech-momentum"),
                instrumentId = InstrumentId("MSFT"),
                assetClass = AssetClass.EQUITY,
                side = Side.BUY,
                quantity = BigDecimal("85"),
                price = usd("421.50"),
                tradedAt = day(2),
                instrumentType = "CASH_EQUITY",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-tm-googl-001"),
                bookId = BookId("tech-momentum"),
                instrumentId = InstrumentId("GOOGL"),
                assetClass = AssetClass.EQUITY,
                side = Side.BUY,
                quantity = BigDecimal("100"),
                price = usd("176.80"),
                tradedAt = day(2),
                instrumentType = "CASH_EQUITY",
            ),
            // Partial sell on META after earnings
            BookTradeCommand(
                tradeId = TradeId("seed-tm-meta-002"),
                bookId = BookId("tech-momentum"),
                instrumentId = InstrumentId("META"),
                assetClass = AssetClass.EQUITY,
                side = Side.SELL,
                quantity = BigDecimal("30"),
                price = usd("510.00"),
                tradedAt = day(6),
                instrumentType = "CASH_EQUITY",
            ),

            // ── balanced-income book: 5 positions (bonds + dividend equities) ──
            BookTradeCommand(
                tradeId = TradeId("seed-bi-us10y-001"),
                bookId = BookId("balanced-income"),
                instrumentId = InstrumentId("US10Y"),
                assetClass = AssetClass.FIXED_INCOME,
                side = Side.BUY,
                quantity = BigDecimal("600"),
                price = usd("96.60"),
                tradedAt = day(1),
                instrumentType = "GOVERNMENT_BOND",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-bi-us30y-001"),
                bookId = BookId("balanced-income"),
                instrumentId = InstrumentId("US30Y"),
                assetClass = AssetClass.FIXED_INCOME,
                side = Side.BUY,
                quantity = BigDecimal("400"),
                price = usd("92.30"),
                tradedAt = day(1),
                instrumentType = "GOVERNMENT_BOND",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-bi-de10y-001"),
                bookId = BookId("balanced-income"),
                instrumentId = InstrumentId("DE10Y"),
                assetClass = AssetClass.FIXED_INCOME,
                side = Side.BUY,
                quantity = BigDecimal("250"),
                price = eur("97.90"),
                tradedAt = day(2),
                instrumentType = "GOVERNMENT_BOND",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-bi-jpm-001"),
                bookId = BookId("balanced-income"),
                instrumentId = InstrumentId("JPM"),
                assetClass = AssetClass.EQUITY,
                side = Side.BUY,
                quantity = BigDecimal("150"),
                price = usd("208.40"),
                tradedAt = day(2),
                instrumentType = "CASH_EQUITY",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-bi-aapl-001"),
                bookId = BookId("balanced-income"),
                instrumentId = InstrumentId("AAPL"),
                assetClass = AssetClass.EQUITY,
                side = Side.BUY,
                quantity = BigDecimal("70"),
                price = usd("187.20"),
                tradedAt = day(4),
                instrumentType = "CASH_EQUITY",
            ),
            // Sell some bonds to rebalance
            BookTradeCommand(
                tradeId = TradeId("seed-bi-us30y-002"),
                bookId = BookId("balanced-income"),
                instrumentId = InstrumentId("US30Y"),
                assetClass = AssetClass.FIXED_INCOME,
                side = Side.SELL,
                quantity = BigDecimal("100"),
                price = usd("93.10"),
                tradedAt = day(6),
                instrumentType = "GOVERNMENT_BOND",
            ),

            // ── derivatives-book: 5 positions (options-heavy) ──
            BookTradeCommand(
                tradeId = TradeId("seed-db-spx-call-001"),
                bookId = BookId("derivatives-book"),
                instrumentId = InstrumentId("SPX-CALL-5000"),
                assetClass = AssetClass.DERIVATIVE,
                side = Side.BUY,
                quantity = BigDecimal("100"),
                price = usd("41.50"),
                tradedAt = day(1),
                instrumentType = "EQUITY_OPTION",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-db-vix-put-001"),
                bookId = BookId("derivatives-book"),
                instrumentId = InstrumentId("VIX-PUT-15"),
                assetClass = AssetClass.DERIVATIVE,
                side = Side.BUY,
                quantity = BigDecimal("200"),
                price = usd("3.75"),
                tradedAt = day(1),
                instrumentType = "EQUITY_OPTION",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-db-spx-put-001"),
                bookId = BookId("derivatives-book"),
                instrumentId = InstrumentId("SPX-PUT-4500"),
                assetClass = AssetClass.DERIVATIVE,
                side = Side.BUY,
                quantity = BigDecimal("75"),
                price = usd("33.00"),
                tradedAt = day(2),
                instrumentType = "EQUITY_OPTION",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-db-nvda-001"),
                bookId = BookId("derivatives-book"),
                instrumentId = InstrumentId("NVDA"),
                assetClass = AssetClass.EQUITY,
                side = Side.BUY,
                quantity = BigDecimal("50"),
                price = usd("888.00"),
                tradedAt = day(2),
                instrumentType = "CASH_EQUITY",
            ),
            BookTradeCommand(
                tradeId = TradeId("seed-db-tsla-001"),
                bookId = BookId("derivatives-book"),
                instrumentId = InstrumentId("TSLA"),
                assetClass = AssetClass.EQUITY,
                side = Side.BUY,
                quantity = BigDecimal("80"),
                price = usd("249.50"),
                tradedAt = day(4),
                instrumentType = "CASH_EQUITY",
            ),
            // Sell some calls to take profit
            BookTradeCommand(
                tradeId = TradeId("seed-db-spx-call-002"),
                bookId = BookId("derivatives-book"),
                instrumentId = InstrumentId("SPX-CALL-5000"),
                assetClass = AssetClass.DERIVATIVE,
                side = Side.SELL,
                quantity = BigDecimal("40"),
                price = usd("44.20"),
                tradedAt = day(6),
                instrumentType = "EQUITY_OPTION",
            ),
        )

        val MARKET_PRICES: Map<Pair<BookId, InstrumentId>, Money> = mapOf(
            // equity-growth
            Pair(BookId("equity-growth"), InstrumentId("AAPL")) to usd("189.25"),
            Pair(BookId("equity-growth"), InstrumentId("GOOGL")) to usd("178.90"),
            Pair(BookId("equity-growth"), InstrumentId("MSFT")) to usd("425.60"),
            Pair(BookId("equity-growth"), InstrumentId("AMZN")) to usd("210.30"),
            Pair(BookId("equity-growth"), InstrumentId("TSLA")) to usd("242.15"),
            // multi-asset
            Pair(BookId("multi-asset"), InstrumentId("AAPL")) to usd("189.25"),
            Pair(BookId("multi-asset"), InstrumentId("EURUSD")) to usd("1.0856"),
            Pair(BookId("multi-asset"), InstrumentId("US10Y")) to usd("97.10"),
            Pair(BookId("multi-asset"), InstrumentId("GC")) to usd("2058.40"),
            Pair(BookId("multi-asset"), InstrumentId("SPX-PUT-4500")) to usd("28.75"),
            Pair(BookId("multi-asset"), InstrumentId("MSFT")) to usd("425.60"),
            // fixed-income
            Pair(BookId("fixed-income"), InstrumentId("US2Y")) to usd("99.40"),
            Pair(BookId("fixed-income"), InstrumentId("US10Y")) to usd("97.10"),
            Pair(BookId("fixed-income"), InstrumentId("US30Y")) to usd("93.25"),
            // emerging-markets
            Pair(BookId("emerging-markets"), InstrumentId("BABA")) to usd("86.10"),
            Pair(BookId("emerging-markets"), InstrumentId("TSLA")) to usd("242.15"),
            Pair(BookId("emerging-markets"), InstrumentId("EURUSD")) to usd("1.0856"),
            Pair(BookId("emerging-markets"), InstrumentId("GBPUSD")) to usd("1.2620"),
            Pair(BookId("emerging-markets"), InstrumentId("USDJPY")) to usd("150.80"),
            // macro-hedge
            Pair(BookId("macro-hedge"), InstrumentId("USDJPY")) to usd("150.80"),
            Pair(BookId("macro-hedge"), InstrumentId("GC")) to usd("2058.40"),
            Pair(BookId("macro-hedge"), InstrumentId("CL")) to usd("78.30"),
            Pair(BookId("macro-hedge"), InstrumentId("SI")) to usd("23.65"),
            Pair(BookId("macro-hedge"), InstrumentId("DE10Y")) to eur("98.20"),
            Pair(BookId("macro-hedge"), InstrumentId("SPX-PUT-4500")) to usd("28.75"),
            // tech-momentum
            Pair(BookId("tech-momentum"), InstrumentId("NVDA")) to usd("892.50"),
            Pair(BookId("tech-momentum"), InstrumentId("META")) to usd("508.40"),
            Pair(BookId("tech-momentum"), InstrumentId("MSFT")) to usd("425.60"),
            Pair(BookId("tech-momentum"), InstrumentId("GOOGL")) to usd("178.90"),
            // balanced-income
            Pair(BookId("balanced-income"), InstrumentId("US10Y")) to usd("97.10"),
            Pair(BookId("balanced-income"), InstrumentId("US30Y")) to usd("93.25"),
            Pair(BookId("balanced-income"), InstrumentId("DE10Y")) to eur("98.20"),
            Pair(BookId("balanced-income"), InstrumentId("JPM")) to usd("211.80"),
            Pair(BookId("balanced-income"), InstrumentId("AAPL")) to usd("189.25"),
            // derivatives-book
            Pair(BookId("derivatives-book"), InstrumentId("SPX-CALL-5000")) to usd("43.80"),
            Pair(BookId("derivatives-book"), InstrumentId("VIX-PUT-15")) to usd("3.60"),
            Pair(BookId("derivatives-book"), InstrumentId("SPX-PUT-4500")) to usd("28.75"),
            Pair(BookId("derivatives-book"), InstrumentId("NVDA")) to usd("892.50"),
            Pair(BookId("derivatives-book"), InstrumentId("TSLA")) to usd("242.15"),
        )

        private fun limit(
            id: String,
            level: LimitLevel,
            entityId: String,
            type: LimitType,
            value: String,
            intraday: String? = null,
            overnight: String? = null,
        ) = LimitDefinition(
            id = id,
            level = level,
            entityId = entityId,
            limitType = type,
            limitValue = BigDecimal(value),
            intradayLimit = intraday?.let { BigDecimal(it) },
            overnightLimit = overnight?.let { BigDecimal(it) },
            active = true,
        )

        val LIMIT_DEFINITIONS: List<LimitDefinition> = listOf(
            // ── FIRM level ──
            limit("seed-lim-firm-notional", LimitLevel.FIRM, "FIRM", LimitType.NOTIONAL, "50000000"),
            limit("seed-lim-firm-position", LimitLevel.FIRM, "FIRM", LimitType.POSITION, "2000000"),
            limit("seed-lim-firm-concentration", LimitLevel.FIRM, "FIRM", LimitType.CONCENTRATION, "0.35"),

            // ── DESK level ──
            limit("seed-lim-desk-eq-notional", LimitLevel.DESK, "equity-growth", LimitType.NOTIONAL, "8000000"),
            limit("seed-lim-desk-tech-notional", LimitLevel.DESK, "tech-momentum", LimitType.NOTIONAL, "6000000"),
            limit("seed-lim-desk-fi-notional", LimitLevel.DESK, "rates-trading", LimitType.NOTIONAL, "12000000"),
            limit("seed-lim-desk-ma-notional", LimitLevel.DESK, "multi-asset-strategies", LimitType.NOTIONAL, "10000000"),
            limit("seed-lim-desk-mh-notional", LimitLevel.DESK, "macro-hedge", LimitType.NOTIONAL, "8500000"),
            limit("seed-lim-desk-em-notional", LimitLevel.DESK, "emerging-markets", LimitType.NOTIONAL, "7000000"),
            limit("seed-lim-desk-bi-notional", LimitLevel.DESK, "balanced-income", LimitType.NOTIONAL, "9000000"),
            limit("seed-lim-desk-db-notional", LimitLevel.DESK, "derivatives-trading", LimitType.NOTIONAL, "5000000"),

            // ── BOOK level — calibrated to produce visible utilisation on seed positions ──
            limit("seed-lim-book-eq-notional", LimitLevel.BOOK, "equity-growth", LimitType.NOTIONAL, "500000",
                intraday = "550000", overnight = "480000"),
            limit("seed-lim-book-tech-notional", LimitLevel.BOOK, "tech-momentum", LimitType.NOTIONAL, "210000",
                intraday = "230000", overnight = "200000"),
            limit("seed-lim-book-tech-conc", LimitLevel.BOOK, "tech-momentum", LimitType.CONCENTRATION, "0.40"),
            limit("seed-lim-book-em-notional", LimitLevel.BOOK, "emerging-markets", LimitType.NOTIONAL, "350000"),
            limit("seed-lim-book-fi-notional", LimitLevel.BOOK, "fixed-income", LimitType.NOTIONAL, "500000"),
            limit("seed-lim-book-ma-notional", LimitLevel.BOOK, "multi-asset", LimitType.NOTIONAL, "600000"),
            limit("seed-lim-book-mh-notional", LimitLevel.BOOK, "macro-hedge", LimitType.NOTIONAL, "400000"),
            limit("seed-lim-book-bi-notional", LimitLevel.BOOK, "balanced-income", LimitType.NOTIONAL, "350000"),
            limit("seed-lim-book-db-notional", LimitLevel.BOOK, "derivatives-book", LimitType.NOTIONAL, "75000",
                intraday = "85000", overnight = "60000"),
            limit("seed-lim-book-db-conc", LimitLevel.BOOK, "derivatives-book", LimitType.CONCENTRATION, "0.40"),
        )
    }
}
