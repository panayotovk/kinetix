# Spec Implementation Gap Plan

Consolidated from trader, architect, QA, UX, and data analyst review of all 20 allium v3 specs against the codebase.

## Items Already Closed (removed from plan)

- **PositionRisk.es_contribution** — already in Kotlin model, mapped in DTOs, rendered in UI
- **Market data completeness gate for EOD promotion** — implemented in `EodPromotionService.promoteToOfficialEod()`
- **HTTP 409 for InvalidTradeStateException** — already handled in position-service `Application.kt`

## New Gaps Discovered During Review

- Hedge accept/reject routes missing from `HedgeRecommendationRoutes.kt`
- Scenario `computePnlImpact()` in `StressScenarioService` is a stub (sums JSON values instead of calling risk engine)
- `HedgeRecommendationService.buildCandidate()` uses `pricePerUnit = 100.0` placeholder instead of live prices

---

## Phase 1 — Scenario Hardening

### 1.1 SavedScenario schema migration

Add missing columns to `stress_scenarios` table.

**Migration**: `regulatory-service/src/main/resources/db/regulatory/V15__add_scenario_versioning_fields.sql`
- `version INTEGER NOT NULL DEFAULT 1`
- `parent_scenario_id VARCHAR(255)`
- `correlation_override TEXT`
- `liquidity_stress_factors TEXT`
- `historical_period_id VARCHAR(255)`
- `target_loss NUMERIC(28,8)`
- Index on `(parent_scenario_id)` and `(name, version DESC)`

**Kotlin files to modify**:
- `regulatory-service/.../stress/StressScenario.kt` — add 6 fields
- `regulatory-service/.../persistence/StressScenariosTable.kt` — add columns
- `regulatory-service/.../persistence/ExposedStressScenarioRepository.kt` — wire in insert/update/toScenario
- `regulatory-service/.../stress/dto/StressScenarioResponse.kt` — add fields
- `regulatory-service/.../stress/dto/CreateScenarioRequest.kt` — add optional fields

**Tests**:
- Unit: `"creates scenario with version=1 by default"`, `"retains parent_scenario_id when provided"`
- Integration: round-trip persistence of new columns
- Acceptance: create and GET verify all new fields

### 1.2 UpdateScenario rule + approved→draft transition

Implement version increment and status reset on scenario update.

**Kotlin files to modify**:
- `StressScenarioService.kt` — add `update(id, shocks, correlationOverride, liquidityStressFactors)`:
  1. Fetch scenario
  2. If APPROVED → reset to DRAFT, clear approvedBy/approvedAt
  3. Increment version, update content fields
  4. Save
- `ExposedStressScenarioRepository.kt` — extend save path for content updates
- `StressScenarioRoutes.kt` — add `PUT /api/v1/stress-scenarios/{id}`

**New file**: `regulatory-service/.../stress/dto/UpdateScenarioRequest.kt`

**Tests**:
- Unit: `"updating an approved scenario resets status to draft and increments version"`
- Unit: `"updating a retired scenario is rejected"` (terminal state guard)
- Acceptance: full `create → submit → approve → update → draft` lifecycle
- Integration: concurrent updates preserve higher version (optimistic locking)

### 1.3 HistoricalScenarioPeriod + HistoricalScenarioReturn entities

New tables for pre-computed crisis period returns.

**Migration**: `regulatory-service/.../db/regulatory/V16__create_historical_scenario_periods.sql`
- `historical_scenario_periods` — PK on `period_id`
- `historical_scenario_returns` — composite PK on `(period_id, instrument_id, return_date)`
- Indexes on `(period_id, return_date)` and `(instrument_id)`

