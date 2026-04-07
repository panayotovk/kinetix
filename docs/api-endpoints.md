# Kinetix API Endpoints

## Service Map

```
                                    ┌─────────────────────────────┐
                                    │           UI (5173)          │
                                    │     React / Vite / Tailwind  │
                                    └──────────┬──────────────────┘
                                               │ HTTP + WebSocket
                                               ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                              GATEWAY (8080)                                      │
│                                                                                  │
│  Books/Trades      Risk/VaR           Regulatory      Notifications    System    │
│  ──────────        ──────────         ──────────       ──────────      ────────  │
│  GET  /books       POST/GET /var/{b}  POST /frtb/{b}   GET  /rules     GET /hth  │
│  GET/POST /trades  POST /stress/{b}   POST /report/{b}  POST /rules    GET /sys  │
│  GET  /positions   GET  /hierarchy/.. ────────────────  DEL  /rules/{} GET /met  │
│  GET  /summary     POST /cross-book   Stress Scenarios  GET  /alerts   ─────────  │
│  ──────────        GET  /budgets      ────────────────  GET  /escalat.  WebSocket │
│  WebSockets        GET  /regime       GET/POST/PATCH   POST /ack        ────────  │
│  ──────────        GET  /krd/{b}      /stress-scenarios WS  /ws/alerts  WS/price  │
│  WS /ws/prices     GET  /jobs/{b}     GET/POST/PATCH   ──────────────   WS/pnl    │
│  WS /ws/pnl        GET  /compare/{b}  /models                           WS/alert  │
│  WS /ws/alerts     GET  /eod-timeline GET/POST/PATCH                              │
│                    POST /what-if      /submissions                                 │
└───┬──────────┬──────────┬──────────┬─────────┬─────────┬─────────┬───────────────┘
    │          │          │          │         │         │         │
    ▼          ▼          ▼          ▼         ▼         ▼         ▼
┌────────┐┌────────┐┌────────┐┌──────────┐┌───────┐┌────────┐┌──────────┐
│Position││ Price  ││  Risk  ││Notific.  ││ Rates ││Ref Data││Volatility│
│Service ││Service ││Orchest.││Service   ││Service││Service ││Service   │
│ (8081) ││ (8082) ││ (8083) ││ (8086)   ││ (8088)││ (8089) ││ (8090)   │
└────────┘└────────┘└───┬────┘└──────────┘└───────┘└────────┘└──────────┘
                        │                                        ┌──────────┐
                        │ gRPC                                   │Correlat. │
                        ▼                                        │Service   │
                 ┌─────────────┐  ┌─────────┐ ┌──────────┐      │ (8091)   │
                 │ Risk Engine │  │  Audit  │ │Regulatory│      └──────────┘
                 │  (50051)    │  │ Service │ │ Service  │
                 │  Python     │  │ (8084)  │ │ (8085)   │
                 └─────────────┘  └─────────┘ └──────────┘
```

---

## Endpoints by Service

### Gateway (port 8080) — API Router & Auth

The gateway proxies all client requests to backend services, enforces JWT authentication, and manages WebSocket connections. All `/api/v1/` routes require a valid JWT unless `GATEWAY_AUTH_ENABLED=false`.

**Infrastructure**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/health` | Service health check |
| `GET` | `/metrics` | Prometheus metrics |
| `GET` | `/openapi.json` | OpenAPI specification |
| `GET` | `/swagger` | Swagger UI |
| `GET` | `/api/v1/system/health` | System-wide health — polls all backend services (public, no auth) |

**WebSockets** (JWT validated on connect)

| Protocol | Path | Description |
|----------|------|-------------|
| `WS` | `/ws/prices` | Real-time price stream (subscribe/unsubscribe by instrumentId) |
| `WS` | `/ws/pnl` | Real-time intraday P&L stream (Kafka `intraday.pnl` broadcast) |
| `WS` | `/ws/alerts` | Real-time alert notification stream |

---

**Books & Positions** (→ Position Service · permission: `READ_PORTFOLIOS` / `READ_POSITIONS` / `WRITE_TRADES`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/books` | List all book IDs |
| `GET` | `/api/v1/books/{bookId}/trades` | Trade history for a book |
| `POST` | `/api/v1/books/{bookId}/trades` | Book a new trade (`WRITE_TRADES`) |
| `GET` | `/api/v1/books/{bookId}/positions` | Positions for a book |
| `GET` | `/api/v1/books/{bookId}/summary` | Multi-currency portfolio summary (`baseCurrency` query param) |

