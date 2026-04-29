# Kinetix

**Real-time portfolio risk management platform for institutional trading desks.**

A production-grade system covering the full risk lifecycle: trade capture, position management, live P&L, VaR/ES/Greeks computation, options pricing, stress testing, FRTB regulatory capital, counterparty exposure, and model governance. Built as a polyglot microservices monorepo with 11 Kotlin services, a Python quantitative engine, and a React trading dashboard.

## Platform at a Glance

| | |
|---|---|
| **Backend** | 11 Kotlin/Ktor microservices on JVM 21 |
| **Risk engine** | Python 3.12 (NumPy, SciPy, PyTorch), gRPC server |
| **Frontend** | React 19 + TypeScript dashboard with 11 trader/risk tabs |
| **Behavioural specs** | 24 [Allium v3](https://github.com/juxt/allium) specifications |
| **Architecture decisions** | 30 ADRs in [`docs/adr/`](docs/adr/) |
| **Reference docs** | Domain glossary in [`docs/glossary/`](docs/glossary/) |
| **Datastores** | PostgreSQL 17 / TimescaleDB (per-service schemas), Redis 7 |
| **Messaging** | Apache Kafka 3.9 (KRaft) — 13 topics with per-topic DLQs |
| **Observability** | Prometheus, Grafana, Loki, Tempo, OpenTelemetry |
| **Quality gates** | Kotest, pytest, Vitest, Playwright, Gatling, Testcontainers |

## Architecture

```
                            +-------------+
                            |     UI      |  React + TypeScript
                            +------+------+
                                   | REST / WebSocket
                            +------+------+
                            |   Gateway   |  JWT auth, rate limiting
                            +------+------+
            +-----------+----------+----------+-----------+
            |           |          |          |           |
       +----+----+  +---+---+  +---+----+  +--+-----+  +--+-----+
       |Position |  | Price |  | Risk   |  | Rates  |  |Ref Data|
       |Service  |  |Service|  |Orchstr |  | Vol    |  |        |
       +----+----+  +---+---+  +-+----+-+  | Corr   |  +--------+
            |           |        |    |    +--------+
            |       +---+---+    |    |  gRPC
            |       | Redis |    |    +-------+
            |       +-------+    |            |
            |                    |       +----+-----+
            |                    |       |   Risk   |  Python
            |                    |       |  Engine  |
            |                    |       +----------+
            +------------+-------+
                         |
            +------------+--------------------------+
            |              Apache Kafka              |
            +---+-----------+-----------+------------+
                |           |           |
           +----+----+  +---+--------+  +--+----------+
           |  Audit  |  | Regulatory |  |Notification |
           | Service |  |  Service   |  |  Service    |
           +---------+  +------------+  +-------------+
                              |
                    PostgreSQL / TimescaleDB
```

Each service owns its own database schema, communicates asynchronously via Kafka, and exposes REST endpoints through the gateway. The Python risk engine talks to the orchestrator over gRPC for low-latency valuation calls. See [`docs/adr/`](docs/adr/) for the architectural decisions behind these choices.

## Key Capabilities

### Trading and Position Management
- **Trade lifecycle** — booking, amendment, cancellation with idempotent processing
- **Pre-trade limit checks** — position, notional, and concentration limits enforced across a six-level hierarchy (Firm / Division / Desk / Book / Trader / Counterparty) via `LimitHierarchyService`, with temporary limit increases
- **Multi-currency positions** — FX rate aggregation with live rate caching and database persistence
- **Realized P&L tracking** — computed on position reduction with full audit trail
- **Order execution** — FIX protocol integration with fill deduplication, overfill protection, and execution cost analysis (slippage, market impact)
- **Prime broker reconciliation** — automated break detection with configurable thresholds

### Risk Analytics
- **VaR/ES** — parametric, historical simulation, and Monte Carlo (default 10K paths with antithetic variates for variance reduction)
- **Greeks** — Delta, Gamma, Vega, Theta, Rho via finite differences; cross-Greeks (Vanna, Volga, Charm) via analytical Black-Scholes
- **Options pricing** — Black-Scholes-Merton with continuous dividend yields
- **P&L attribution** — intraday streaming with Greek decomposition (delta, gamma, vega, theta, rho, unexplained)
- **What-if analysis** — hypothetical trade simulation with full risk re-computation
- **Factor risk decomposition** — five systematic factors (equity beta, rates duration, credit spread, FX delta, vol exposure) with OLS and analytical loading estimation
- **Cross-book aggregation** — multi-book VaR with shared-cache promotion and scheduled recalculation

### Stress Testing and Scenarios
- **Historical replay** — apply actual crisis-period returns (GFC 2008, COVID 2020, Taper Tantrum 2013, Euro Crisis 2011) to the current portfolio
- **Reverse stress testing** — minimum-norm solver (SciPy SLSQP) to find the smallest shock producing a target loss
- **Custom scenarios** — multi-factor parametric shocks with correlation override and liquidity stress factors
- **Scenario governance** — version-controlled scenarios with approval workflow (draft / pending approval / approved / retired)

### Regulatory and Compliance
- **FRTB capital** — Standardised Approach: Sensitivities-Based Method (SBM), Default Risk Charge (DRC with credit-rating PDs, seniority-adjusted LGD, maturity weighting, sector concentration), Residual Risk Add-On (RRAO)
- **VaR backtesting** — Kupiec POF and Christoffersen independence tests with Basel traffic-light zones
- **Model governance** — versioned model registry with four-stage lifecycle (draft / validated / approved / retired)
- **Regulatory submissions** — four-eyes approval workflow (preparer cannot be approver)
- **Audit trail** — SHA-256 hash-chained immutable events with 7-year TimescaleDB retention

### Market Regime Detection
- **Rule-based classifier** — four regimes (normal, elevated volatility, crisis, recovery) with debounced transitions
- **Adaptive VaR parameters** — calculation method, confidence level, and time horizon auto-adjust per regime
- **Early warning signals** — alerts at 80% of regime transition thresholds

### Counterparty Risk
- **Exposure aggregation** — gross, net, and net-net (post-collateral) exposure per counterparty
- **Netting sets** — ISDA/GMRA agreement modelling with close-out netting
- **PFE** — Monte Carlo potential future exposure at 95%/99% confidence across tenor buckets
- **CVA** — discrete approximation using CDS-implied or Basel II default probabilities

## Tech Stack

| Layer | Technology |
|---|---|
| Languages | Kotlin 2.1 (JVM 21), Python 3.12, TypeScript 5.9 |
| Backend | Ktor 3.1, Exposed ORM, Kotlinx Serialization |
| Risk engine | NumPy, SciPy, PyTorch, scikit-learn |
| Frontend | React 19, Tailwind CSS 4, Vite 7, Recharts |
| Database | PostgreSQL 17 / TimescaleDB (hypertables, continuous aggregates, retention policies) |
| Messaging | Apache Kafka 3.9 (KRaft mode), 13 topics with per-topic DLQs |
| Caching | Redis 7 (Lettuce client) |
| Inter-service | gRPC 1.70 / Protobuf 4.29 |
| Auth | Keycloak 24 (OAuth2/OIDC, role-based access) |
| Observability | Micrometer, OpenTelemetry, Prometheus, Grafana, Loki, Tempo |
| Build | Gradle 9.3 (Kotlin DSL, convention plugins), uv, npm |
| Testing | Kotest, Testcontainers, MockK, pytest, Vitest, Playwright, Gatling |
| CI/CD | GitHub Actions (parallel jobs per push) |
| Deployment | Docker, Helm, Kubernetes |

## Services

| Service | Language | Purpose |
|---|---|---|
| **Gateway** | Kotlin | API routing, JWT auth (Keycloak), rate limiting, WebSocket proxy |
| **Position Service** | Kotlin | Trade booking/amend/cancel, positions, hierarchical limit checks, P&L, order execution, prime broker reconciliation, counterparty exposure |
| **Price Service** | Kotlin | Price ingestion, TimescaleDB storage, Redis caching, Kafka publishing |
| **Risk Orchestrator** | Kotlin | VaR/ES/Greeks orchestration, cross-book aggregation, P&L attribution, hedge recommendations, what-if analysis, EOD promotion, SOD baselines |
| **Audit Service** | Kotlin | Hash-chained immutable audit trail with 7-year retention; DLQ replay |
| **Regulatory Service** | Kotlin | FRTB capital, model governance, scenario management, backtesting, regulatory submissions |
| **Notification Service** | Kotlin | Risk breach alerts, suggested actions, anomaly detection, multi-channel delivery (in-app, email, webhook) |
| **Rates Service** | Kotlin | Yield curves, risk-free rates, forward curves |
| **Reference Data Service** | Kotlin | Instruments (11 typed subtypes), divisions, desks, counterparties, dividend yields, credit spreads |
| **Volatility Service** | Kotlin | Volatility surfaces with bilinear interpolation |
| **Correlation Service** | Kotlin | Correlation matrices with Ledoit-Wolf shrinkage estimation |
| **Risk Engine** | Python | VaR, Monte Carlo, Black-Scholes, cross-Greeks, FRTB, factor model, regime detection, reverse stress (gRPC server) |
| **UI** | TypeScript | React trading and risk dashboard — 11 tabs, dark mode, CSV export, WebSocket streaming, WAI-ARIA accessibility |

## Behavioural Specifications

The platform's intended behaviour is formally documented in 24 [Allium v3](https://github.com/juxt/allium) specification files in [`specs/`](specs/), covering trading, positions, risk, alerts, audit, limits, market data, scenarios, regulatory, execution, hedge recommendations, counterparty risk, hierarchy risk, regime detection, intraday P&L, liquidity, factor model, and risk models.

Each spec declares entities with lifecycle transition graphs, state-dependent field presence, rules with pre/post-conditions, and invariants — serving as both design documentation and a verifiable contract between the spec and the implementation.

## Quick Start

### Prerequisites

- **Java 21** (Temurin)
- **Python 3.12+** with [uv](https://docs.astral.sh/uv/)
- **Node.js 22** with npm
- **Docker** and Docker Compose

### Start

```bash
./dev-up.sh        # Infrastructure + all services + UI
```

### Try it out

Once everything is up:

1. Open the dashboard at <http://localhost:5173> and log in as `trader1` / `trader1`.
2. Go to the **Trades** tab and book a buy order — for example, 1,000 shares of `AAPL` at the live price.
3. Switch to **Positions** to see the new position appear with live mark-to-market.
4. Switch to **Risk** to see VaR, ES, and Greeks recompute on the new exposure within seconds.
5. The **Alerts** tab will surface any limit warnings or breaches as you keep trading.

This exercises the full pipeline end-to-end: gateway → position-service → Kafka (`trades.lifecycle`) → risk-orchestrator → risk-engine (gRPC) → notification-service → UI WebSocket.

### Stop

```bash
./dev-down.sh
```

### URLs

| URL | Service |
|---|---|
| <http://localhost:5173> | Trading & Risk Dashboard |
| <http://localhost:8080> | Gateway API |
| <http://localhost:3000> | Grafana (admin/admin) |
| <http://localhost:9090> | Prometheus |
| <http://localhost:8180> | Keycloak (admin/admin) |

### Default Users

| Username | Password | Role |
|---|---|---|
| trader1 | trader1 | TRADER |
| risk_mgr | risk_mgr | RISK_MANAGER |
| compliance1 | compliance1 | COMPLIANCE |
| admin | admin | ADMIN |

## Testing

The full quality suite runs as parallel CI jobs on every push.

```bash
# Kotlin
./gradlew test                                    # Unit tests (all modules)
./gradlew acceptanceTest                          # Acceptance tests (route/contract)
./gradlew integrationTest                         # Integration tests (Testcontainers)
./gradlew :end2end-tests:end2EndTest              # End-to-end tests across services

# Python risk engine
cd risk-engine && uv run pytest                   # Unit + integration
cd risk-engine && uv run pytest -m unit           # Unit only

# UI
cd ui && npm run test                             # Vitest unit tests
cd ui && npx playwright test                      # Playwright E2E (browser)

# Load tests
./gradlew :load-tests:gatlingRun                  # Gatling performance tests
```

See [`CLAUDE.md`](CLAUDE.md) for the testing philosophy (TDD/BDD, coverage expectations, naming conventions).

## Project Structure

```
kinetix/
├── gateway/                 API gateway (auth, routing, rate limiting)
├── position-service/        Trade booking, positions, limits, execution, reconciliation
├── price-service/           Price ingestion pipeline
├── rates-service/           Yield curves and risk-free rates
├── reference-data-service/  Instruments, divisions, desks, counterparties
├── volatility-service/      Volatility surfaces
├── correlation-service/     Correlation matrices
├── risk-orchestrator/       Risk calculation coordinator
├── audit-service/           Hash-chained immutable audit trail
├── regulatory-service/      FRTB, model governance, scenarios, submissions
├── notification-service/    Risk breach alerts and anomaly detection
├── risk-engine/             Python quantitative engine (gRPC)
├── ui/                      React trading and risk dashboard
├── proto/                   Protobuf/gRPC service contracts
├── common/                  Shared Kotlin library
├── specs/                   Allium v3 behavioural specifications
├── end2end-tests/           End-to-end API tests
├── schema-tests/            Kafka event schema compatibility tests
├── load-tests/              Gatling performance tests
├── deploy/                  Docker, Helm, Kubernetes configs
└── docs/                    ADRs, glossary, runbooks, plans
```

## Where to Go Next

If you're new to the codebase, this is the recommended reading order:

1. **[`CLAUDE.md`](CLAUDE.md)** — project conventions, testing philosophy, design principles, and guardrails. Read this first.
2. **[`docs/glossary/`](docs/glossary/)** — domain vocabulary. [`kinetix.md`](docs/glossary/kinetix.md) covers platform-specific concepts (limit hierarchy, Kafka topics, audit chain), [`generic.md`](docs/glossary/generic.md) covers finance terminology (VaR, Greeks, FRTB).
3. **[`docs/adr/`](docs/adr/)** — 30 architecture decision records explaining *why* the platform looks the way it does (Ktor over Spring, gRPC for Python, hash-chained audit, etc.).
4. **[`specs/`](specs/)** — Allium v3 behavioural contracts. Start with `core.allium`, `trading.allium`, and `risk.allium`.
5. **[`docs/runbooks/`](docs/runbooks/)** — operational procedures.
6. **`*/README.md`** — service-level READMEs (`ui/README.md`, `risk-engine/README.md`, etc.) where they exist.

For changes:

- Follow strict TDD — write a failing test first, then make it pass.
- Every backend feature needs unit + acceptance tests; every UI feature needs Vitest + Playwright coverage.
- Don't add libraries, modify CI files, or change architecture without explicit approval — see the **Guardrails** section of `CLAUDE.md`.
