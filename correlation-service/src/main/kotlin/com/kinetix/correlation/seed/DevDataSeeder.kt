package com.kinetix.correlation.seed

import com.kinetix.common.model.CorrelationMatrix
import com.kinetix.common.model.EstimationMethod
import com.kinetix.correlation.persistence.CorrelationMatrixRepository
import org.slf4j.LoggerFactory
import java.time.Instant

class DevDataSeeder(
    private val correlationMatrixRepository: CorrelationMatrixRepository,
) {
    private val log = LoggerFactory.getLogger(DevDataSeeder::class.java)

    suspend fun seed() {
        val existing = correlationMatrixRepository.findLatest(listOf("AAPL", "MSFT"), WINDOW_DAYS)
        if (existing != null) {
            log.info("Correlation data already present, skipping seed")
            return
        }

        log.info("Seeding correlation matrix for {} instruments", LABELS.size)

        val matrix = CorrelationMatrix(
            labels = LABELS,
            values = CORRELATION_VALUES,
            windowDays = WINDOW_DAYS,
            asOfDate = AS_OF,
            method = EstimationMethod.HISTORICAL,
        )
        correlationMatrixRepository.save(matrix)

        log.info("Correlation matrix seeding complete")
    }

    companion object {
        val AS_OF: Instant = Instant.parse("2026-02-22T10:00:00Z")
        const val WINDOW_DAYS = 252

        val LABELS: List<String> = listOf(
            "AAPL", "AAPL-BOND-2030", "AAPL-C-200-20260620", "AAPL-P-180-20260620",
            "ADBE", "AMD", "AMZN", "AMZN-C-220-20260620", "AMZN-P-190-20260620",
            "AUDUSD", "BABA", "BAC", "CL", "CL-P-70-DEC26", "CRM", "CVX",
            "DE10Y", "DE2Y", "DIS",
            "EUR-ESTR-5Y", "EURGBP", "EURUSD", "EURUSD-6M", "EURUSD-P-1.08-SEP26",
            "GBPUSD", "GBPUSD-3M", "GC", "GC-C-2200-DEC26",
            "GOOGL", "GOOGL-C-190-20260620", "GOOGL-P-160-20260620",
            "GS", "GS-BOND-2029",
            "HG", "INTC",
            "JNJ", "JP10Y", "JPM", "JPM-BOND-2031",
            "KO",
            "META", "MS", "MSFT", "MSFT-BOND-2032", "MSFT-C-450-20260620", "MSFT-P-400-20260620",
            "NDX-SEP26", "NG", "NVDA", "NVDA-C-950-20260620", "NVDA-P-800-20260620",
            "NZDUSD",
            "ORCL",
            "PFE", "PL",
            "RTY-SEP26",
            "SI", "SPX-CALL-5000", "SPX-CALL-5200", "SPX-PUT-4500", "SPX-PUT-4800", "SPX-SEP26",
            "TSLA", "TSLA-C-280-20260620", "TSLA-P-220-20260620",
            "UK10Y", "UNH",
            "US10Y", "US2Y", "US30Y", "US5Y",
            "USD-SOFR-10Y", "USD-SOFR-5Y", "USDCAD", "USDCHF",
            "USDJPY", "USDJPY-3M", "USDJPY-C-155-SEP26",
            "VIX-PUT-15", "WMT", "WTI-AUG26",
            "XOM", "ZC",
        )

        private enum class Sector { TECH, FX, FIXED_INCOME, COMMODITY, DERIVATIVE, FINANCE }

        private val SECTOR_MAP: Map<String, Sector> = mapOf(
            "AAPL" to Sector.TECH, "AMZN" to Sector.TECH, "BABA" to Sector.TECH,
            "GOOGL" to Sector.TECH, "META" to Sector.TECH, "MSFT" to Sector.TECH,
            "NVDA" to Sector.TECH, "TSLA" to Sector.TECH,
            "JPM" to Sector.FINANCE,
            "EURUSD" to Sector.FX, "GBPUSD" to Sector.FX, "USDJPY" to Sector.FX,
            "GBPUSD-3M" to Sector.FX,
            "US2Y" to Sector.FIXED_INCOME, "US10Y" to Sector.FIXED_INCOME,
            "US30Y" to Sector.FIXED_INCOME, "DE10Y" to Sector.FIXED_INCOME,
            "JPM-BOND-2031" to Sector.FIXED_INCOME, "USD-SOFR-5Y" to Sector.FIXED_INCOME,
            "GC" to Sector.COMMODITY, "CL" to Sector.COMMODITY, "SI" to Sector.COMMODITY,
            "WTI-AUG26" to Sector.COMMODITY, "GC-C-2200-DEC26" to Sector.COMMODITY,
            "SPX-PUT-4500" to Sector.DERIVATIVE, "SPX-CALL-5000" to Sector.DERIVATIVE,
            "SPX-PUT-4800" to Sector.DERIVATIVE, "SPX-CALL-5200" to Sector.DERIVATIVE,
            "NVDA-C-950-20260620" to Sector.DERIVATIVE, "NVDA-P-800-20260620" to Sector.DERIVATIVE,
            "AAPL-P-180-20260620" to Sector.DERIVATIVE, "AAPL-C-200-20260620" to Sector.DERIVATIVE,
            "EURUSD-P-1.08-SEP26" to Sector.DERIVATIVE, "SPX-SEP26" to Sector.DERIVATIVE,
            "VIX-PUT-15" to Sector.DERIVATIVE,
            // New equities
            "AMD" to Sector.TECH, "INTC" to Sector.TECH,
            "CRM" to Sector.TECH, "ORCL" to Sector.TECH, "ADBE" to Sector.TECH,
            "BAC" to Sector.FINANCE, "GS" to Sector.FINANCE, "MS" to Sector.FINANCE,
            "DIS" to Sector.TECH, "KO" to Sector.TECH,
            "WMT" to Sector.TECH, "JNJ" to Sector.TECH,
            "PFE" to Sector.TECH, "UNH" to Sector.TECH,
            "XOM" to Sector.COMMODITY, "CVX" to Sector.COMMODITY,
            // New options
            "MSFT-C-450-20260620" to Sector.DERIVATIVE, "MSFT-P-400-20260620" to Sector.DERIVATIVE,
            "TSLA-C-280-20260620" to Sector.DERIVATIVE, "TSLA-P-220-20260620" to Sector.DERIVATIVE,
            "GOOGL-C-190-20260620" to Sector.DERIVATIVE, "GOOGL-P-160-20260620" to Sector.DERIVATIVE,
            "AMZN-C-220-20260620" to Sector.DERIVATIVE, "AMZN-P-190-20260620" to Sector.DERIVATIVE,
            // New bonds
            "US5Y" to Sector.FIXED_INCOME, "UK10Y" to Sector.FIXED_INCOME,
            "JP10Y" to Sector.FIXED_INCOME, "DE2Y" to Sector.FIXED_INCOME,
            "AAPL-BOND-2030" to Sector.FIXED_INCOME, "GS-BOND-2029" to Sector.FIXED_INCOME,
            "MSFT-BOND-2032" to Sector.FIXED_INCOME,
            // New FX
            "AUDUSD" to Sector.FX, "USDCAD" to Sector.FX, "USDCHF" to Sector.FX,
            "EURGBP" to Sector.FX, "NZDUSD" to Sector.FX,
            "EURUSD-6M" to Sector.FX, "USDJPY-3M" to Sector.FX,
            "USDJPY-C-155-SEP26" to Sector.DERIVATIVE,
            // New swaps
            "USD-SOFR-10Y" to Sector.FIXED_INCOME, "EUR-ESTR-5Y" to Sector.FIXED_INCOME,
            // New futures
            "NDX-SEP26" to Sector.DERIVATIVE, "RTY-SEP26" to Sector.DERIVATIVE,
            // New commodities
            "NG" to Sector.COMMODITY, "HG" to Sector.COMMODITY,
            "PL" to Sector.COMMODITY, "ZC" to Sector.COMMODITY,
            "CL-P-70-DEC26" to Sector.COMMODITY,
        )

        private val SPECIFIC_PAIRS: Map<String, Double> = mapOf(
            "AAPL:MSFT" to 0.82,
            "AAPL:GOOGL" to 0.78,
            "AMZN:GOOGL" to 0.75,
            "META:GOOGL" to 0.73,
            "MSFT:NVDA" to 0.76,
            "AAPL:NVDA" to 0.72,
            "BABA:TSLA" to 0.45,
            "CL:GC" to 0.25,
            "CL:SI" to 0.30,
            "GC:SI" to 0.65,
            "US10Y:US30Y" to 0.95,
            "US2Y:US10Y" to 0.88,
            "US2Y:US30Y" to 0.80,
            "DE10Y:US10Y" to 0.72,
            "DE10Y:US2Y" to 0.60,
            "DE10Y:US30Y" to 0.68,
            "EURUSD:GBPUSD" to 0.70,
            "EURUSD:USDJPY" to -0.40,
            "GBPUSD:USDJPY" to -0.30,
            "SPX-CALL-5000:SPX-PUT-4500" to 0.50,
            "SPX-CALL-5000:VIX-PUT-15" to -0.65,
            "SPX-PUT-4500:VIX-PUT-15" to 0.55,
            "SPX-PUT-4500:SPX-PUT-4800" to 0.95,
            "SPX-CALL-5000:SPX-CALL-5200" to 0.93,
            "NVDA-C-950-20260620:NVDA" to 0.80,
            "NVDA-P-800-20260620:NVDA" to -0.75,
            "AAPL-P-180-20260620:AAPL" to -0.72,
            // Financials
            "BAC:JPM" to 0.85,
            "BAC:GS" to 0.78,
            "GS:MS" to 0.88,
            "BAC:MS" to 0.80,
            // Tech
            "AMD:NVDA" to 0.82,
            "AMD:INTC" to 0.65,
            "CRM:ORCL" to 0.60,
            "ADBE:CRM" to 0.68,
            "ADBE:MSFT" to 0.70,
            // Energy
            "CVX:XOM" to 0.92,
            "CL:XOM" to 0.75,
            "CL:CVX" to 0.72,
            // Consumer/Healthcare
            "KO:WMT" to 0.55,
            "JNJ:PFE" to 0.60,
            "JNJ:UNH" to 0.52,
            // Bonds
            "US10Y:US5Y" to 0.92,
            "US2Y:US5Y" to 0.90,
            "US30Y:US5Y" to 0.85,
            "DE10Y:DE2Y" to 0.88,
            "UK10Y:US10Y" to 0.68,
            "JP10Y:US10Y" to 0.45,
            // FX
            "AUDUSD:NZDUSD" to 0.85,
            "USDCAD:USDJPY" to 0.30,
            "EURUSD:USDCHF" to -0.92,
            "EURGBP:EURUSD" to 0.60,
            // Option-underlying pairs
            "MSFT:MSFT-C-450-20260620" to 0.78,
            "MSFT:MSFT-P-400-20260620" to -0.72,
            "TSLA:TSLA-C-280-20260620" to 0.80,
            "TSLA:TSLA-P-220-20260620" to -0.75,
            "GOOGL:GOOGL-C-190-20260620" to 0.78,
            "GOOGL:GOOGL-P-160-20260620" to -0.72,
            "AMZN:AMZN-C-220-20260620" to 0.78,
            "AMZN:AMZN-P-190-20260620" to -0.72,
        )

        private val CROSS_SECTOR_CORRS: Map<Pair<Sector, Sector>, Double> = mapOf(
            (Sector.TECH to Sector.FX) to 0.15,
            (Sector.TECH to Sector.FIXED_INCOME) to -0.20,
            (Sector.TECH to Sector.COMMODITY) to 0.10,
            (Sector.TECH to Sector.DERIVATIVE) to 0.45,
            (Sector.TECH to Sector.FINANCE) to 0.55,
            (Sector.FX to Sector.FIXED_INCOME) to 0.25,
            (Sector.FX to Sector.COMMODITY) to 0.20,
            (Sector.FX to Sector.DERIVATIVE) to 0.05,
            (Sector.FX to Sector.FINANCE) to 0.20,
            (Sector.FIXED_INCOME to Sector.COMMODITY) to -0.15,
            (Sector.FIXED_INCOME to Sector.DERIVATIVE) to -0.10,
            (Sector.FIXED_INCOME to Sector.FINANCE) to 0.30,
            (Sector.COMMODITY to Sector.DERIVATIVE) to 0.08,
            (Sector.COMMODITY to Sector.FINANCE) to 0.15,
            (Sector.DERIVATIVE to Sector.FINANCE) to 0.40,
        )

        // Must be declared after LABELS, SECTOR_MAP, SPECIFIC_PAIRS, and CROSS_SECTOR_CORRS
        val CORRELATION_VALUES: List<Double> = buildCorrelationMatrix()

        private fun buildCorrelationMatrix(): List<Double> {
            val n = LABELS.size
            val matrix = Array(n) { DoubleArray(n) }

            for (i in 0 until n) {
                matrix[i][i] = 1.0
                for (j in i + 1 until n) {
                    val corr = baseCorrelation(LABELS[i], LABELS[j])
                    matrix[i][j] = corr
                    matrix[j][i] = corr
                }
            }

            return matrix.flatMap { it.toList() }
        }

        private fun baseCorrelation(a: String, b: String): Double {
            val sectorA = SECTOR_MAP[a] ?: Sector.TECH
            val sectorB = SECTOR_MAP[b] ?: Sector.TECH

            if (sectorA == sectorB) {
                return when (sectorA) {
                    Sector.TECH -> pairCorrelation(a, b, default = 0.70)
                    Sector.FX -> pairCorrelation(a, b, default = 0.55)
                    Sector.FIXED_INCOME -> pairCorrelation(a, b, default = 0.85)
                    Sector.COMMODITY -> pairCorrelation(a, b, default = 0.35)
                    Sector.DERIVATIVE -> pairCorrelation(a, b, default = 0.60)
                    Sector.FINANCE -> 1.0
                }
            }

            return crossSectorCorrelation(sectorA, sectorB)
        }

        private fun pairCorrelation(a: String, b: String, default: Double): Double {
            val key = if (a < b) "$a:$b" else "$b:$a"
            return SPECIFIC_PAIRS[key] ?: default
        }

        private fun crossSectorCorrelation(a: Sector, b: Sector): Double {
            val key = if (a.ordinal <= b.ordinal) a to b else b to a
            return CROSS_SECTOR_CORRS[key] ?: 0.10
        }
    }
}