**New Kotlin files**:
- `regulatory-service/.../historical/HistoricalScenarioPeriod.kt`
- `regulatory-service/.../historical/HistoricalScenarioReturn.kt`
- `regulatory-service/.../persistence/HistoricalScenarioPeriodsTable.kt`
- `regulatory-service/.../persistence/HistoricalScenarioReturnsTable.kt`
- `regulatory-service/.../historical/HistoricalScenarioRepository.kt` (interface)
- `regulatory-service/.../persistence/ExposedHistoricalScenarioRepository.kt`
- `regulatory-service/.../seed/HistoricalScenarioSeeder.kt` — seeds GFC, COVID, Taper Tantrum, Euro Crisis
- `regulatory-service/.../historical/HistoricalScenarioPeriodRoutes.kt` — `GET /api/v1/historical-periods`

**Tests**:
- Unit: route tests with mock repository
- Integration: seed and query by period/instrument/date range

### 1.4 Historical scenario replay

Wire Python `historical_replay.py` through gRPC to a Kotlin service.

**Proto changes**: `stress_testing.proto` — add `RunHistoricalReplay` RPC
**Python**: `stress_server.py` — implement RPC handler delegating to `run_historical_replay`

**New Kotlin files**:
- `regulatory-service/.../historical/HistoricalReplayService.kt`
- `regulatory-service/.../historical/dto/ReplayResultResponse.kt`
- `regulatory-service/.../historical/HistoricalReplayRoutes.kt`
  - `POST /api/v1/historical-periods/{periodId}/replay` — replay against pre-computed returns
  - `POST /api/v1/historical-periods/custom-replay` — replay from price-service date range

**Tests**:
- Unit: mock gRPC; test proxy fallback, max-period enforcement
- Python unit: daily P&L series, max drawdown, VaR breach count
- Acceptance: testApplication with stubbed gRPC; verify `proxy_used=true` in response

### 1.5 Reverse stress testing (gRPC wire-up)

Wire Python `reverse_stress.py` through gRPC.

**Proto changes**: `stress_testing.proto` — add `RunReverseStress` RPC
**Python**: `stress_server.py` — implement handler, aggregate instrument-level to asset-class-level shocks

**Kotlin files**:
- `risk-orchestrator/.../client/RiskEngineClient.kt` — extend interface
- `risk-orchestrator/.../client/GrpcRiskEngineClient.kt` — add `reverseStress()`
- `regulatory-service/.../stress/ReverseStressService.kt`
- `regulatory-service/.../stress/dto/ReverseStressRequest.kt`, `ReverseStressResponse.kt`
- `StressScenarioRoutes.kt` — add `POST /api/v1/stress-scenarios/reverse-stress`

**Data layer**: `reverse_stress_solver_log` table for convergence diagnostics (hypertable, 1-year retention)

**Tests**:
- Python integration: gRPC round-trip in `test_stress_server.py`
- Kotlin unit: mock gRPC; test non-convergence path
- Acceptance: verify HTTP contract; saving non-converged result returns 422

### 1.6 Fix scenario computePnlImpact stub

Replace trivial sum-of-shocks with actual risk engine call.

**File**: `StressScenarioService.kt` — `computePnlImpact()` must call risk-engine gRPC `Valuate` with shocked market data, not just sum JSON values.

**Tests**:
- Unit: mock risk engine; verify P&L impact is based on position × shock, not sum of shocks

---

## Phase 2 — Execution Pipeline

### 2.1 Order submission endpoint

**New files**:
- `position-service/.../fix/OrderSubmissionService.kt` — implements SubmitOrder rule:
  1. Validate quantity > 0
  2. Fetch arrival price (check 30s staleness)
  3. Create Order in PENDING_RISK_CHECK
  4. Run pre-trade check with 100ms timeout
  5. Transition to APPROVED or REJECTED
- `position-service/.../routes/dtos/SubmitOrderRequest.kt`, `OrderResponse.kt`
- Add `POST /api/v1/orders` to `ExecutionRoutes.kt`

**Tests**:
- Unit: `"rejects when arrival price is stale"`, `"transitions to APPROVED when check passes"`, `"transitions to REJECTED when check fails"`
- Acceptance: HTTP contract with mock dependencies

### 2.2 FIX outbound (SendOrderToFIX)

**New files**:
- `position-service/.../fix/FIXOrderSender.kt` (interface)
- `position-service/.../fix/LoggingFIXOrderSender.kt` — logs FIX tag=value format, updates status to SENT

