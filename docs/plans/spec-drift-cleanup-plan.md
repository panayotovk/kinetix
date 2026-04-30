# Spec/Code Drift Cleanup — Phased Plan

**Created:** 2026-04-30
**Source audit:** `docs/spec-drift-audit.md`
**Total items:** 68 actionable + 17 deferred

This plan executes the cleanup of all 68 actionable spec/code divergences flagged by the cross-spec `weed` audit on 2026-04-30. Items reference the audit document by number (e.g. `[A-7]` = audit item 7).

## Guiding principles

- **Lowest-risk first.** Spec-only edits before code edits. Distillation before policy decisions.
- **One concern per batch.** Each batch has a single theme so failures are isolated.
- **Commit frequently.** Each item or tightly-related group is its own commit per `CLAUDE.md`.
- **Test at all applicable levels.** Per `CLAUDE.md`: unit + acceptance/integration + E2E for code changes.
- **Clear context between batches** to keep the prompt cache warm and avoid drift.
- **No batch starts until the previous one is committed and tests pass.**

## Phase structure

Nine batches grouped into three phases:

- **Phase 1 — Spec hygiene** (Batches A, B, H): pure spec edits. No code, no tests.
- **Phase 2 — Code correctness** (Batches C, D, E): high-confidence code fixes with TDD.
- **Phase 3 — Decisions & contracts** (Batches F, G, I): require business/quant calls or cross-cutting decisions.

---

## Phase 1 — Spec hygiene (low risk, fast wins)

### Batch A — Stale-spec corrections

Spec text that contradicts current code; correct the spec.