**Strategies** (→ Position Service · permission: `READ_PORTFOLIOS`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/books/{bookId}/strategies` | Create a trading strategy |
| `GET` | `/api/v1/books/{bookId}/strategies` | List strategies for a book |
| `GET` | `/api/v1/books/{bookId}/strategies/{strategyId}` | Get strategy by ID |
| `POST` | `/api/v1/books/{bookId}/strategies/{strategyId}/trades` | Book a trade linked to a strategy |

**Prices** (→ Price Service · permission: `READ_PORTFOLIOS`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/prices/{instrumentId}/latest` | Latest price for an instrument |
| `GET` | `/api/v1/prices/{instrumentId}/history` | Price history (`from`, `to` ISO-8601 params, required) |

**VaR & Risk** (→ Risk Orchestrator · permission: `CALCULATE_RISK`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/risk/var/{bookId}` | Calculate VaR (`calculationType`, `confidenceLevel`, `timeHorizonDays`, `numSimulations`, `requestedOutputs`) |
| `GET` | `/api/v1/risk/var/{bookId}` | Latest cached VaR result (`valuationDate` YYYY-MM-DD for historical snapshot) |
| `GET` | `/api/v1/risk/var/{bookId}/intraday` | Intraday VaR timeline series (`from`, `to` ISO-8601 instants, required) |
| `POST` | `/api/v1/risk/var/cross-book` | Cross-book aggregated VaR (bookIds in body; multi-book access check) |
| `GET` | `/api/v1/risk/var/cross-book/{groupId}` | Cached cross-book VaR result |
| `POST` | `/api/v1/risk/var/cross-book/stressed` | Stressed cross-book VaR (correlation spike scenario) |
| `POST` | `/api/v1/risk/what-if/{bookId}` | What-if analysis with hypothetical trades |
| `POST` | `/api/v1/risk/what-if/{bookId}/rebalance` | Rebalancing what-if analysis |
| `GET` | `/api/v1/risk/positions/{bookId}` | Position-level risk breakdown (`valuationDate` for historical) |
| `POST` | `/api/v1/risk/dependencies/{bookId}` | Discover market data dependencies |
| `GET` | `/api/v1/risk/pnl/intraday/{bookId}` | Intraday P&L series (`from`, `to` ISO-8601 instants, required) |
| `GET` | `/api/v1/risk/pnl-attribution/{bookId}` | P&L attribution (`date` YYYY-MM-DD) |
| `POST` | `/api/v1/risk/pnl-attribution/{bookId}/compute` | Trigger P&L attribution computation |
| `POST` | `/api/v1/risk/reports/cro` | Generate CRO report |

**Liquidity & Factor Risk** (→ Risk Orchestrator · permission: `CALCULATE_RISK`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/books/{bookId}/liquidity-risk` | Calculate liquidity risk (`baseVar` in body) |
| `GET` | `/api/v1/books/{bookId}/liquidity-risk/latest` | Latest liquidity risk result |
| `GET` | `/api/v1/books/{bookId}/liquidity-risk` | Liquidity risk history (`limit` query param) |
| `GET` | `/api/v1/books/{bookId}/factor-risk/latest` | Latest factor risk decomposition |
| `GET` | `/api/v1/books/{bookId}/factor-risk` | Factor risk history (`limit` query param) |

**Hierarchy & Budget** (→ Risk Orchestrator · permission: `CALCULATE_RISK`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/risk/hierarchy/{level}/{entityId}` | Risk at hierarchy node (`level`: FIRM/DIVISION/DESK/BOOK) |
| `GET` | `/api/v1/risk/budgets` | List risk budgets (`level`, `entityId` query params) |
| `POST` | `/api/v1/risk/budgets` | Create a risk budget |
| `GET` | `/api/v1/risk/budgets/{id}` | Get risk budget by ID |
| `DELETE` | `/api/v1/risk/budgets/{id}` | Delete a risk budget |

**Stress Tests** (→ Risk Orchestrator · permission: `READ_RISK`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/risk/stress/{bookId}` | Run stress test with custom shocks |
| `POST` | `/api/v1/risk/stress/{bookId}/batch` | Run all stress scenarios, ranked by worst P&L |
| `GET` | `/api/v1/risk/stress/scenarios` | List available stress test scenario names |
| `POST` | `/api/v1/risk/stress/{bookId}/historical-replay` | Historical scenario replay with instrument returns |
| `POST` | `/api/v1/risk/stress/{bookId}/reverse` | Reverse stress test (find minimum shock vector for target loss) |
| `POST` | `/api/v1/risk/greeks/{bookId}` | Calculate portfolio Greeks (Delta, Gamma, Vega, Theta, Rho) |

**Valuation Job History** (→ Risk Orchestrator · permission: `READ_RISK`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/risk/jobs/{bookId}` | List valuation jobs (`limit`, `offset`, `from`, `to`, `valuationDate`) |
| `GET` | `/api/v1/risk/jobs/{bookId}/chart` | Aggregated chart data (`from`, `to` ISO-8601 instants, required) |
| `GET` | `/api/v1/risk/jobs/detail/{jobId}` | Valuation job detail |
| `PATCH` | `/api/v1/risk/jobs/{jobId}/label` | Promote/demote a job's EOD label (`PROMOTE_EOD_RUN` permission) |
| `GET` | `/api/v1/risk/jobs/{bookId}/official-eod` | Official EOD designation for a book and date (`date` YYYY-MM-DD) |

**SOD Snapshot** (→ Risk Orchestrator · permission: `CALCULATE_RISK`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/risk/sod-snapshot/{bookId}/status` | SOD baseline status |
| `POST` | `/api/v1/risk/sod-snapshot/{bookId}` | Create manual SOD snapshot (`jobId` optional query param) |
| `DELETE` | `/api/v1/risk/sod-snapshot/{bookId}` | Reset SOD baseline |

**EOD & Run Comparison** (→ Risk Orchestrator · permission: `CALCULATE_RISK`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/risk/eod-timeline/{bookId}` | EOD history timeline (`from`, `to` YYYY-MM-DD, required) |
| `POST` | `/api/v1/risk/compare/{bookId}` | Compare two valuation runs by job ID (`baseJobId`, `targetJobId` in body) |
| `GET` | `/api/v1/risk/compare/{bookId}/day-over-day` | Day-over-day VaR comparison (`targetDate`, `baseDate`) |
| `POST` | `/api/v1/risk/compare/{bookId}/day-over-day/attribution` | Day-over-day VaR attribution (expensive; `targetDate`, `baseDate`) |
| `POST` | `/api/v1/risk/compare/{bookId}/model` | Compare VaR across two model configurations |
| `GET` | `/api/v1/risk/compare/{bookId}/market-data-quant` | Quantitative diff for a market data item between two manifests (`dataType`, `instrumentId`, `baseManifestId`, `targetManifestId`) |

**Market Regime & Hedge** (→ Risk Orchestrator · permission: `READ_RISK`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/risk/regime/current` | Current market regime and adaptive VaR parameters |
| `GET` | `/api/v1/risk/regime/history` | Market regime history (`limit` query param, default 50) |
| `POST` | `/api/v1/risk/hedge-suggest/{bookId}` | Generate hedge recommendations |
| `GET` | `/api/v1/risk/hedge-suggest/{bookId}` | List latest hedge recommendations (`limit` query param) |
| `GET` | `/api/v1/risk/hedge-suggest/{bookId}/{id}` | Get specific hedge recommendation |

**Counterparty Risk** (→ Risk Orchestrator · permission: `READ_RISK`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/counterparty-risk` | All counterparty exposures |
| `GET` | `/api/v1/counterparty-risk/{counterpartyId}` | Exposure for a specific counterparty |
| `GET` | `/api/v1/counterparty-risk/{counterpartyId}/history` | Exposure history (`limit` query param) |
| `POST` | `/api/v1/counterparty-risk/{counterpartyId}/pfe` | Compute Potential Future Exposure |
| `POST` | `/api/v1/counterparty-risk/{counterpartyId}/cva` | Compute Credit Valuation Adjustment |
| `GET` | `/api/v1/counterparty/{counterpartyId}/sa-ccr` | SA-CCR exposure (`collateral` query param) |

**Key Rate Duration & Margin** (→ Risk Orchestrator · permission: `CALCULATE_RISK`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/risk/krd/{bookId}` | Key Rate Durations for a book |
| `GET` | `/api/v1/books/{bookId}/margin` | Margin estimate (`previousMTM` optional query param) |

**Benchmark Attribution** (→ Risk Orchestrator · permission: `CALCULATE_RISK`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/books/{bookId}/attribution` | Brinson attribution vs benchmark (`benchmarkId` required, `asOfDate` optional) |

**Reports** (→ Risk Orchestrator · permission: `CALCULATE_RISK`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/reports/templates` | List report templates |
| `POST` | `/api/v1/reports/generate` | Generate a report (template + params in body) |
| `GET` | `/api/v1/reports/{outputId}` | Get report output by ID |
| `GET` | `/api/v1/reports/{outputId}/csv` | Download report output as CSV |

**Regulatory** (→ Risk Orchestrator · permission: `READ_REGULATORY`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/regulatory/frtb/{bookId}` | Calculate FRTB capital charges |
| `POST` | `/api/v1/regulatory/report/{bookId}` | Generate regulatory report (CSV or XBRL; `format` in body) |

**Backtest** (→ Regulatory Service · permission: `MANAGE_SCENARIOS`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/regulatory/backtest/compare` | Compare two backtest results (`baseId`, `targetId` query params) |

**Stress Scenarios** (→ Regulatory Service · permission: `MANAGE_SCENARIOS`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/stress-scenarios` | List all stress scenarios |
| `GET` | `/api/v1/stress-scenarios/approved` | List approved stress scenarios |
| `POST` | `/api/v1/stress-scenarios` | Create a new stress scenario |
| `PATCH` | `/api/v1/stress-scenarios/{id}/submit` | Submit scenario for approval |
| `PATCH` | `/api/v1/stress-scenarios/{id}/approve` | Approve a stress scenario |
| `PATCH` | `/api/v1/stress-scenarios/{id}/retire` | Retire a stress scenario |

**Volatility Surfaces** (→ Volatility Service · permission: `READ_RISK`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/volatility/{instrumentId}/surface` | Latest volatility surface |
| `GET` | `/api/v1/volatility/{instrumentId}/surface/diff` | Day-over-day vol surface diff (`compareDate` ISO-8601 instant, required) |

**Execution** (→ Position Service · permission: `READ_POSITIONS`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/orders` | Submit order for execution (`WRITE_TRADES`) |
| `POST` | `/api/v1/risk/pre-trade-check` | Pre-trade risk check without booking (< 100 ms target; `READ_POSITIONS`) |
| `GET` | `/api/v1/execution/cost/{bookId}` | Execution cost analysis for a book |
| `GET` | `/api/v1/execution/reconciliation/{bookId}` | Prime broker reconciliation history |
| `POST` | `/api/v1/execution/reconciliation/{bookId}/statements` | Upload prime broker statement and run reconciliation |

**Instruments** (→ Reference Data Service · permission: `READ_POSITIONS`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/instruments` | List instruments (`type`, `assetClass` query filters) |
| `GET` | `/api/v1/instruments/{id}` | Get instrument by ID |

**Data Quality** (→ aggregated probe · permission: `READ_POSITIONS`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/data-quality/status` | Aggregated data quality status (price freshness, position count, risk completeness, FIX connectivity) |

**Audit** (→ Audit Service · permission: `READ_AUDIT`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/audit/events` | Audit events (`bookId`, `afterId` cursor, `limit` query params) |
| `GET` | `/api/v1/audit/verify` | Verify audit chain hash integrity |

**Notifications** (→ Notification Service · permission: `READ_ALERTS`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/notifications/rules` | List alert rules |
| `POST` | `/api/v1/notifications/rules` | Create an alert rule |
| `DELETE` | `/api/v1/notifications/rules/{ruleId}` | Delete an alert rule |
| `GET` | `/api/v1/notifications/alerts` | Recent alerts (`limit`, `status` query params) |
| `GET` | `/api/v1/notifications/alerts/escalated` | Escalated alerts |
| `GET` | `/api/v1/notifications/alerts/{alertId}/contributors` | Position breakdown at alert breach time |
| `POST` | `/api/v1/notifications/alerts/{alertId}/acknowledge` | Acknowledge an alert (`acknowledgedBy`, `notes` in body) |

**Demo Admin** (no auth — activated only when `DEMO_ADMIN_KEY` and `DEMO_RESET_TOKEN` env vars are set; `X-Demo-Admin-Key` header required)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/admin/demo-reset` | Reset position, audit, and risk state for demo environment |

---

### Position Service (port 8081) — Trade Booking & Positions

**Common**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/health` | Liveness check |
| `GET` | `/health/ready` | Readiness check (DB + Kafka consumers) |
| `GET` | `/metrics` | Prometheus metrics |
| `GET` | `/openapi.json` | OpenAPI specification |
| `GET` | `/swagger` | Swagger UI |

**Books & Trades**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/books` | List all book IDs |
| `GET` | `/api/v1/books/{bookId}/trades` | Trade history for a book |
| `POST` | `/api/v1/books/{bookId}/trades` | Book a new trade |
| `PUT` | `/api/v1/books/{bookId}/trades/{tradeId}` | Amend a trade |
| `DELETE` | `/api/v1/books/{bookId}/trades/{tradeId}` | Cancel a trade |
| `GET` | `/api/v1/books/{bookId}/positions` | Positions for a book |
| `GET` | `/api/v1/books/{bookId}/summary` | Multi-currency portfolio summary (`baseCurrency` query param) |

**Strategies**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/books/{bookId}/strategies` | Create a trading strategy |
| `GET` | `/api/v1/books/{bookId}/strategies` | List strategies for a book |
| `GET` | `/api/v1/books/{bookId}/strategies/{strategyId}` | Get strategy by ID |
| `POST` | `/api/v1/books/{bookId}/strategies/{strategyId}/trades` | Book a trade linked to a strategy |

**Limits**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/limits` | List all limit definitions |
| `POST` | `/api/v1/limits` | Create a limit definition (FIRM/DESK/TRADER/COUNTERPARTY levels) |
| `PUT` | `/api/v1/limits/{id}` | Update a limit definition |
| `POST` | `/api/v1/limits/{id}/temporary-increase` | Request a temporary limit increase |

**Orders & Execution**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/orders` | Submit order for execution |
| `POST` | `/api/v1/risk/pre-trade-check` | Pre-trade limit check without booking |
| `GET` | `/api/v1/execution/cost/{bookId}` | Execution cost analysis |
| `GET` | `/api/v1/execution/reconciliation/{bookId}` | Prime broker reconciliation history |
| `POST` | `/api/v1/execution/reconciliation/{bookId}/statements` | Upload prime broker statement |
| `PATCH` | `/api/v1/execution/reconciliation-breaks/{reconciliationId}/{instrumentId}/status` | Update reconciliation break status |
| `GET` | `/api/v1/fix/sessions` | List FIX session connectivity status |
| `PATCH` | `/api/v1/fix/sessions/{sessionId}/status` | Update FIX session status |

**Book Hierarchy**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/book-hierarchy` | List book-to-desk mappings (`deskId` filter) |
| `POST` | `/api/v1/book-hierarchy` | Create a book hierarchy mapping |
| `GET` | `/api/v1/book-hierarchy/{bookId}` | Get hierarchy mapping for a book |
| `DELETE` | `/api/v1/book-hierarchy/{bookId}` | Delete a book hierarchy mapping |

**Counterparty (internal)**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/counterparties/{counterpartyId}/trades` | Trades for a counterparty (SA-CCR position assembly) |
| `GET` | `/api/v1/counterparty-exposure` | Counterparty exposure aggregation (`bookId` query param) |
| `GET` | `/api/v1/counterparties/{counterpartyId}/instrument-netting-sets` | InstrumentId → nettingSetId mapping |

**Collateral**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/counterparties/{counterpartyId}/collateral` | Post collateral |
| `GET` | `/api/v1/counterparties/{counterpartyId}/collateral` | List collateral balances |
| `GET` | `/api/v1/counterparties/{counterpartyId}/collateral/net` | Net collateral position |

**Internal**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/internal/position/demo-reset` | Reset all position and trade data (demo mode; `X-Demo-Reset-Token` required) |

**Kafka:** Publishes to `trades.lifecycle` · Consumes from `price.updates`

---

### Price Service (port 8082) — Price Ingestion & Distribution

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/health` | Liveness check |
| `GET` | `/health/ready` | Readiness check |
| `GET` | `/metrics` | Prometheus metrics |
| `GET` | `/openapi.json` | OpenAPI specification |
| `GET` | `/swagger` | Swagger UI |
| `GET` | `/api/v1/prices/{instrumentId}/latest` | Latest price for an instrument |
| `GET` | `/api/v1/prices/{instrumentId}/history` | Price history (`from`, `to` ISO-8601, required) |
| `POST` | `/api/v1/prices/ingest` | Ingest a new price point |

**Kafka:** Publishes to `price.updates`

---

### Risk Orchestrator (port 8083) — VaR, Stress Tests, FRTB

**Common**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/health` | Liveness check |
| `GET` | `/health/ready` | Readiness check (DB + Kafka consumers, including DLQ counts) |
| `GET` | `/metrics` | Prometheus metrics |
| `GET` | `/openapi.json` | OpenAPI specification |
| `GET` | `/swagger` | Swagger UI |

**VaR**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/risk/var/{bookId}` | Calculate VaR (`calculationType`, `confidenceLevel`, `timeHorizonDays`, `numSimulations`, `requestedOutputs`) |
| `GET` | `/api/v1/risk/var/{bookId}` | Latest cached VaR result (`valuationDate` for historical) |
| `POST` | `/api/v1/risk/var/cross-book` | Cross-book aggregated VaR |
| `GET` | `/api/v1/risk/var/cross-book/{groupId}` | Cached cross-book VaR |
| `POST` | `/api/v1/risk/var/cross-book/stressed` | Stressed cross-book VaR |
| `GET` | `/api/v1/risk/var/{bookId}/intraday` | Intraday VaR timeline (`from`, `to`) |

**Stress Tests**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/risk/stress/{bookId}` | Run stress test |
| `POST` | `/api/v1/risk/stress/{bookId}/batch` | Run all stress scenarios, ranked by worst P&L |
| `GET` | `/api/v1/risk/stress/scenarios` | List scenario names |
| `POST` | `/api/v1/risk/stress/{bookId}/historical-replay` | Historical scenario replay |
| `POST` | `/api/v1/risk/stress/{bookId}/reverse` | Reverse stress test |
| `POST` | `/api/v1/risk/greeks/{bookId}` | Calculate portfolio Greeks |

**Position Risk & Attribution**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/risk/positions/{bookId}` | Position-level risk breakdown (`valuationDate`) |
| `GET` | `/api/v1/risk/pnl-attribution/{bookId}` | P&L attribution (`date`) |
| `POST` | `/api/v1/risk/pnl-attribution/{bookId}/compute` | Compute P&L attribution |
| `GET` | `/api/v1/risk/pnl/intraday/{bookId}` | Intraday P&L series (`from`, `to`) |
| `GET` | `/api/v1/books/{bookId}/factor-risk/latest` | Latest factor risk |
| `GET` | `/api/v1/books/{bookId}/factor-risk` | Factor risk history |
| `GET` | `/api/v1/books/{bookId}/attribution` | Brinson attribution (`benchmarkId`, `asOfDate`) |

**What-If**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/risk/what-if/{bookId}` | What-if analysis |
| `POST` | `/api/v1/risk/what-if/{bookId}/rebalance` | Rebalancing what-if |

**Dependencies**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/risk/dependencies/{bookId}` | Discover market data dependencies |

**SOD Snapshot**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/risk/sod-snapshot/{bookId}/status` | SOD baseline status |
| `POST` | `/api/v1/risk/sod-snapshot/{bookId}` | Create manual SOD snapshot (`jobId` optional) |
| `DELETE` | `/api/v1/risk/sod-snapshot/{bookId}` | Reset SOD baseline |

**EOD Timeline**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/risk/eod-timeline/{bookId}` | EOD history timeline (`from`, `to` YYYY-MM-DD, required) |

**Valuation Job History**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/risk/jobs/{bookId}` | List valuation jobs (`limit`, `offset`, `from`, `to`, `valuationDate`) |
| `GET` | `/api/v1/risk/jobs/{bookId}/chart` | Aggregated chart data (`from`, `to`) |
| `GET` | `/api/v1/risk/jobs/detail/{jobId}` | Job execution details |
| `PATCH` | `/api/v1/risk/jobs/{jobId}/label` | Promote/demote EOD label |
| `GET` | `/api/v1/risk/jobs/{bookId}/official-eod` | Official EOD designation (`date` YYYY-MM-DD) |

**Run Comparison**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/risk/compare/{bookId}` | Compare two runs by job ID |
| `GET` | `/api/v1/risk/compare/{bookId}/day-over-day` | Day-over-day VaR comparison |
| `POST` | `/api/v1/risk/compare/{bookId}/day-over-day/attribution` | Day-over-day attribution (expensive) |
| `POST` | `/api/v1/risk/compare/{bookId}/model` | Compare VaR across model configurations |
| `GET` | `/api/v1/risk/compare/{bookId}/market-data-quant` | Quantitative market data diff between manifests |

**Hierarchy & Budget**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/risk/hierarchy/{level}/{entityId}` | Risk at hierarchy node |
| `GET` | `/api/v1/risk/budgets` | List risk budgets |
| `POST` | `/api/v1/risk/budgets` | Create risk budget |
| `GET` | `/api/v1/risk/budgets/{id}` | Get risk budget |
| `DELETE` | `/api/v1/risk/budgets/{id}` | Delete risk budget |

**CRO Report**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/risk/reports/cro` | Generate CRO report |

**Liquidity Risk**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/books/{bookId}/liquidity-risk` | Calculate liquidity risk |
| `GET` | `/api/v1/books/{bookId}/liquidity-risk/latest` | Latest liquidity risk result |
| `GET` | `/api/v1/books/{bookId}/liquidity-risk` | Liquidity risk history |

**Market Regime**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/risk/regime/current` | Current market regime and adaptive VaR parameters |
| `GET` | `/api/v1/risk/regime/history` | Market regime history (`limit`) |

**Counterparty Risk**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/counterparty-risk` | All counterparty exposures |
| `GET` | `/api/v1/counterparty-risk/{counterpartyId}` | Single counterparty exposure |
| `GET` | `/api/v1/counterparty-risk/{counterpartyId}/history` | Exposure history |
| `POST` | `/api/v1/counterparty-risk/{counterpartyId}/pfe` | Compute PFE |
| `POST` | `/api/v1/counterparty-risk/{counterpartyId}/cva` | Compute CVA |
| `GET` | `/api/v1/counterparty/{counterpartyId}/sa-ccr` | SA-CCR exposure |

**Key Rate Duration & Margin**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/risk/krd/{bookId}` | Key Rate Durations |
| `GET` | `/api/v1/books/{bookId}/margin` | Margin estimate |

**Hedge Recommendations**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/risk/hedge-suggest/{bookId}` | Generate hedge recommendations |
| `GET` | `/api/v1/risk/hedge-suggest/{bookId}` | List latest hedge recommendations |
| `GET` | `/api/v1/risk/hedge-suggest/{bookId}/{id}` | Get specific hedge recommendation |

**Reports**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/reports/templates` | List report templates |
| `POST` | `/api/v1/reports/generate` | Generate a report |
| `GET` | `/api/v1/reports/{outputId}` | Get report output |
| `GET` | `/api/v1/reports/{outputId}/csv` | Download report as CSV |

**Regulatory**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/regulatory/frtb/{bookId}` | Calculate FRTB capital charges |
| `POST` | `/api/v1/regulatory/report/{bookId}` | Generate regulatory report |

**Internal**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/internal/risk/demo-reset` | Reset risk data for demo environment (`X-Demo-Reset-Token` required) |

**Kafka:** Consumes from `trades.lifecycle`, `price.updates`, `rates.*` · Publishes to `risk.results`  
**gRPC:** Calls Risk Engine on port 50051

---

### Notification Service (port 8086) — Alerts & Rules

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/health` | Liveness check |
| `GET` | `/health/ready` | Readiness check (DB + Kafka consumers) |
| `GET` | `/metrics` | Prometheus metrics |
| `GET` | `/openapi.json` | OpenAPI specification |
| `GET` | `/swagger` | Swagger UI |
| `GET` | `/api/v1/notifications/rules` | List alert rules |
| `POST` | `/api/v1/notifications/rules` | Create an alert rule |
| `DELETE` | `/api/v1/notifications/rules/{ruleId}` | Delete an alert rule |
| `GET` | `/api/v1/notifications/alerts` | Recent alerts (`limit`, `status` filter) |
| `GET` | `/api/v1/notifications/alerts/escalated` | Escalated alerts |
| `GET` | `/api/v1/notifications/alerts/{alertId}/contributors` | Position breakdown at breach time |
| `POST` | `/api/v1/notifications/alerts/{alertId}/acknowledge` | Acknowledge an alert |

**Kafka:** Consumes from `risk.results`, `risk.anomalies`

---

### Rates Service (port 8088) — Yield Curves, Risk-Free Rates, Forwards

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/health` | Liveness check |
| `GET` | `/health/ready` | Readiness check |
| `GET` | `/metrics` | Prometheus metrics |
| `GET` | `/openapi.json` | OpenAPI specification |
| `GET` | `/swagger` | Swagger UI |
| `GET` | `/api/v1/rates/yield-curves/{curveId}/latest` | Latest yield curve |
| `GET` | `/api/v1/rates/yield-curves/{curveId}/history` | Yield curve history (`from`, `to`) |
| `POST` | `/api/v1/rates/yield-curves` | Ingest yield curve data |
| `GET` | `/api/v1/rates/risk-free/{currency}/latest` | Latest risk-free rate (`tenor` param) |
| `POST` | `/api/v1/rates/risk-free` | Ingest risk-free rate |
| `GET` | `/api/v1/rates/forwards/{instrumentId}/latest` | Latest forward curve |
| `GET` | `/api/v1/rates/forwards/{instrumentId}/history` | Forward curve history (`from`, `to`) |
| `POST` | `/api/v1/rates/forwards` | Ingest forward curve data |

**Kafka:** Publishes to `rates.yield-curves`, `rates.risk-free`, `rates.forwards`

---

### Reference Data Service (port 8089) — Instruments, Organisations, Counterparties

**Common**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/health` | Liveness check |
| `GET` | `/health/ready` | Readiness check |
| `GET` | `/metrics` | Prometheus metrics |
| `GET` | `/openapi.json` | OpenAPI specification |
| `GET` | `/swagger` | Swagger UI |

**Instruments**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/instruments` | List instruments (`type`, `assetClass` filters) |
| `POST` | `/api/v1/instruments` | Create an instrument |
| `GET` | `/api/v1/instruments/{id}` | Get instrument by ID |

**Dividends & Credit Spreads**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/reference-data/dividends/{instrumentId}/latest` | Latest dividend yield |
| `POST` | `/api/v1/reference-data/dividends` | Ingest dividend yield |
| `GET` | `/api/v1/reference-data/credit-spreads/{instrumentId}/latest` | Latest credit spread |
| `POST` | `/api/v1/reference-data/credit-spreads` | Ingest credit spread |

**Divisions**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/divisions` | List all divisions (includes desk count) |
| `POST` | `/api/v1/divisions` | Create a division |
| `GET` | `/api/v1/divisions/{id}` | Get division by ID |
| `GET` | `/api/v1/divisions/{id}/desks` | List desks in a division |

**Desks**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/desks` | List all desks |
| `POST` | `/api/v1/desks` | Create a desk |
| `GET` | `/api/v1/desks/{id}` | Get desk by ID |

**Counterparties & Netting**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/counterparties` | List all counterparties |
| `POST` | `/api/v1/counterparties` | Create or update a counterparty |
| `GET` | `/api/v1/counterparties/{id}` | Get counterparty by ID |
| `GET` | `/api/v1/counterparties/{id}/netting-sets` | List netting agreements for a counterparty |
| `POST` | `/api/v1/netting-agreements` | Create or update a netting agreement |
| `GET` | `/api/v1/netting-agreements/{id}` | Get netting agreement by netting set ID |

**Liquidity**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/liquidity` | List all instrument liquidity records |
| `POST` | `/api/v1/liquidity` | Create or update instrument liquidity |
| `GET` | `/api/v1/liquidity/batch` | Batch liquidity lookup (`ids` comma-separated query param) |
| `GET` | `/api/v1/liquidity/{id}` | Liquidity data for a specific instrument |

**Benchmarks**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/benchmarks` | List all benchmarks |
| `POST` | `/api/v1/benchmarks` | Create a benchmark |
| `GET` | `/api/v1/benchmarks/{id}` | Get benchmark with constituents (`asOfDate` YYYY-MM-DD) |
| `PUT` | `/api/v1/benchmarks/{id}/constituents` | Replace benchmark constituent weights |
| `POST` | `/api/v1/benchmarks/{id}/returns` | Record a benchmark daily return |

---

### Volatility Service (port 8090) — Volatility Surfaces

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/health` | Liveness check |
| `GET` | `/health/ready` | Readiness check |
| `GET` | `/metrics` | Prometheus metrics |
| `GET` | `/openapi.json` | OpenAPI specification |
| `GET` | `/swagger` | Swagger UI |
| `GET` | `/api/v1/volatility/{instrumentId}/surface/latest` | Latest volatility surface |
| `GET` | `/api/v1/volatility/{instrumentId}/surface/diff` | Vol surface day-over-day diff (`compareDate` ISO-8601 instant, required) |
| `GET` | `/api/v1/volatility/{instrumentId}/surface/history` | Vol surface history (`from`, `to` ISO-8601) |
| `POST` | `/api/v1/volatility/surfaces` | Ingest a volatility surface |

---

### Correlation Service (port 8091) — Correlation Matrices

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/health` | Liveness check |
| `GET` | `/health/ready` | Readiness check |
| `GET` | `/metrics` | Prometheus metrics |
| `GET` | `/openapi.json` | OpenAPI specification |
| `GET` | `/swagger` | Swagger UI |
| `GET` | `/api/v1/correlations/latest` | Latest correlation matrix (`labels`, `window` params) |
| `POST` | `/api/v1/correlations/ingest` | Ingest correlation matrix data |

---

### Audit Service (port 8084) — Immutable Audit Trail

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/health` | Liveness check |
| `GET` | `/health/ready` | Readiness check |
| `GET` | `/metrics` | Prometheus metrics |
| `GET` | `/openapi.json` | OpenAPI specification |
| `GET` | `/swagger` | Swagger UI |
| `GET` | `/api/v1/audit/events` | Audit events (`bookId`, `afterId` cursor, `limit`) |
| `GET` | `/api/v1/audit/verify` | Verify hash-chain integrity (incremental, checkpoint-resumable) |
| `GET` | `/api/v1/audit/gaps` | Detect sequence number gaps |

**Internal**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/internal/audit/demo-reset` | Reset audit data for demo environment (`X-Demo-Reset-Token` required) |

**Kafka:** Consumes from `trades.lifecycle`

---

### Regulatory Service (port 8085) — FRTB, Backtesting, Submissions, Governance

**Common**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/health` | Liveness check |
| `GET` | `/health/ready` | Readiness check |
| `GET` | `/metrics` | Prometheus metrics |
| `GET` | `/openapi.json` | OpenAPI specification |
| `GET` | `/swagger` | Swagger UI |

**FRTB**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/regulatory/frtb/{bookId}/calculate` | Calculate FRTB capital charges and persist record |
| `GET` | `/api/v1/regulatory/frtb/{bookId}/history` | FRTB calculation history (`limit`, `offset`) |
| `GET` | `/api/v1/regulatory/frtb/{bookId}/latest` | Latest FRTB result |

**Backtesting**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/regulatory/backtest/{bookId}` | Run VaR backtest (Kupiec POF + Christoffersen tests) |
| `GET` | `/api/v1/regulatory/backtest/{bookId}/latest` | Latest backtest result |
| `GET` | `/api/v1/regulatory/backtest/{bookId}/compare` | Compare two backtest results (`baseId`, `targetId`) |
| `GET` | `/api/v1/regulatory/backtest/{bookId}/history` | Backtest history (`limit`, `offset`) |

**Stress Scenarios** (governance lifecycle)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/stress-scenarios` | List all stress scenarios |
| `GET` | `/api/v1/stress-scenarios/approved` | List approved scenarios |
| `POST` | `/api/v1/stress-scenarios` | Create a scenario |
| `PATCH` | `/api/v1/stress-scenarios/{id}/submit` | Submit for approval |
| `PATCH` | `/api/v1/stress-scenarios/{id}/approve` | Approve |
| `PATCH` | `/api/v1/stress-scenarios/{id}/retire` | Retire |
| `PUT` | `/api/v1/stress-scenarios/{id}` | Update scenario shocks/correlations |
| `POST` | `/api/v1/stress-scenarios/{id}/run` | Run scenario against a book |
| `POST` | `/api/v1/stress-scenarios/reverse-stress` | Find minimum-norm shock vector for target loss |
| `POST` | `/api/v1/stress-scenarios/correlated` | Create scenario with correlation-derived secondary shocks |
| `POST` | `/api/v1/stress-scenarios/parametric-grid` | 2D parametric sweep returning P&L impact matrix |

**Historical Scenarios**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/historical-scenarios/custom-replay` | Replay arbitrary historical date range using live price data |

**Regulatory Submissions** (four-eyes workflow)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/submissions` | Create a submission |
| `GET` | `/api/v1/submissions` | List all submissions |
| `PATCH` | `/api/v1/submissions/{id}/review` | Submit for review |
| `PATCH` | `/api/v1/submissions/{id}/approve` | Approve (four-eyes enforced) |
| `PATCH` | `/api/v1/submissions/{id}/acknowledge` | Acknowledge |
| `PATCH` | `/api/v1/submissions/{id}/submit` | Final submit |

**Model Governance**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/models` | List all model versions |
| `POST` | `/api/v1/models` | Register a new model version |
| `PATCH` | `/api/v1/models/{id}/status` | Transition model version status |

**Kafka:** Consumes from `risk.results`

---

### Risk Engine (gRPC port 50051, metrics 9091) — Python Compute

All endpoints are gRPC RPCs. No REST API.

**RiskCalculationService**

| RPC | Request → Response | Description |
|-----|-------------------|-------------|
| `Valuate` | `ValuationRequest → ValuationResponse` | Unified VaR + Greeks calculation (requested outputs: VAR, EXPECTED_SHORTFALL, GREEKS) |
| `CalculateVaR` | `VaRRequest → VaRResponse` | ~~Deprecated~~ — use `Valuate` instead |
| `CalculateVaRStream` | `stream VaRRequest → stream VaRResponse` | Bidirectional streaming VaR |

**StressTestService**

| RPC | Request → Response | Description |
|-----|-------------------|-------------|
| `RunStressTest` | `StressTestRequest → StressTestResponse` | Run stress test with market shocks |
| `RunHistoricalReplay` | `HistoricalReplayRequest → HistoricalReplayResponse` | Replay historical returns against current positions |
| `RunReverseStress` | `ReverseStressRequest → ReverseStressResponse` | Find minimum-norm shock vector for target loss |
| `ListScenarios` | `ListScenariosRequest → ListScenariosResponse` | List historical stress scenarios |
| `CalculateGreeks` | `GreeksRequest → GreeksResponse` | ~~Deprecated~~ — use `Valuate` with GREEKS output instead |

**RegulatoryReportingService**

| RPC | Request → Response | Description |
|-----|-------------------|-------------|
| `CalculateFrtb` | `FrtbRequest → FrtbResponse` | FRTB capital charges and components |
| `GenerateReport` | `GenerateReportRequest → GenerateReportResponse` | Regulatory report (CSV/XBRL) |

**MarketDataDependenciesService**

| RPC | Request → Response | Description |
|-----|-------------------|-------------|
| `DiscoverDependencies` | `DataDependenciesRequest → DataDependenciesResponse` | Discover required market data |

**MLPredictionService**

| RPC | Request → Response | Description |
|-----|-------------------|-------------|
| `PredictVolatility` | `VolatilityRequest → VolatilityResponse` | Single instrument vol prediction (LSTM) |
| `PredictVolatilityBatch` | `BatchVolatilityRequest → BatchVolatilityResponse` | Batch vol prediction |
| `ScoreCredit` | `CreditScoreRequest → CreditScoreResponse` | Credit default probability (GBT) |
| `DetectAnomaly` | `AnomalyRequest → AnomalyResponse` | Anomaly detection (Isolation Forest) |

| Protocol | Endpoint | Description |
|----------|----------|-------------|
| `HTTP GET` | `http://localhost:9091/metrics` | Prometheus metrics |

---

## Permission Reference

The gateway enforces role-based permissions on all `authenticate("auth-jwt")` routes.

| Permission | Routes |
|------------|--------|
| `READ_PORTFOLIOS` | Books, trades, positions, strategies, prices |
| `READ_POSITIONS` | Instruments, data quality, execution endpoints |
| `WRITE_TRADES` | `POST /books/{bookId}/trades`, `POST /orders` |
| `CALCULATE_RISK` | VaR, cross-book VaR, what-if, dependencies, liquidity risk, factor risk, SOD, run comparison, intraday P&L/VaR, reports, benchmark attribution, KRD, margin |
| `READ_RISK` | Stress tests, job history, market regime, counterparty risk, SA-CCR, vol surfaces, hedge recommendations |
| `READ_REGULATORY` | FRTB, regulatory report, EOD timeline |
| `READ_ALERTS` | Notification rules and alerts |
| `READ_AUDIT` | Audit events and verification |
| `MANAGE_SCENARIOS` | Stress scenario governance (create/approve/retire), backtest proxy |
| `PROMOTE_EOD_RUN` | Patch job EOD label |

---

## Summary

```
Protocol Breakdown
──────────────────────────────────────────────
  REST (HTTP)    ~250 endpoints across 11 services
  gRPC            14 RPCs in 5 service definitions
  WebSocket        3 real-time streams
──────────────────────────────────────────────

Method Distribution (REST, approximate)
──────────────────────────────────────────────
  GET            ~150  ████████████████████  (60%)
  POST            ~70  ████████████          (28%)
  PATCH           ~20  ████                  ( 8%)
  DELETE           ~8  █                     ( 3%)
  PUT              ~5  █                     ( 2%)
──────────────────────────────────────────────

Endpoints per Service (REST, approximate)
──────────────────────────────────────────────
  Gateway          ~80  (all proxied; broken out above)
  Risk Orchestr.   ~55  (direct service endpoints)
  Position Svc     ~30
  Regulatory Svc   ~25
  Ref Data Svc     ~25
  Rates Service    ~12
  Notification     ~12
  Volatility Svc    ~8
  Price Service     ~8
  Audit Service     ~8
  Correlation Svc   ~6
──────────────────────────────────────────────

Common Endpoints (all Kotlin services)
──────────────────────────────────────────────
  GET /health          Liveness check
  GET /health/ready    Readiness check (DB + Kafka)
  GET /metrics         Prometheus scrape target
  GET /openapi.json    OpenAPI specification
  GET /swagger         Swagger UI
──────────────────────────────────────────────
```
