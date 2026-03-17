# ADR-0016: WebSocket for Real-Time UI Updates

## Status
Accepted

## Context
The UI needs real-time market data updates without polling. Price ticks arrive via Kafka, but the browser cannot consume Kafka directly. Options: WebSocket, Server-Sent Events (SSE), long polling.

## Decision
Use Ktor's WebSocket support in the gateway to push real-time price updates to the UI. The `PriceBroadcaster` class manages instrument-level subscriptions:

- Clients connect via WebSocket and subscribe to specific instrument IDs
- `PriceBroadcaster` maintains a `ConcurrentHashMap<instrumentId, Set<WebSocketServerSession>>`
- When a price tick arrives, it is serialized to JSON and broadcast to all sessions subscribed to that instrument
- Dead sessions are detected (send failure) and automatically removed
- WebSocket ping/pong: 30s ping period, 10s timeout

The UI implements auto-reconnect with exponential backoff (max 20 attempts) and displays a "Reconnecting..." banner during disconnection.

## Consequences

### Positive
- True push — no polling overhead, updates arrive within milliseconds of the Kafka event
- Instrument-level subscriptions avoid sending irrelevant data to clients
- Dead session cleanup prevents memory leaks from disconnected clients
- Built into Ktor — no additional WebSocket library needed

### Negative
- WebSocket connections are stateful — complicates horizontal scaling (mitigated by sticky sessions or a pub/sub backplane)
- Each connected client holds a server-side session in memory
- WebSocket protocol can be blocked by some corporate proxies (fallback to SSE not implemented)

### Alternatives Considered
- **Server-Sent Events (SSE)**: Simpler (HTTP-based, unidirectional), but lacks bidirectional communication for subscription management. Would require separate subscribe/unsubscribe endpoints.
- **Long polling**: Works through any proxy, but higher latency and server load due to repeated connection setup.
- **Direct Kafka consumption (kafka.js)**: Exposes Kafka to the browser, which is a security and operational concern.
