# ADR-0022: Correlation ID Propagation

## Status
Accepted

## Context
When a trade is booked, it triggers a chain of events: trade event → risk calculation → risk result → alert evaluation → notification. Debugging a failure in this chain requires correlating log entries across 5+ services. Without a shared identifier, tracing a request across services requires timestamp matching — unreliable under load.

## Decision
Propagate a `correlationId` (UUID string) across all Kafka events and HTTP requests:

- **Kafka events**: `TradeEventMessage`, `PriceEvent`, and `RiskResultEvent` all carry an optional `correlationId` field
- **HTTP requests**: The gateway reads `X-Correlation-ID` from incoming requests (or generates a UUID if absent) and logs it via MDC
- **Origin**: The first service in a chain generates the correlation ID (e.g., position-service generates it when publishing a trade event; price-service generates it for price updates)
- **Propagation**: Downstream consumers extract the correlation ID from the incoming event and pass it to outgoing events (e.g., risk-orchestrator extracts from trade event, passes to risk result event)

The gateway's `CallLogging` plugin adds `correlationId` and `userId` to the MDC for structured logging.

## Consequences

### Positive
- End-to-end request tracing across all services via a single grep/filter
- Structured logging (MDC) makes correlation IDs searchable in Grafana/Loki
- Optional field (nullable) — backward compatible with events produced before this was introduced
- Generated at the origin (not relying on external infrastructure like a tracing agent)

### Negative
- Correlation ID propagation is manual — each service must explicitly pass it through. Missing propagation silently breaks the chain.
- Not a replacement for distributed tracing (OpenTelemetry spans provide richer parent-child relationships) — it's a lightweight complement
- UUID generation at origin means the ID is meaningful only within one causal chain, not across related chains

### Alternatives Considered
- **OpenTelemetry trace context only**: Provides automatic propagation and parent-child spans, but requires instrumentation agents and infrastructure. Correlation IDs are simpler and work in Kafka events where OTel propagation is non-trivial.
- **Kafka headers**: Store correlation ID in Kafka message headers instead of the event payload. Cleaner separation, but headers are less visible in serialized event schemas and harder to inspect in debugging tools.
- **No correlation**: Rely on timestamps and topic partitioning. Unworkable when multiple trades are processed concurrently.
