# Kinetix Platform Memory

## Plan Progress Tracker
Source: `docs/evolution/agent-team-system-plan-no-sre.md` (44 items, 6 phases)

### Phase 1: Critical Fixes -- COMPLETED
- [x] 1.1 Fix Rho Greek (risk-engine: greeks.py, portfolio_risk.py)
- [x] 1.2 Make VaR Deterministic (risk-engine: server.py, valuation.py)
- [x] 1.3 Hash-Chained Audit Trail (audit-service: V2 migration, AuditHasher, verify endpoint)
- [x] 1.4 Extend Audit Coverage (audit-service: V3 migration, userId/userRole/eventType)
- [x] 1.5 Decouple Risk-Orchestrator (risk-orchestrator: HttpPositionServiceClient, removed position-service dep)
- [x] 1.6 Kafka DLQ and Retry (common: RetryableConsumer, updated all 6 consumers)
- [x] Fix: end2end test adapter for PositionServiceClient

### Phase 2: Trading Workflow Essentials -- COMPLETED
- [x] 2.1 Trade Amend and Cancel (position-service: TradeType, TradeStatus, TradeLifecycleService, PUT/DELETE endpoints)
- [x] 2.2 Trade Blotter UI (ui: TradeBlotter component + trades tab, gateway: trade history endpoint)
- [x] 2.3 Pre-Trade Limit Checks (position-service: LimitCheckService, position/notional/concentration limits)
- [x] 2.4 Realized P&L Tracking (common: Position.realizedPnl, position-service: applyTrade computes realized)
- [x] 2.5 Multi-Currency Portfolio Aggregation (position-service: PortfolioAggregationService, FxRateProvider, ui: PortfolioSummaryCard)

### Phase 3: Quantitative Models -- COMPLETED
- [x] 3.1 Black-Scholes Options Pricing (risk-engine: bs_price/delta/gamma/vega/theta/rho, OptionPosition model)
- [x] 3.2 Wire Up Vol Surface and Yield Curve (risk-engine: VolSurface/YieldCurveData models, market_data_consumer wiring)
- [x] 3.3 Fix Historical VaR (risk-engine: historical_returns param for actual return replay)
- [x] 3.4 EWMA Volatility Estimation (risk-engine: ewma.py, VolatilityProvider.ewma() factory, lambda=0.94)
- [x] 3.5 VaR Backtesting Framework (risk-engine: Kupiec POF + Christoffersen tests, regulatory-service: backtest endpoints)
- [x] 3.6 Enhanced DRC with Credit Quality Bucketing (risk-engine: 21 credit ratings, seniority LGD, maturity weights, sector concentration)

### Phase 4: Data Hardening -- COMPLETED
- [x] 4.1 TimescaleDB Retention/Compression (prices: 2yr retention, valuation_jobs: 1yr, audit: 7yr)
- [x] 4.2 Continuous Aggregates (hourly VaR summary, daily P&L summary)
- [x] 4.3 Redis for Shared Caching (VaRCache interface, RedisVaRCache with Lettuce, InMemory fallback)
- [x] 4.4 Correlation ID Propagation (correlationId on TradeEvent/PriceEvent/RiskResultEvent, UUID at source)
- [x] 4.5 Provision All Kafka Topics (expanded create-topics.sh from 3 to 16 topics including DLQs)
- [x] 4.6 Fix Audit Schema Types (V5 migration: quantity/price_amount->NUMERIC, traded_at->TIMESTAMPTZ)
- [x] 4.7 Add Missing Database Indexes (positions: instrument_id/updated_at, daily_risk_snapshots: snapshot_date)
- Note: TimescaleDB migrations require postgres with timescaledb extension; test containers need update

### Phase 5: UI/UX Improvements -- COMPLETED
- [x] 5.1 Accessibility (WAI-ARIA roles, keyboard nav, aria-labels, aria-live)
- [x] 5.2 WebSocket Auto-Reconnect (exponential backoff, max 20 attempts, reconnecting banner)
- [x] 5.3 Position Grid Pagination (50 rows/page)
- [x] 5.4 Column Visibility Toggles (gear dropdown, localStorage persistence)
- [x] 5.5 Consolidate Currency Formatting (shared formatCurrency utility)
- [x] 5.6 Dark Mode (useTheme hook, Tailwind class-based, sun/moon toggle)
- [x] 5.7 Confirmation on Alert Rule Deletion (ConfirmDialog component)
- [x] 5.8 CSV Export from All Tabs (exportToCsv utility, buttons on positions/risk/P&L/alerts)
- [x] Fix: audit-service TimescaleDB compatibility (triggers instead of rules, timescaledb test container)

