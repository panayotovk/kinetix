package com.kinetix.regulatory.seed

import com.kinetix.regulatory.historical.HistoricalScenarioPeriod
import com.kinetix.regulatory.historical.HistoricalScenarioRepository
import com.kinetix.regulatory.historical.HistoricalScenarioReturn
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.math.sin

class HistoricalScenarioSeeder(
    private val repository: HistoricalScenarioRepository,
) {
    private val log = LoggerFactory.getLogger(HistoricalScenarioSeeder::class.java)

    suspend fun seed() {
        val existing = repository.findAllPeriods()
        if (existing.isNotEmpty()) {
            log.info("Historical scenario periods already present, skipping seed")
            return
        }

        log.info("Seeding {} historical scenario periods", PERIODS.size)
        for (periodDef in PERIODS) {
            repository.savePeriod(periodDef.period)
            val returns = generateReturns(periodDef)
            repository.saveReturns(returns)
            log.info(
                "Seeded period={} with {} return records",
                periodDef.period.periodId,
                returns.size,
            )
        }
        log.info("Historical scenario seeding complete")
    }

    private fun generateReturns(def: PeriodDef): List<HistoricalScenarioReturn> {
        val start = LocalDate.parse(def.period.startDate)
        val end = LocalDate.parse(def.period.endDate)
        val returns = mutableListOf<HistoricalScenarioReturn>()

        var date = start
        var dayIndex = 0
        while (!date.isAfter(end)) {
            for ((instrumentIndex, instrumentId) in INSTRUMENTS.withIndex()) {
                // Sine wave biased toward negative (stress period) with per-instrument phase offset.
                // sin() oscillates -1..1; the bias shifts it so average is negative.
                val phase = dayIndex * 0.3 + instrumentIndex * 0.7
                val rawWave = sin(phase)
                val biasedReturn = def.bias + def.amplitude * rawWave
                val dailyReturn = BigDecimal(biasedReturn).setScale(8, RoundingMode.HALF_UP)
                returns.add(
                    HistoricalScenarioReturn(
                        periodId = def.period.periodId,
                        instrumentId = instrumentId,
                        returnDate = date.toString(),
                        dailyReturn = dailyReturn,
                    )
                )
            }
            date = date.plusDays(1)
            dayIndex++
        }
        return returns
    }

    private data class PeriodDef(
        val period: HistoricalScenarioPeriod,
        /** Mean daily return (negative = drawdown). */
        val bias: Double,
        /** Half-amplitude of the sine oscillation around the bias. */
        val amplitude: Double,
    )

    companion object {
        private val INSTRUMENTS = listOf("AAPL", "MSFT", "GOOGL", "AMZN", "TSLA")

        private val PERIODS = listOf(
            PeriodDef(
                period = HistoricalScenarioPeriod(
                    periodId = "GFC_OCT_2008",
                    name = "GFC_OCT_2008",
                    description = "Global Financial Crisis — Lehman Brothers collapse through market trough. " +
                        "Equity markets lost 50% peak-to-trough. Daily losses of 2–5% were common.",
                    startDate = "2008-09-15",
                    endDate = "2009-03-15",
                    assetClassFocus = "EQUITY",
                    severityLabel = "SEVERE",
                ),
                bias = -0.030,
                amplitude = 0.020,
            ),
            PeriodDef(
                period = HistoricalScenarioPeriod(
                    periodId = "COVID_MAR_2020",
                    name = "COVID_MAR_2020",
                    description = "COVID-19 pandemic shock — fastest bear market in history followed by rapid recovery. " +
                        "Equities fell 34% in 33 days then recovered strongly through April.",
                    startDate = "2020-02-19",
                    endDate = "2020-04-30",
                    assetClassFocus = "EQUITY",
                    severityLabel = "SEVERE",
                ),
                bias = -0.015,
                amplitude = 0.025,
            ),
            PeriodDef(
                period = HistoricalScenarioPeriod(
                    periodId = "TAPER_TANTRUM_2013",
                    name = "TAPER_TANTRUM_2013",
                    description = "Fed taper announcement shock — Bernanke's tapering hint caused 10y Treasury yields " +
                        "to jump 100bps in seven weeks. EM equities and currencies hit hardest.",
                    startDate = "2013-05-01",
                    endDate = "2013-09-30",
                    assetClassFocus = "RATES",
                    severityLabel = "MODERATE",
                ),
                bias = -0.008,
                amplitude = 0.010,
            ),
            PeriodDef(
                period = HistoricalScenarioPeriod(
                    periodId = "EURO_CRISIS_2011",
                    name = "EURO_CRISIS_2011",
                    description = "European Sovereign Debt Crisis — Italian and Spanish 10y yields surged past 7%, " +
                        "EURUSD fell sharply, and European bank stocks collapsed.",
                    startDate = "2011-07-01",
                    endDate = "2011-12-31",
                    assetClassFocus = "CREDIT",
                    severityLabel = "MODERATE",
                ),
                bias = -0.012,
                amplitude = 0.015,
            ),
        )
    }
}