**Modify**: `OrderSubmissionService.kt` — after APPROVED, check session status, call sender, update to SENT. If session DISCONNECTED, leave in APPROVED.

**Tests**:
- Unit: `"transitions to SENT when FIX session connected"`, `"leaves in APPROVED when session disconnected"`

### 2.3 FIX execution report processor (fills, cancels, replaces)

**New file**: `position-service/.../fix/FIXExecutionReportProcessor.kt` — dispatches on execType:
- `F`/`1`: dedup check → create fill → update order status → trigger BookTradeRequested
- `4`: validate order in {SENT, PARTIAL} → update to CANCELLED
- `5`: update order quantity/limitPrice

**Overfill guard** (FillsMatchOrder invariant): before saving fill, check `existingFilledQty + lastQty <= order.quantity`. Reject if exceeded.

**Tests**:
- Unit: `"processes full fill and transitions to FILLED"`, `"discards duplicate fill based on fix_exec_id"`, `"cancels order on ExecType=4"`, `"updates quantity on ExecType=5"`, `"rejects fill when cumulative quantity exceeds order quantity"`
- Acceptance: `FIXFillAutoBookingAcceptanceTest` — submit order, process fill, verify trade appears in position-service

### 2.4 FIX session health monitoring

**New file**: `position-service/.../fix/FIXSessionHealthMonitor.kt` — scheduled every 30s, checks `last_message_at` against 60s heartbeat threshold

**Migration**: `V18__create_fix_session_events.sql` — `fix_session_events` hypertable for connectivity history (1-year retention)

**Modify**: `FIXSessionRoutes.kt` — add `GET /api/v1/fix-sessions/health`

**Tests**:
- Unit: `"flags session as unhealthy when last_message_at exceeds threshold"`

### 2.5 Reconciliation break alerts

**New Kafka topic**: `reconciliation.alerts` (add to `deploy/create-topics.sh`)

**New files**:
- `position-service/.../kafka/ReconciliationAlertPublisher.kt` (interface)
- `position-service/.../kafka/KafkaReconciliationAlertPublisher.kt`

**Modify**: `PrimeBrokerReconciliationService.kt` — publish alerts for breaks > $10K notional (WARNING < $100K, CRITICAL >= $100K)

**Migration**: Convert `breaks` column from TEXT to JSONB for queryability

**Tests**:
- Unit: `"publishes CRITICAL alert for break exceeding $100K"`, `"does not publish for breaks below threshold"`
- Acceptance: `"reconciliation with PB-only position surfaces as a break"`, `"break_qty of exactly 1.0 is material"`

### 2.6 Execution data layer hardening

**Migrations**:
- Convert `execution_orders` and `execution_fills` to TimescaleDB hypertables (7-year retention, compression after 30 days)
- Add `fill_rate_pct` to `execution_cost_analysis`

---

## Phase 3 — Risk Hardening

### 3.1 DiversificationBenefitNonNegative invariant

**File**: `risk-orchestrator/.../service/CrossBookVaRCalculationService.kt`
- Change: `val diversificationBenefit = maxOf(0.0, totalStandaloneVar - crossBookVarValue)`
- Same clamp for per-book contribution

**Tests**:
- Unit: `"diversification benefit is never negative when cross-book VaR exceeds sum of standalones"`

### 3.2 ES contribution as first-class columns

**Migration**: Add `es_contribution NUMERIC(28,8)` and `var_contribution NUMERIC(28,8)` to `daily_risk_snapshots`. Populate from `position_risk_snapshot` JSONB at EOD promotion time.

### 3.3 EOD completeness materialised view

**Migration**: Create `daily_eod_completeness` materialised view showing promoted vs pending books per date. Refresh on `OfficialEodPromoted` events.

---

## Phase 4 — Polish + Data Quality

### 4.1 FxRate database persistence

**Migration**: `position-service/.../db/position/V18__create_fx_rates.sql` — `fx_rates` table with PK on `(from_currency, to_currency)`. Also `fx_rate_snapshots` hypertable for historical audit trail (2-year retention).