### Phase 6: Advanced Capabilities -- COMPLETED
- [x] 6.1 Cross-Greeks (Vanna, Volga, Charm -- analytical Black-Scholes formulas)
- [x] 6.2 Variance Reduction for Monte Carlo (antithetic variates)
- [x] 6.3 Dynamic Correlation Estimation (Ledoit-Wolf shrinkage)
- [x] 6.4 Model Governance Framework (regulatory-service: model versioning, approval workflow)
- [x] 6.5 Regulatory Submission Workflow (four-eyes principle, preparer/approver separation)
- [x] 6.6 Limit Management Hierarchy (FIRM→DESK→TRADER→COUNTERPARTY, intraday/overnight)
- [x] 6.7 Margin Calculator (SPAN/SIMM simplified margin by asset class)
- [x] 6.8 Counterparty Risk View (exposure aggregation, netting sets)
- [x] 6.9 Multi-Portfolio Aggregate View (UI: multi-select portfolio picker)
- [x] 6.10 Stress Testing Governance (scenario management, approval workflow)
- [x] 6.11 Data Quality Monitoring (UI: traffic light indicator, staleness detection)
- [x] 6.12 Workspace Customisation (UI: layout/preference persistence via localStorage)

## Test Gap Remediation Progress
Source: `docs/test-gap-remediation-plan.md`

### Phase 1: Critical -- COMPLETED
- [x] RiskResultSchemaCompatibilityTest + notification-service type fix
- [x] TradeEventSchemaCompatibilityTest + risk-orchestrator field fix
- [x] PriceEventSchemaCompatibilityTest
- [x] Position-service acceptance tests (booking, lifecycle, limits) - 11 tests
- [x] Audit-service acceptance tests (hash chain, event consumption) - 7 tests
- [x] buf lint/breaking CI for proto module

### Phase 2: Important -- COMPLETED
- [x] Risk-engine pytest markers (unit/integration/performance)
- [x] Price-service acceptance tests (7 tests, fixed DESC sort order bug)
- [x] Data service contract tests (rates: 6, correlation: 3, volatility: 3, reference-data: 6)
- [x] RetryableConsumer Kafka integration test (3 tests, in position-service due to common module Docker incompatibility)

### Phase 3: Hardening -- COMPLETED
- [x] Playwright P1 browser tests (dark mode 7, CSV export 8, WebSocket reconnect 6)
- [x] Playwright P2 browser tests (column visibility 3, pagination 6, alert rules 5, keyboard nav 8)
- [x] Kafka event schema consolidation into common module (8 per-service files deleted)
- [x] CircuitBreaker HTTP integration test (5 tests, embedded HttpServer)
- [x] gRPC contract integration test (5 tests, real Python risk-engine in Docker)

### Phase 4: Post-Remediation Follow-on -- COMPLETE (start 2026-04-22, completed 2026-04-29)
Source: `docs/plans/test-gap-remediation-phase-4.md`

#### 4A: Critical (P0) -- COMPLETE
- [x] 4A.1 LimitRoutesAcceptanceTest (8 tests) — hierarchy limit routes
- [x] 4A.2 BookHierarchyRoutesAcceptanceTest (7 tests)
- [x] 4A.3 InternalRoutesAcceptanceTest (4 tests) — note: countSince filters on row createdAt, not tradedAt
- [x] 4A.4 AlertLifecycleEventSchemaCompatibilityTest (4 tests)
- [x] 4A.5 BookVaRContributionEventSchemaCompatibilityTest (3 tests)
- [x] 4A.6 CrossBookRiskResult + LiquidityRisk schema compat tests (6 tests)

#### 4B: High (P1) -- COMPLETE (7/8, 1 deferred)
- [x] 4B.1 ExposedModelVersionRepositoryIntegrationTest (5 tests) — state machine lives in ModelRegistry service, not repo
- [x] 4B.2 ExposedSubmissionRepositoryIntegrationTest (5 tests) — four-eyes lives in SubmissionService, not repo
- [x] 4B.3 MarketRegimeEventConsumerTest (5 tests)
- [ ] 4B.4 PagerDutyDeliveryServiceTest -- DEFERRED (stub implementation, no HTTP client yet)
- [x] 4B.5 GatewayExecutionProxyContractAcceptanceTest (6 tests)
- [x] 4B.6 GatewayMarketRegimeContractAcceptanceTest (5 tests) — margin sub-item deferred (route orphaned)
- [x] 4B.7 KafkaFIXSessionEventPublisherIntegrationTest (2 tests)
- [x] 4B.8 KafkaReconciliationAlertPublisherIntegrationTest (3 tests)

#### 4C: Moderate (P2) -- COMPLETE (3 done, 2 deferred)
- [x] 4C.1 ExposedInstrumentRepository (7) + ExposedNettingAgreementRepository (5) integration tests
- [x] 4C.2 UI components: AlertDrillDownPanel (11) + VaRAttributionPanel (8) + EodTimelineTab (7)
- [x] 4C.3 UI API modules: execution.ts (8) + regime.ts (7)
- [ ] 4C.4 Playwright margin.spec.ts -- DEFERRED (UI has no margin panel)
- [ ] 4C.5 Playwright limit-management.spec.ts -- DEFERRED (no limit-management UI surface)

