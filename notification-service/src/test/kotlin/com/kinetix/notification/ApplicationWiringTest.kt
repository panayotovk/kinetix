package com.kinetix.notification

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File

/**
 * Structural assertions that lock in which Kafka consumers `moduleWithRoutes()`
 * starts at application boot. Booting the full module against a real Kafka and
 * Postgres is heavyweight; this guards against the regression flagged in the
 * 2026-04-30 spec drift audit (item A-1) where `MarketRegimeEventConsumer` was
 * defined but never wired.
 */
class ApplicationWiringTest : FunSpec({

    val applicationSource = File("src/main/kotlin/com/kinetix/notification/Application.kt")
        .readText()

    test("moduleWithRoutes constructs MarketRegimeEventConsumer") {
        applicationSource.contains("MarketRegimeEventConsumer(") shouldBe true
    }

    test("moduleWithRoutes launches MarketRegimeEventConsumer.start() on boot") {
        applicationSource shouldContain "regimeEventConsumer.start()"
    }

    test("readiness checker tracks the regime change consumer") {
        applicationSource shouldContain "regimeTracker"
    }

    test("all four event consumers are wired in moduleWithRoutes") {
        applicationSource shouldContain "riskResultConsumer.start()"
        applicationSource shouldContain "anomalyEventConsumer.start()"
        applicationSource shouldContain "limitBreachEventConsumer.start()"
        applicationSource shouldContain "regimeEventConsumer.start()"
    }
})
