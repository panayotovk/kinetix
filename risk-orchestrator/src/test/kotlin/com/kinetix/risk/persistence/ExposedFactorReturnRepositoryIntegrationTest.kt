package com.kinetix.risk.persistence

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeWithinPercentageOf
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDate

class ExposedFactorReturnRepositoryIntegrationTest : FunSpec({

    val db = DatabaseTestSetup.startAndMigrate()
    val repository: FactorReturnRepository = ExposedFactorReturnRepository(db)

    beforeEach {
        newSuspendedTransaction(db = db) { FactorReturnsTable.deleteAll() }
    }

    test("saves and retrieves a factor return by factor name and date") {
        val factorReturn = com.kinetix.risk.model.FactorReturn(
            factorName = "EQUITY_BETA",
            asOfDate = LocalDate.of(2026, 3, 24),
            returnValue = 0.012,
            source = "market_data",
        )

        repository.save(factorReturn)

        val found = repository.findByFactorAndDate("EQUITY_BETA", LocalDate.of(2026, 3, 24))
        found!!.factorName shouldBe "EQUITY_BETA"
        found.asOfDate shouldBe LocalDate.of(2026, 3, 24)
        found.returnValue.shouldBeWithinPercentageOf(0.012, 0.01)
        found.source shouldBe "market_data"
    }

    test("returns null when factor return not found") {
        val found = repository.findByFactorAndDate("EQUITY_BETA", LocalDate.of(2026, 3, 24))
        found shouldBe null
    }

    test("upserts a factor return when same composite key already exists") {
        val original = com.kinetix.risk.model.FactorReturn(
            factorName = "EQUITY_BETA",
            asOfDate = LocalDate.of(2026, 3, 24),
            returnValue = 0.010,
            source = "market_data",
        )
        val updated = original.copy(returnValue = 0.015, source = "corrected")

        repository.save(original)
        repository.save(updated)

        val found = repository.findByFactorAndDate("EQUITY_BETA", LocalDate.of(2026, 3, 24))
        found!!.returnValue.shouldBeWithinPercentageOf(0.015, 0.01)
        found.source shouldBe "corrected"
    }

    test("findByDateRange returns all factor returns in the given date range") {
        val dates = listOf(
            LocalDate.of(2026, 3, 20),
            LocalDate.of(2026, 3, 21),
            LocalDate.of(2026, 3, 22),
            LocalDate.of(2026, 3, 25),
        )
        dates.forEach { d ->
            repository.save(
                com.kinetix.risk.model.FactorReturn(
                    factorName = "RATES_DURATION",
                    asOfDate = d,
                    returnValue = 0.001,
                    source = "market_data",
                )
            )
        }

        val results = repository.findByFactorAndDateRange(
            "RATES_DURATION",
            LocalDate.of(2026, 3, 21),
            LocalDate.of(2026, 3, 22),
        )

        results shouldHaveSize 2
        results.map { it.asOfDate } shouldBe listOf(
            LocalDate.of(2026, 3, 21),
            LocalDate.of(2026, 3, 22),
        )
    }

    test("saveBatch persists multiple factor returns in a single call") {
        val batch = listOf(
            com.kinetix.risk.model.FactorReturn("EQUITY_BETA", LocalDate.of(2026, 3, 24), 0.012, "feed"),
            com.kinetix.risk.model.FactorReturn("RATES_DURATION", LocalDate.of(2026, 3, 24), -0.005, "feed"),
            com.kinetix.risk.model.FactorReturn("FX_DELTA", LocalDate.of(2026, 3, 24), 0.003, "feed"),
        )

        repository.saveBatch(batch)

        repository.findByFactorAndDate("EQUITY_BETA", LocalDate.of(2026, 3, 24)) shouldBe
            com.kinetix.risk.model.FactorReturn("EQUITY_BETA", LocalDate.of(2026, 3, 24), 0.012, "feed")
        repository.findByFactorAndDate("RATES_DURATION", LocalDate.of(2026, 3, 24))!!
            .returnValue.shouldBeWithinPercentageOf(-0.005, 0.01)
        repository.findByFactorAndDate("FX_DELTA", LocalDate.of(2026, 3, 24)) shouldBe
            com.kinetix.risk.model.FactorReturn("FX_DELTA", LocalDate.of(2026, 3, 24), 0.003, "feed")
    }
})