#### 4D: P3 -- COMPLETE (2/3, 1 deferred)
- [x] 4D.1 CounterpartyRoutesAcceptanceTest (5 tests)
- [x] 4D.2 Regulatory-service HTTP client unit tests (Correlation 6, Price 6, RiskOrchestrator 7)
- [ ] 4D.3 Cross-service limit-breach → alert-escalation E2E -- DEFERRED (no LimitBreachEvent on Kafka)

**Final total: 18 test classes, 127 tests, all green** across 7 services. Commits: one per test class per the plan's convention.

### Phase 4 Findings (surfaced during implementation)
- **Orphaned `marginRoutes`**: defined in `gateway/src/main/kotlin/com/kinetix/gateway/routes/MarginRoutes.kt` but not referenced from any `Application.module(...)` overload. `/api/v1/books/{bookId}/margin` returns 404 on a running gateway. UI doesn't call it. Either wire into `module(riskClient)` or remove route + client method + DTO.
- **`PagerDutyDeliveryService` is a stub** (`TODO(ALT-04)`): no HTTP client, routing key, or retry logic. Real Events API v2 integration needed before meaningful retry/error tests.
- **`countSince` semantics** in `TradeEventRepository` filters on row `createdAt` (persistence time), not `tradedAt` — correct for reconciliation but worth keeping in mind if future features need trade-time filtering.
- **No limit-management UI surface**: only `LimitBreachCard` (stress-scenario breaches) and `useVarLimit` (alert rule threshold) exist. The FIRM/DESK/TRADER/COUNTERPARTY limits hierarchy from position-service is not rendered anywhere — 4C.5 cannot be tested until a limit-management UI lands.
- **No `LimitBreachEvent` Kafka chain**: position-service throws `LimitBreachException` synchronously on breach; no Kafka topic for limit breaches, no risk-orchestrator consumer, no notification-service rule for limit-breach alerts. Synchronous enforcement is already covered by `LimitEnforcementAcceptanceTest`. 4D.3 needs the breach-event flow to be built first.
- **`/api/v1/counterparty-exposure` returns empty list (not 404) on unknown bookId** — 4D.1 asserts the actual behaviour; if the contract should change, that's a separate decision.

### Known Issues
- Testcontainers Docker connectivity fails in common module (library module classpath missing Docker client deps). Workaround: place integration tests in service modules.
- Exposed 0.58.0 + Kotest: exceptions inside newSuspendedTransaction cannot be caught by shouldThrow. Workaround: move validation before transactional.run{} block.
- risk-engine Dockerfile needs PYTHONPATH=/app/src env var for module resolution (uv sync doesn't install project in editable mode)

## User
- [User profile](user_profile.md) — experienced developer, values clean readable code

## Workflow Preferences
- [Commit granularity](feedback_commit_granularity.md) — smaller, incremental commits per logical layer

## Instrument Type Model -- COMPLETED
Source: `docs/plans/instrument-types.md` (3 phases, 13 commits)

### Phase A: Kotlin Type Hierarchy & Instrument Master
- [x] A1. Sealed interface InstrumentType with 11 typed subtypes (common module)
- [x] A2. Instruments table migration + repository (reference-data-service, JSONB attributes)
- [x] A3. Instrument REST routes + service + acceptance tests (7 tests)
- [x] A4. Dev data seeder with sample instruments for all 11 types
- [x] A5. Proto extension: InstrumentTypeEnum + 5 attribute messages + Position fields
- [x] A6. Position-service V9 migration + instrument_type on Trade/Position models

### Phase B: Risk Engine Integration
- [x] B1. Python position subtypes (BondPosition, FuturePosition, FxPosition, SwapPosition)
- [x] B2. Black-Scholes Merton dividend yield extension (q parameter throughout)
- [x] B3. Converter upgrade: proto instrument_type → typed Python positions
- [x] B4. Delta-adjusted VaR via position_resolver.py + valuation.py wiring

### Phase C: UI & Gateway
- [x] C1. Risk-orchestrator InstrumentServiceClient + Position proto enrichment
- [x] C2. Gateway instrument routes + position/trade response enrichment
- [x] C3. UI instrument type/name columns + 6 Playwright tests

## Key Patterns
- Kotlin services: Ktor, Exposed ORM, Kotest FunSpec, MockK
- Python risk-engine: uv, pytest
- UI: Vite + React + TypeScript + Vitest
- HTTP clients in risk-orchestrator follow pattern: interface + HttpClient impl + DTOs with toDomain() + MockEngine tests
- Kafka consumers now use RetryableConsumer from common module
- Audit events have hash chain (AuditHasher) and immutability rules
