package com.kinetix.rates.persistence

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

/**
 * Verifies that the yield curve retention migration (V2) declares the correct
 * 7-year (2555-day) retention policy on all three time-series tables.
 *
 * We validate the migration SQL content directly rather than running it against
 * a database because the target deployment uses TimescaleDB — an extension that
 * is not present in the lightweight test container.
 */
class YieldCurveRetentionMigrationTest : FunSpec({

    val migrationSql: String by lazy {
        val resource = Thread.currentThread().contextClassLoader
            .getResourceAsStream("db/rates/V2__add_yield_curve_retention.sql")
            ?: error("V2__add_yield_curve_retention.sql not found on classpath")
        resource.bufferedReader().readText()
    }

    test("migration applies 2555-day retention to yield_curves") {
        migrationSql shouldContain "add_retention_policy('yield_curves', INTERVAL '2555 days')"
    }

    test("migration applies 2555-day retention to forward_curves") {
        migrationSql shouldContain "add_retention_policy('forward_curves', INTERVAL '2555 days')"
    }

    test("migration applies 2555-day retention to risk_free_rates") {
        migrationSql shouldContain "add_retention_policy('risk_free_rates', INTERVAL '2555 days')"
    }

    test("migration converts yield_curves to a hypertable") {
        migrationSql shouldContain "create_hypertable"
        migrationSql shouldContain "'yield_curves'"
    }

    test("migration does not use CREATE INDEX CONCURRENTLY (incompatible with Flyway transactions)") {
        migrationSql.uppercase() shouldNotContain "CONCURRENTLY"
    }

    test("migration drops FK constraints before hypertable conversion") {
        migrationSql shouldContain "DROP CONSTRAINT IF EXISTS fk_yield_curve_tenors_curve"
        migrationSql shouldContain "DROP CONSTRAINT IF EXISTS fk_forward_curve_points_curve"
    }
})
