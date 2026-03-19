package com.kinetix.risk.client

import com.kinetix.common.resilience.CircuitBreaker
import com.kinetix.common.resilience.CircuitBreakerConfig
import com.kinetix.common.resilience.CircuitBreakerOpenException
import com.kinetix.common.resilience.CircuitState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class RiskEngineCircuitBreakerConfigTest : FunSpec({

    fun riskEngineCircuitBreaker(clock: Clock = Clock.systemUTC()): CircuitBreaker = CircuitBreaker(
        config = CircuitBreakerConfig(
            failureThreshold = 3,
            resetTimeoutMs = 15_000,
            halfOpenMaxCalls = 2,
            name = "risk-engine",
        ),
        clock = clock,
    )

    test("circuit opens after exactly 3 failures") {
        val cb = riskEngineCircuitBreaker()

        repeat(2) {
            runCatching { cb.execute { throw RuntimeException("failure") } }
        }
        cb.currentState shouldBe CircuitState.CLOSED

        runCatching { cb.execute { throw RuntimeException("third failure") } }
        cb.currentState shouldBe CircuitState.OPEN
    }

    test("circuit does not open on fewer than 3 failures") {
        val cb = riskEngineCircuitBreaker()

        repeat(2) {
            runCatching { cb.execute { throw RuntimeException("failure") } }
        }

        cb.currentState shouldBe CircuitState.CLOSED
    }

    test("circuit transitions to HALF_OPEN after 15 seconds") {
        var now = Instant.parse("2026-01-01T10:00:00Z")
        val mutableClock = object : Clock() {
            override fun getZone(): ZoneId = ZoneId.of("UTC")
            override fun withZone(zone: ZoneId?): Clock = this
            override fun instant(): Instant = now
        }
        val cb = riskEngineCircuitBreaker(mutableClock)

        repeat(3) {
            runCatching { cb.execute { throw RuntimeException("failure") } }
        }
        cb.currentState shouldBe CircuitState.OPEN

        // Advance 14 seconds — still OPEN
        now = now.plusMillis(14_000)
        shouldThrow<CircuitBreakerOpenException> { cb.execute { "should not run" } }

        // Advance past 15s reset timeout — transitions to HALF_OPEN
        now = now.plusMillis(1_001)
        val result = cb.execute { "recovered" }
        result shouldBe "recovered"
        cb.currentState shouldBe CircuitState.CLOSED
    }

    test("circuit allows up to 2 half-open calls before rejecting the third") {
        var now = Instant.parse("2026-01-01T10:00:00Z")
        val mutableClock = object : Clock() {
            override fun getZone(): ZoneId = ZoneId.of("UTC")
            override fun withZone(zone: ZoneId?): Clock = this
            override fun instant(): Instant = now
        }
        val cb = riskEngineCircuitBreaker(mutableClock)

        // Trip the circuit open
        repeat(3) {
            runCatching { cb.execute { throw RuntimeException("failure") } }
        }
        cb.currentState shouldBe CircuitState.OPEN

        // Advance past reset timeout
        now = now.plusMillis(16_000)

        // First half-open call — allowed (transitions to HALF_OPEN)
        runCatching { cb.execute { throw RuntimeException("still failing") } }
        // Circuit re-opens after failure in HALF_OPEN
        cb.currentState shouldBe CircuitState.OPEN
    }

    test("circuit breaker name is risk-engine") {
        val cb = riskEngineCircuitBreaker()

        repeat(3) {
            runCatching { cb.execute { throw RuntimeException("failure") } }
        }

        val ex = shouldThrow<CircuitBreakerOpenException> {
            cb.execute { "rejected" }
        }
        ex.circuitName shouldBe "risk-engine"
    }
})
