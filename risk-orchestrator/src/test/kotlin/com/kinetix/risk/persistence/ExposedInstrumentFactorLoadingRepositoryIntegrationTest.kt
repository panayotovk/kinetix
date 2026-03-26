package com.kinetix.risk.persistence

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeWithinPercentageOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDate

class ExposedInstrumentFactorLoadingRepositoryIntegrationTest : FunSpec({

    val db = DatabaseTestSetup.startAndMigrate()
    val repository: InstrumentFactorLoadingRepository = ExposedInstrumentFactorLoadingRepository(db)

    beforeEach {
        newSuspendedTransaction(db = db) { InstrumentFactorLoadingsTable.deleteAll() }
    }

    fun sampleLoading(
        instrumentId: String = "AAPL",
        factorName: String = "EQUITY_BETA",
        loading: Double = 1.2,
        rSquared: Double? = 0.85,
        method: String = "OLS_REGRESSION",
        estimationDate: LocalDate = LocalDate.of(2026, 3, 24),
        estimationWindow: Int = 252,
    ) = com.kinetix.risk.model.InstrumentFactorLoading(
        instrumentId = instrumentId,
        factorName = factorName,
        loading = loading,
        rSquared = rSquared,
        method = method,
        estimationDate = estimationDate,
        estimationWindow = estimationWindow,
    )

    test("saves and retrieves an instrument factor loading") {
        repository.save(sampleLoading())

        val found = repository.findByInstrumentAndFactor("AAPL", "EQUITY_BETA")
        found shouldNotBe null
        found!!.instrumentId shouldBe "AAPL"
        found.factorName shouldBe "EQUITY_BETA"
        found.loading.shouldBeWithinPercentageOf(1.2, 0.01)
        found.rSquared?.shouldBeWithinPercentageOf(0.85, 0.01)
        found.method shouldBe "OLS_REGRESSION"
        found.estimationDate shouldBe LocalDate.of(2026, 3, 24)
        found.estimationWindow shouldBe 252
    }

    test("returns null when loading not found for instrument and factor") {
        val found = repository.findByInstrumentAndFactor("UNKNOWN", "EQUITY_BETA")
        found shouldBe null
    }

    test("upserts loading when same instrument and factor already exists") {
        repository.save(sampleLoading(loading = 1.0, estimationDate = LocalDate.of(2026, 3, 23)))
        repository.save(sampleLoading(loading = 1.2, estimationDate = LocalDate.of(2026, 3, 24)))

        val found = repository.findByInstrumentAndFactor("AAPL", "EQUITY_BETA")
        found!!.loading.shouldBeWithinPercentageOf(1.2, 0.01)
        found.estimationDate shouldBe LocalDate.of(2026, 3, 24)
    }

    test("findByInstrument returns all factor loadings for the given instrument") {
        repository.save(sampleLoading(factorName = "EQUITY_BETA", loading = 1.2))
        repository.save(sampleLoading(factorName = "RATES_DURATION", loading = -0.5))
        repository.save(sampleLoading(factorName = "VOL_EXPOSURE", loading = 0.3))

        val results = repository.findByInstrument("AAPL")
        results shouldHaveSize 3
        results.map { it.factorName }.toSet() shouldBe setOf("EQUITY_BETA", "RATES_DURATION", "VOL_EXPOSURE")
    }

    test("findByInstrument returns empty list for unknown instrument") {
        val results = repository.findByInstrument("UNKNOWN")
        results shouldHaveSize 0
    }

    test("persists null rSquared for analytical loadings") {
        repository.save(sampleLoading(rSquared = null, method = "ANALYTICAL"))

        val found = repository.findByInstrumentAndFactor("AAPL", "EQUITY_BETA")
        found!!.rSquared shouldBe null
        found.method shouldBe "ANALYTICAL"
    }

    test("isStale returns true when estimation date is before the cutoff") {
        val cutoff = LocalDate.of(2026, 3, 24)
        repository.save(sampleLoading(estimationDate = LocalDate.of(2026, 3, 20)))

        val loading = repository.findByInstrumentAndFactor("AAPL", "EQUITY_BETA")!!
        loading.isStale(cutoff) shouldBe true
    }

    test("isStale returns false when estimation date is on or after the cutoff") {
        val cutoff = LocalDate.of(2026, 3, 24)
        repository.save(sampleLoading(estimationDate = LocalDate.of(2026, 3, 24)))

        val loading = repository.findByInstrumentAndFactor("AAPL", "EQUITY_BETA")!!
        loading.isStale(cutoff) shouldBe false
    }

    test("findStaleByDate returns loadings with estimation date before the cutoff") {
        repository.save(sampleLoading(instrumentId = "AAPL", estimationDate = LocalDate.of(2026, 3, 20)))
        repository.save(sampleLoading(instrumentId = "MSFT", estimationDate = LocalDate.of(2026, 3, 22)))
        repository.save(sampleLoading(instrumentId = "GOOG", estimationDate = LocalDate.of(2026, 3, 24)))

        val stale = repository.findStaleByDate(LocalDate.of(2026, 3, 23))
        stale shouldHaveSize 2
        stale.map { it.instrumentId }.toSet() shouldBe setOf("AAPL", "MSFT")
    }
})
