package com.kinetix.risk.resilience

import com.kinetix.common.resilience.CircuitBreaker
import com.kinetix.common.resilience.CircuitBreakerConfig
import com.kinetix.common.resilience.CircuitState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry

class CircuitBreakerMetricsTest : FunSpec({

    test("gauge reads 0.0 for CLOSED state after registration") {
        val registry = SimpleMeterRegistry()
        CircuitBreaker(
            CircuitBreakerConfig(failureThreshold = 3, name = "test-cb"),
            onStateChange = CircuitBreakerMetrics.onStateChange("test-cb", registry),
        )

        val gauge = registry.find("circuit_breaker_state").tag("circuit", "test-cb").gauge()
        gauge!!.value() shouldBeExactly 0.0
    }

    test("gauge reads 2.0 (OPEN) after circuit breaker opens") {
        val registry = SimpleMeterRegistry()
        val cb = CircuitBreaker(
            CircuitBreakerConfig(failureThreshold = 2, name = "price-service"),
            onStateChange = CircuitBreakerMetrics.onStateChange("price-service", registry),
        )

        repeat(2) {
            runCatching { cb.execute { throw RuntimeException("down") } }
        }

        cb.currentState shouldBe CircuitState.OPEN
        registry.find("circuit_breaker_state").tag("circuit", "price-service").gauge()!!.value() shouldBeExactly 2.0
    }

    test("gauge reads 2.0 (OPEN) after HALF_OPEN re-trips") {
        var now = java.time.Instant.parse("2025-01-15T10:00:00Z")
        val clock = object : java.time.Clock() {
            override fun getZone() = java.time.ZoneId.of("UTC")
            override fun withZone(zone: java.time.ZoneId?) = this
            override fun instant() = now
        }
        val registry = SimpleMeterRegistry()
        val cb = CircuitBreaker(
            CircuitBreakerConfig(failureThreshold = 2, resetTimeoutMs = 5_000, name = "rates-service"),
            clock = clock,
            onStateChange = CircuitBreakerMetrics.onStateChange("rates-service", registry),
        )

        repeat(2) {
            runCatching { cb.execute { throw RuntimeException("down") } }
        }
        cb.currentState shouldBe CircuitState.OPEN

        // Advance past reset timeout so the next call trips via HALF_OPEN back to OPEN
        now = now.plusMillis(6_000)
        runCatching { cb.execute { throw RuntimeException("still down") } }

        cb.currentState shouldBe CircuitState.OPEN
        // CLOSED->OPEN, then OPEN->HALF_OPEN, then HALF_OPEN->OPEN — final is OPEN=2
        registry.find("circuit_breaker_state").tag("circuit", "rates-service").gauge()!!.value() shouldBeExactly 2.0
    }

    test("gauge reads 0.0 (CLOSED) after successful recovery from HALF_OPEN") {
        var now = java.time.Instant.parse("2025-01-15T10:00:00Z")
        val clock = object : java.time.Clock() {
            override fun getZone() = java.time.ZoneId.of("UTC")
            override fun withZone(zone: java.time.ZoneId?) = this
            override fun instant() = now
        }
        val registry = SimpleMeterRegistry()
        val cb = CircuitBreaker(
            CircuitBreakerConfig(failureThreshold = 2, resetTimeoutMs = 5_000, name = "vol-service"),
            clock = clock,
            onStateChange = CircuitBreakerMetrics.onStateChange("vol-service", registry),
        )

        repeat(2) {
            runCatching { cb.execute { throw RuntimeException("down") } }
        }
        cb.currentState shouldBe CircuitState.OPEN

        now = now.plusMillis(6_000)
        cb.execute { "recovered" }
        cb.currentState shouldBe CircuitState.CLOSED

        registry.find("circuit_breaker_state").tag("circuit", "vol-service").gauge()!!.value() shouldBeExactly 0.0
    }

    test("transition counter increments on each state change") {
        val registry = SimpleMeterRegistry()
        val cb = CircuitBreaker(
            CircuitBreakerConfig(failureThreshold = 2, name = "corr-service"),
            onStateChange = CircuitBreakerMetrics.onStateChange("corr-service", registry),
        )

        repeat(2) {
            runCatching { cb.execute { throw RuntimeException("down") } }
        }

        val counter = registry.find("circuit_breaker_transitions_total")
            .tag("circuit", "corr-service")
            .tag("from", "CLOSED")
            .tag("to", "OPEN")
            .counter()
        counter!!.count() shouldBeExactly 1.0
    }

    test("onStateChange callback is invoked with correct old and new states on CLOSED to OPEN") {
        val transitions = mutableListOf<Pair<CircuitState, CircuitState>>()
        val registry = SimpleMeterRegistry()
        val cb = CircuitBreaker(
            CircuitBreakerConfig(failureThreshold = 2, name = "ref-data"),
            onStateChange = { old, new ->
                transitions.add(old to new)
                CircuitBreakerMetrics.onStateChange("ref-data", registry).invoke(old, new)
            },
        )

        repeat(2) {
            runCatching { cb.execute { throw RuntimeException("down") } }
        }

        transitions shouldBe listOf(CircuitState.CLOSED to CircuitState.OPEN)
    }
})