- [ ] [A-42] `scenario-lifecycle.allium`: remove "current implementation does NOT enforce four-eyes" note + close open question #1 (`StressScenarioService.kt:73-75` already enforces).
- [ ] [A-43] `regulatory.allium:240-243`: remove "computePnlImpact stub is a known data integrity bug" — now delegates to `riskOrchestratorClient.runStressTest`.
- [ ] [A-44] `alerts.allium:338-340`: remove "PAGER_DUTY ... not yet in core.allium" — already present in `core.allium:109`.
- [ ] [A-45] `discovery-valuation.allium:268`: change "Fetches proceed sequentially" to "parallel up to 10 via Semaphore" (`MarketDataFetcher.kt:62-69`).
- [ ] [A-50] `execution.allium:540`: close open question on fail-open vs fail-closed — code is fail-closed (`OrderSubmissionService.kt:113-120`).
- [ ] [A-46] `scenarios.allium:226`: drop or update `StressTestResultRecord` rename guidance (code didn't rename).
- [ ] [A-68] `core.allium`: add a one-line note on the enum casing translation convention (`buy` ↔ `BUY`).

**Acceptance:** `allium check specs/*.allium` passes with no new errors. No code changes.

**Execution:** parallel `tend` agents — each item is independent.

### Batch B — Distill missing fields into spec

Real code-side fields/lifecycles that the spec omits; add them to the spec.

- [ ] [A-48] `audit.allium` AuditEvent: add `sequence_number` field + sequence-gap detection rule.
- [ ] [A-49] `execution.allium` ReconciliationBreak: add `status` field + `ReconciliationBreakStatus` lifecycle (OPEN/INVESTIGATING/RESOLVED) + transitions.
- [ ] [A-51] `regime.allium` RegimeHistory: add `id`, `confidence`, `degradedInputs`, `consecutiveObservations`; clarify `durationMs` vs `duration`.
- [ ] [A-52] `intraday-pnl.allium` IntradayPnlSnapshot: add `missingFxRates`, `dataQualityWarning`.
- [ ] [A-53] `risk.allium` DailyRiskSnapshot: add `varContribution`, `esContribution`, `sodVol`, `sodRate`.
- [ ] [A-54] `factor-model.allium` FactorDecompositionSnapshot: add `concentration_warning`.
- [ ] [A-5] `alerts.allium`: distill the `LIMIT_BREACH` pipeline (Kafka topic `limits.breaches`, `LimitBreachEvent`, `LimitBreachRule`, in-app delivery).

**Acceptance:** `allium check specs/*.allium` passes; new entities/fields validate.

**Execution:** parallel `tend` agents per spec.

### Batch H — Cosmetic type drift (spec-only resolution where possible)

Mostly: align spec types with persisted reality, OR add `@guidance` notes documenting the translation (e.g. `Decimal` ↔ `Double`).

- [ ] [A-57] `SodBaseline.source_job_id` — change to `UUID?` or document.
- [ ] [A-58] `SavedScenario.description` — make non-null in spec.
- [ ] [A-59] `SavedScenario.correlation_override`/`shocks` — update spec to `String?` (JSON) with @guidance.
- [ ] [A-60] `StressTestResult.var_impact` — document Decimal↔Double translation.
- [ ] [A-61] `InstrumentFactorLoading.factor` — change spec to `String` (or change code to enum — needs decision).
- [ ] [A-62] `FxRate.as_of` — rename to `updated_at` in spec.
- [ ] [A-63] `NettingSetSummary` → `NettingSetExposure`.
- [ ] [A-64] `RegimeState.degraded_inputs` default — align.
- [ ] [A-65] Counterparty Decimal/Double — add @guidance note.
- [ ] [A-66] `LiquidityRiskSnapshot.adv_data_as_of` — make nullable in spec.
- [ ] [A-67] `Counterparty.sector` nullability — fix orchestrator DTO (this one IS code).

**Acceptance:** specs validate. One coding ticket carved out for [A-67] only.

**Execution:** parallel `tend` agents.

---

## Phase 2 — Code correctness (TDD; medium risk)

### Batch C — Trivial P0 code fixes

Each is single-file with clear scope and obvious correct behaviour.

- [ ] [A-1] **Wire `MarketRegimeEventConsumer`** in `notification-service/.../Application.kt:235`.
  - Test: integration test that publishes a regime change event and asserts an alert is produced. Acceptance test that the consumer starts on application boot.
- [ ] [A-7] **Implement `AlertOnBudgetBreach`** — replace `TODO(HIER-03)` in `BudgetUtilisationService.kt:63-71` with `RISK_BUDGET_EXCEEDED` alert publish.
  - Test: unit test for breach detection; acceptance test that an alert flows through notification-service.
- [ ] [A-8] **Use regulator-supplied `acknowledged_at`** in `SubmissionService.kt:78-98`.
  - Add `AcknowledgeSubmissionRequest(acknowledgedAt: Instant)` body to `SubmissionRoutes.kt:74-87`.
  - Test: route-level acceptance test asserting the body's timestamp lands in DB.
- [ ] [A-14] **Wire `arrivalPriceTimestamp`** through `OrderRoutes.kt:45-56` → `SubmitOrderRequest` → `OrderSubmissionService.submit`.
  - Test: route test for stale arrival-price rejection.
- [ ] [A-16] **Propagate `correlationId`** in `FIXExecutionReportProcessor.kt:111-121` — set `correlationId = order.orderId` on `BookTradeCommand`.
  - Test: integration test that fill→trade chain shares correlation id.
- [ ] [A-20] **Single-statement `expire_all_pending_past_deadline`** in `ExposedHedgeRecommendationRepository.kt:88-104`.
  - Test: integration test asserting one DB statement (verify via test-container query log) and N rows updated atomically.

**Acceptance:** `./gradlew test acceptanceTest integrationTest` clean for affected modules. `cd ui && npm run lint` if any UI touched (probably none here).

**Execution:** sequential, one commit per item. Can use `architect` subagent for TDD scaffolding.

### Batch D — Input validation across market-data ingestion

One pattern, six routes. Single batch for consistency.

- [ ] [A-4] Add validation in:
  - `price-service/.../PriceRoutes.kt` — price ≥ 0.
  - `rates-service/.../RatesRoutes.kt` — yield curve `tenors.size ≥ 1` and monotonic; risk-free `tenor.isNotBlank()`; forward `points.size ≥ 1`.
  - `volatility-service/.../VolatilityRoutes.kt` — `points.size ≥ 1`, all vols positive, strikes/maturities positive.
  - `correlation-service/.../CorrelationRoutes.kt` — `values.size == labels.size²`, `labels.size ≥ 1`.
- Return HTTP 400 with `ValidationException` on failure. Pattern matches the existing `IllegalArgumentException → 400` mapping in each service.

**Acceptance:** route tests for happy path + every rejection branch. No regression in upstream consumers (acceptance tests in `risk-orchestrator` should still pass).

**Execution:** one commit per service (4 commits). Tests-first.

### Batch E — Schema/data integrity

- [ ] [A-6] **Add unique constraint** on `LimitDefinitionsTable (level, entity_id, limit_type)`.
  - New Flyway migration in `position-service/.../resources/db/migration/`.
  - **CLAUDE.md note:** must NOT use `CREATE INDEX CONCURRENTLY` (Flyway runs in tx).
  - Backfill: detect & remove duplicates first (manual decision needed — flag if any exist in `seed.sql` or live data).
  - Test: integration test asserting duplicate insert fails.
- [ ] [A-22] **Reconcile pre-trade warning threshold** — pick `>=` (matches code) and update `limits.allium:142` strict-`>` text. Spec edit. Confirm with PM/quant if business reading differs.

**Acceptance:** migrations apply cleanly on a fresh DB; existing acceptance tests pass.

---

## Phase 3 — Decisions & contracts (high risk, need input)

### Batch F — Quant decisions

These items need a methodology call before any code change. **Open a `/quant` review for each.**

- [ ] [A-2] Wrong-way risk taxonomy. Decide: keep coarse heuristic (and update spec), or implement strict sector-match (and update code).
- [ ] [A-3] VaR-vs-pricing Greeks for intraday attribution. Decide: build `SodGreekSnapshot` infrastructure, or accept VaR-Greek attribution as an approximation (and update spec to acknowledge).
- [ ] [A-15] Vol-surface diff method. Decide: nearest-neighbour vs interpolation. (Probably interpolation per spec; check perf trade-off.)
- [ ] [A-17] Regime degraded-input behaviour. Decide: stricter (always hold) or spec-conformant (transition if both signals agree).

**Acceptance:** decision memos in `docs/adr/` for each.

### Batch G — Contract drift (cross-cutting)

- [ ] [A-9] Collapse `LiquidityTier` to a single canonical enum. Likely:
  - Delete `reference-data-service/.../InstrumentLiquidityTier.kt`.
  - Use `common/.../LiquidityTier.kt` everywhere.
  - Update `InstrumentLiquidityService` returned tier names.
  - Fix hedge `LIQUID_TIERS` filter to `setOf("HIGH_LIQUID", "LIQUID")`.
  - Test: end-to-end that hedge eligible-instruments filtering still works; UI E2E for liquidity tier display.
- [ ] [A-10] `PositionPriceUpdated` event — decide spec vs code direction.
- [ ] [A-11] Restore `counterpartyId`/`strategyId` on `TradeEventMessage` (or update spec to acknowledge).
- [ ] [A-12] Add `AUTO_CLOSE` carve-out to spec invariant.
- [ ] [A-13] Implement `ExpireDayOrder` scheduled job — needs business call on cutoff time.
- [ ] [A-18] Reconciliation severity tiers — code or spec adjusts.
- [ ] [A-19] Hedge 30-min staleness warning — implement.
- [ ] [A-21] Gateway proxies for hedge accept/reject.
- [ ] [A-23] `UpdateLimit` partial vs full — pick semantics, fix spec or code.
- [ ] [A-24] Per-break threshold check for reconciliation alerts.

**Acceptance:** each item passes its own TDD cycle; cross-service E2E tests cover renamed enums.

**Execution:** sequential; high-risk so single-context-per-item is safer.

### Batch I — Aspirational triage (P2 deferred items 25–41 + 55–56)

For each: decide implement / defer / deprecate-from-spec. This is roadmap work, not cleanup.

**Acceptance:** every P2 item has a status comment in `docs/spec-drift-audit.md`.

---

## Tracking

Update `docs/spec-drift-audit.md` checkbox status (☐ → ✓) as items complete. Each batch ends with a commit referencing `[spec-drift A-N]` where N is the audit item number.

## Context-clearing checkpoints

Recommended `/clear` points:
1. After Batch A commits.
2. After Batch B commits.
3. After Batch C commits.
4. After Batch D commits.
5. After Batch E commits.
6. Per-item for Phase 3 (high-risk).

Each new session begins by re-reading `docs/spec-drift-audit.md` and `docs/plans/spec-drift-cleanup-plan.md`.

## Out of scope

- Phase 2 P2 aspirational implementations (#25-41) — covered in Batch I as triage only.
- Cross-service refactors not flagged in the audit.
- New features.
