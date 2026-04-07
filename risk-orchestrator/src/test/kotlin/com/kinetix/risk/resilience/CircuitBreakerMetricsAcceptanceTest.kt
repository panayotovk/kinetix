package com.kinetix.risk.resilience

import com.kinetix.common.resilience.CircuitBreaker
import com.kinetix.common.resilience.CircuitBreakerConfig
import com.kinetix.common.resilience.CircuitState
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

/**
 * Acceptance test: verifies that circuit breaker state is surfaced in the Prometheus
 * scrape output that the /metrics endpoint serves.
 *
 * The /metrics route is `call.respondText(registry.scrape())`, so asserting on
 * `registry.scrape()` is equivalent to asserting on a GET /metrics response body.
 */
class CircuitBreakerMetricsAcceptanceTest : BehaviorSpec({

    given("a Prometheus registry with a circuit breaker wired via CircuitBreakerMetrics") {

        `when`("the circuit breaker is in its initial CLOSED state") {
            val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            CircuitBreaker(
                CircuitBreakerConfig(failureThreshold = 3, name = "price-service"),
                onStateChange = CircuitBreakerMetrics.onStateChange("price-service", registry),
            )

            then("GET /metrics contains the circuit_breaker_state gauge at 0.0") {
                val scrape = registry.scrape()
                scrape shouldContain "circuit_breaker_state"
                scrape shouldContain """circuit="price-service""""
                // 0.0 = CLOSED
                scrape shouldContain "0.0"
            }
        }

        `when`("the circuit breaker opens after repeated failures") {
            val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            val cb = CircuitBreaker(
                CircuitBreakerConfig(failureThreshold = 3, name = "rates-service"),
                onStateChange = CircuitBreakerMetrics.onStateChange("rates-service", registry),
            )

            repeat(3) {
                runCatching { cb.execute { throw RuntimeException("downstream unavailable") } }
            }

            then("circuit is OPEN") {
                cb.currentState shouldBe CircuitState.OPEN
            }

            then("GET /metrics contains circuit_breaker_state gauge with value 2.0 for the circuit") {
                val scrape = registry.scrape()
                scrape shouldContain "circuit_breaker_state"
                scrape shouldContain """circuit="rates-service""""
                // 2.0 = OPEN
                scrape shouldContain "2.0"
            }

            then("GET /metrics contains circuit_breaker_transitions_total counter for CLOSED->OPEN") {
                val scrape = registry.scrape()
                scrape shouldContain "circuit_breaker_transitions_total"
                scrape shouldContain """from="CLOSED""""
                scrape shouldContain """to="OPEN""""
            }
        }

        `when`("the circuit breaker recovers through HALF_OPEN back to CLOSED") {
            var now = java.time.Instant.parse("2025-01-15T10:00:00Z")
            val clock = object : java.time.Clock() {
                override fun getZone() = java.time.ZoneId.of("UTC")
                override fun withZone(zone: java.time.ZoneId?) = this
                override fun instant() = now
            }
            val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            val cb = CircuitBreaker(
                CircuitBreakerConfig(failureThreshold = 2, resetTimeoutMs = 5_000, name = "vol-service"),
                clock = clock,
                onStateChange = CircuitBreakerMetrics.onStateChange("vol-service", registry),
            )

            repeat(2) {
                runCatching { cb.execute { throw RuntimeException("down") } }
            }
            now = now.plusMillis(6_000)
            cb.execute { "recovered" }

            then("circuit is back to CLOSED") {
                cb.currentState shouldBe CircuitState.CLOSED
            }

            then("GET /metrics reflects CLOSED state (0.0) after recovery") {
                val scrape = registry.scrape()
                scrape shouldContain "circuit_breaker_state"
                scrape shouldContain """circuit="vol-service""""
                scrape shouldContain "0.0"
            }

            then("transition counter records all three transitions") {
                val scrape = registry.scrape()
                scrape shouldContain """from="CLOSED",to="OPEN""""
                scrape shouldContain """from="OPEN",to="HALF_OPEN""""
                scrape shouldContain """from="HALF_OPEN",to="CLOSED""""
            }
        }
    }
})
