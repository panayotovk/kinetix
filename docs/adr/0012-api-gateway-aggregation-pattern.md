# ADR-0012: API Gateway Aggregation Pattern

## Status
Accepted

## Context
The UI needs data from multiple backend services (positions, prices, risk, notifications, regulatory, audit). Exposing each service directly to the frontend creates coupling, complicates CORS, and pushes aggregation logic into the browser.

## Decision
Use a dedicated API gateway service (`gateway/`) that aggregates all backend services behind a single HTTP endpoint for the UI. The gateway is a stateless Ktor application that proxies and composes responses from backend services via typed HTTP client interfaces.

Backend service clients are defined as interfaces (`PositionServiceClient`, `PriceServiceClient`, `RiskServiceClient`, `NotificationServiceClient`, `RegulatoryServiceClient`) with `Http*` implementations using Ktor's HTTP client. This allows mock implementations in tests.

The gateway also hosts:
- WebSocket endpoint for real-time price streaming (`PriceBroadcaster`)
- JWT authentication and RBAC enforcement
- OpenAPI/Swagger documentation
- System health aggregation (`/api/v1/system/health` — fans out health checks to all 10 backend services)

## Consequences

### Positive
- Single entry point for the UI — one base URL, one CORS configuration
- Aggregation happens server-side with low-latency internal calls instead of client-side with multiple round trips
- Backend services remain unaware of UI concerns (response shaping, auth token validation)
- Client interfaces enable comprehensive gateway testing with mock backends

### Negative
- Gateway is an additional service to deploy and monitor
- Request latency includes an extra hop (UI → gateway → backend service)
- Gateway must be updated when new backend endpoints are added

### Alternatives Considered
- **Direct service-to-UI calls**: Simpler architecture, but the UI would need to manage multiple base URLs, CORS for each service, and client-side aggregation. Authentication would need to be implemented in every service.
- **API mesh / service mesh (Envoy, Istio)**: Handles routing and mTLS, but doesn't provide response aggregation or business-level composition. Too heavy for the current scale.
- **BFF (Backend for Frontend)**: Conceptually similar; our gateway is effectively a BFF for the React UI.