**New files**:
- `position-service/.../persistence/FxRatesTable.kt`
- `position-service/.../persistence/FxRateRepository.kt` (interface)
- `position-service/.../persistence/ExposedFxRateRepository.kt`

**Modify**: `LiveFxRateProvider.kt` — persist on update, fall back to DB on cache miss

**Tests**:
- Unit: `"falls back to DB when in-memory cache is empty"`
- Integration: upsert and query
- Unit: `"PortfolioAggregationService excludes position when FX rate unavailable and logs warning"`

### 4.2 Audit fields in request DTOs

**Modify**:
- `BookTradeRequest`, `AmendTradeRequest`, `CancelTradeRequest` — add optional `userId`, `userRole`
- Wire through command objects → TradeLifecycleService → KafkaTradeEventPublisher

**Tests**:
- Unit: `"publishes trade event with userId and userRole from request"`

### 4.3 Hedge recommendation fixes

**Accept/reject routes** — add to `HedgeRecommendationRoutes.kt`:
- `POST /hedge/{id}/accept` — validate pending + not expired, set ACCEPTED
- `POST /hedge/{id}/reject` — validate pending, set REJECTED

**Background expiry job** — new `ScheduledHedgeExpiryJob.kt` (runs every 60s, calls `repository.expirePending()`)

**Placeholder price fix** — `HedgeRecommendationService.buildCandidate()` must fetch live prices from price-service

**Tests**:
- Acceptance: `"rejects acceptance of expired recommendation with 409"`, `"rejects acceptance of already-accepted recommendation"`
- Unit: `"calls expirePending on repository every cycle"`

---

## UI Work (no new top-level tabs)

### Scenarios tab enhancements
- Version history accordion in `ScenarioLibraryGrid` row expansion
- Daily P&L bar chart + drawdown area chart in `HistoricalReplayPanel`
- Shock bar chart (horizontal, sorted by magnitude) in `ReverseStressDialog`
- Split footer: "Save Draft" / "Submit for Approval" / "Run Ad-Hoc" in `CustomScenarioBuilder`
- Clone-to-draft confirmation dialog when editing APPROVED scenario

### Trades tab — new "Orders" sub-tab
- `blotter | orders | cost | reconciliation`
- Order blotter with status badges, fill progress bars, row expansion for fill history
- "New Order" drawer panel (instrument autocomplete, side toggle, order type, quantity, price)

### Header indicators
- FIX session health indicator next to DataQualityIndicator (green/amber/red + tooltip)

### Risk tab
- ES Contribution column tooltip text (one-line fix)
- Market data completeness checklist in EodDrillPanel before promote button

### Accessibility requirements
- Instrument autocomplete: full ARIA combobox pattern
- Charts: `role="img"` + `aria-label`, keyboard-navigable data points
- Status badges: `aria-label` with text status
- Order blotter: `aria-live="polite"` for real-time status changes

---

## Critical Test Coverage (from QA review)

### P0 — Data integrity
- Double-fill deduplication (same exec_id twice → exactly one fill, one trade)
- Concurrent trade amendments (exactly one succeeds)
- Overfill protection (cum_qty > order.quantity → rejected)
- Order state machine illegal transitions (terminal → non-terminal blocked)

### P1 — Governance + safety
- Scenario version increment + APPROVED→DRAFT reset
- EOD promotion: SYSTEM-triggered job four-eyes case
- Expired hedge recommendation race condition (clock-at-check vs clock-at-commit)

### P2 — Correctness
- ES contribution sum equals aggregate ES
- Reconciliation: PB-only positions, exact threshold boundaries
- Historical replay proxy coverage at API level
- Reverse stress convergence failure at route level

---

## Commit Strategy

Per project conventions, each logical layer is a separate commit:
1. DB migration
2. Domain model + table object
3. Repository interface + Exposed implementation
4. Service logic
5. Route handler + DTOs
6. Unit tests
7. Integration test
8. Acceptance test

Each commit must leave the build green. Do not batch unrelated phases.
