package com.kinetix.risk.resilience

import com.kinetix.common.resilience.CircuitState
import io.micrometer.core.instrument.MeterRegistry
import java.util.concurrent.atomic.AtomicInteger

/**
 * Wires a [com.kinetix.common.resilience.CircuitBreaker] to Prometheus metrics.
 *
 * Registers two instruments per circuit:
 * - `circuit_breaker_state{circuit="..."}` — a Gauge: 0=CLOSED, 1=HALF_OPEN, 2=OPEN
 * - `circuit_breaker_transitions_total{circuit="...", from="...", to="..."}` — a Counter per transition pair
 *
 * Usage: pass the returned lambda as the `onStateChange` constructor argument of [CircuitBreaker].
 * The gauge is registered immediately at factory-call time, so it reads 0.0 (CLOSED) before
 * any transition occurs.
 */
object CircuitBreakerMetrics {

    private fun CircuitState.numericValue(): Int = when (this) {
        CircuitState.CLOSED -> 0
        CircuitState.HALF_OPEN -> 1
        CircuitState.OPEN -> 2
    }

    fun onStateChange(
        circuitName: String,
        registry: MeterRegistry,
    ): (CircuitState, CircuitState) -> Unit {
        val stateValue = AtomicInteger(CircuitState.CLOSED.numericValue())

        registry.gauge(
            "circuit_breaker_state",
            listOf(io.micrometer.core.instrument.Tag.of("circuit", circuitName)),
            stateValue,
        )

        return { old, new ->
            stateValue.set(new.numericValue())
            registry.counter(
                "circuit_breaker_transitions_total",
                "circuit", circuitName,
                "from", old.name,
                "to", new.name,
            ).increment()
        }
    }
}
