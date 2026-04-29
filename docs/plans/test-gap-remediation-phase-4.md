# Test Gap Remediation — Phase 4

Follow-on to the three phases in `docs/test-gap-remediation-plan.md` (all complete). This phase addresses gaps that have emerged since that plan closed — primarily around features added in Phase 5–6 of the platform roadmap and routes/consumers that were never back-filled with contract or integration tests.

Audit source: QA review run on 2026-04-22. See `docs/evolution-report.md` for the full context of the work this plan covers.

## Progress (as of 2026-04-22)

**Phase 4A — COMPLETE (6/6)**
- [x] 4A.1 LimitRoutesAcceptanceTest — 8 tests green (`position-service`)
- [x] 4A.2 BookHierarchyRoutesAcceptanceTest — 7 tests green (`position-service`)
- [x] 4A.3 InternalRoutesAcceptanceTest — 4 tests green (`position-service`)
- [x] 4A.4 AlertLifecycleEvent schema compat — 4 tests green (`schema-tests`)
- [x] 4A.5 BookVaRContributionEvent schema compat — 3 tests green (`schema-tests`)
- [x] 4A.6 CrossBookRiskResult + LiquidityRisk schema compat — 6 tests green (`schema-tests`)

**Phase 4B — IN PROGRESS (7/8, 1 deferred)**
- [x] 4B.1 ExposedModelVersionRepositoryIntegrationTest — 5 tests green (`regulatory-service`)
- [x] 4B.2 ExposedSubmissionRepositoryIntegrationTest — 5 tests green (`regulatory-service`)
- [x] 4B.3 MarketRegimeEventConsumerTest — 5 tests green (`notification-service`)
- [ ] 4B.4 PagerDutyDeliveryServiceTest — **DEFERRED** (stub implementation, no HTTP client)
- [x] 4B.5 GatewayExecutionProxyContractAcceptanceTest — 6 tests green (`gateway`)
- [x] 4B.6 Gateway regime contract — 5 tests green; **margin sub-item deferred** (route not wired)
- [x] 4B.7 KafkaFIXSessionEventPublisherIntegrationTest — 2 tests green (`position-service`)
- [x] 4B.8 KafkaReconciliationAlertPublisherIntegrationTest — 3 tests green (`position-service`)

**Phase 4C — COMPLETE (3/5 done, 2 deferred)**
- [x] 4C.1 Reference-data repos — Instrument (7) + NettingAgreement (5) integration tests green
- [x] 4C.2 UI component Vitest — AlertDrillDownPanel (11) + VaRAttributionPanel (8) + EodTimelineTab (7) green
- [x] 4C.3 UI API module tests — execution.ts (8) + regime.ts (7) green
- [ ] 4C.4 margin.spec.ts Playwright — **DEFERRED** (UI has no margin panel)
- [ ] 4C.5 limit-management.spec.ts Playwright — **DEFERRED** (no limit-management UI surface)

**Phase 4D (P3) — COMPLETE (2/3, 1 deferred)**
- [x] 4D.1 CounterpartyRoutesAcceptanceTest — 5 tests green (`position-service`)
- [x] 4D.2 Regulatory-service HTTP client unit tests — Correlation (6) + Price (6) + RiskOrchestrator (7) = 19 tests green
- [ ] 4D.3 Cross-service limit-breach → alert-escalation E2E — **DEFERRED** (no LimitBreachEvent on Kafka; breaches are thrown synchronously)

**Totals:** 18 test classes landed across 7 services, **127 new tests**, all green.
Deferred with documented rationale: 4B.4 PagerDuty, 4B.6 margin sub-item, 4C.4 UI margin spec, 4C.5 UI limit-management spec, 4D.3 limit-breach → alert E2E.

### Findings surfaced during implementation
1. **`PagerDutyDeliveryService` is a stub** (`TODO(ALT-04)`) — no HTTP client, no retries. Testing would be shallow.
2. **`Route.marginRoutes` is orphaned** — defined in `MarginRoutes.kt` but not referenced from any `Application.module(...)` overload. The gateway `/api/v1/books/{bookId}/margin` endpoint returns 404 at runtime. UI doesn't call it either. Requires wiring fix before its tests are meaningful.
3. **`countSince` in `TradeEventRepository` filters on row `createdAt`, not trade `tradedAt`** — test in 4A.3 was adjusted to reflect this. The reconciliation job depends on this semantics; documenting here in case it drives a later feature decision.
4. **No limit-management UI surface exists** — 4C.5 was scoped on the assumption that the FIRM/DESK/TRADER/COUNTERPARTY limit hierarchy from the position-service backend is rendered somewhere in the UI. It is not. The only limit-related UI is `LimitBreachCard` (stress-scenario breaches under `ScenarioDetailPanel`) and `useVarLimit` (reads VAR_BREACH alert-rule threshold). Revive 4C.5 once a limit-management UI lands.
5. **`/api/v1/counterparty-exposure` does not 404 on unknown bookId** — the route returns an empty list. 4D.1 was written to assert the actual behaviour; if the contract should change to 404, that's a separate decision.
6. **No `LimitBreachEvent` Kafka chain exists** — 4D.3 was scoped on the assumption that a FIRM-limit breach during trade booking publishes a Kafka event consumed by risk-orchestrator and routed to notification-service for an in-app alert with an escalation timer. In reality, position-service throws `LimitBreachException` synchronously and the request fails with 4xx; there is no Kafka topic for limit breaches, no risk-orchestrator consumer, and no notification-service rule for limit-breach alerts (the existing `VAR_BREACH` rule fires off risk-result events instead). Synchronous enforcement is already covered by `LimitEnforcementAcceptanceTest` in position-service. Revive 4D.3 once the breach-event flow is built — it would be a meaningful end-to-end test once the wiring exists.

---

## Guiding principles

- **Follow existing patterns.** Every new test should mirror an existing sibling — `LimitRoutesAcceptanceTest` should read like `TradeLifecycleAcceptanceTest`; a new schema compat test should read like `TradeEventSchemaCompatibilityTest`.
- **Do not write tests to hit coverage numbers.** Each item below has a concrete failure mode it protects against. If the failure mode is not compelling, drop the item from the plan rather than writing a shallow test.
- **Keep each item in its own commit.** Commit granularity is one test class (or one paired fix + test) per commit, following the existing convention.
- **Red-Green-Refactor.** Write the test, confirm it runs against the real behaviour (not just a mock reflecting itself), then commit. If the test exposes a bug, raise it before patching.

---

## Scope summary

| Priority | Count | Effort | Focus |
|---|---|---|---|
| P0 | 6 items | ~1–1.5 days | Route acceptance tests on untested HTTP contracts; schema compatibility for unprotected Kafka events |
| P1 | 8 items | ~2 days | Repository integration tests, Kafka publisher integration, untested consumers and gateway routes |
| P2 | 5 items | ~1.5 days | Reference-data repositories, UI component unit tests, Playwright browser coverage |
| P3 | 3 items | ~0.5 day | Nice-to-haves — proxy contract test, HTTP client unit tests, cross-service E2E |

Total estimate: **~5 days** of focused implementation, assuming no major bugs surface (each exposed bug adds triage + fix time).

---

## Phase 4A — P0: critical gaps (~1–1.5 days)

Goal: close gaps where a silent regression could let bad data through a regulatory control, a live-trade path, or a cross-service Kafka contract.

### 4A.1 `LimitRoutesAcceptanceTest`
- **Module:** `position-service`
- **File to create:** `position-service/src/test/kotlin/com/kinetix/position/routes/LimitRoutesAcceptanceTest.kt`
- **Pattern to follow:** `TradeLifecycleAcceptanceTest` (Ktor `testApplication` harness).
- **Cases:**
  1. `POST /api/v1/limits` with a valid FIRM-level limit returns 201 and the persisted limit.
  2. `POST` with unknown `level` enum (e.g. `"GALAXY"`) returns 400 with error body `{ "error": "invalid level" }`.
  3. `POST` with non-numeric `limitValue` returns 400.
  4. `PUT /api/v1/limits/{id}` for a missing id returns 404.
  5. `GET /api/v1/limits?level=DESK&entityId=eq-desk` returns only matching limits.
  6. Temporary limit increase POST `/api/v1/limits/{id}/temporary-increase` persists with a TTL and is retrievable.
- **Acceptance criterion:** All six cases green. Run: `./gradlew :position-service:acceptanceTest --tests LimitRoutesAcceptanceTest`.

### 4A.2 `BookHierarchyRoutesAcceptanceTest`
- **Module:** `position-service`
- **File to create:** `position-service/src/test/kotlin/com/kinetix/position/routes/BookHierarchyRoutesAcceptanceTest.kt`
- **Cases:**
  1. `GET /api/v1/book-hierarchy` returns the seeded hierarchy.
  2. `POST /api/v1/book-hierarchy` with `{bookId, deskId, firmId}` creates the mapping.
  3. `POST` with a missing required field returns 400.
  4. `GET /api/v1/book-hierarchy/{bookId}` returns the node or 404.
- **Acceptance criterion:** Run: `./gradlew :position-service:acceptanceTest --tests BookHierarchyRoutesAcceptanceTest`.

### 4A.3 `InternalRoutesAcceptanceTest`
- **Module:** `position-service`
- **File to create:** `position-service/src/test/kotlin/com/kinetix/position/routes/InternalRoutesAcceptanceTest.kt`
- **Cases:**
  1. `GET /api/v1/internal/trades/count` (no `since`) returns 400.
  2. `GET /api/v1/internal/trades/count?since=not-a-date` returns 400 with an informative error.
  3. `GET /api/v1/internal/trades/count?since=<valid-iso>` returns 200 with the correct count. Seed 3 trades, assert `count == 3`.
  4. Future timestamp returns `count == 0`.
- **Acceptance criterion:** Run: `./gradlew :position-service:acceptanceTest --tests InternalRoutesAcceptanceTest`.

### 4A.4–4A.6 Schema compatibility tests for 4 Kafka events
- **Module:** `schema-tests`
- **Files to create** (one per event, following the existing `TradeEventSchemaCompatibilityTest.kt` pattern):
  1. `AlertLifecycleEventSchemaCompatibilityTest.kt`
  2. `BookVaRContributionEventSchemaCompatibilityTest.kt`
  3. `CrossBookRiskResultEventSchemaCompatibilityTest.kt`
  4. `LiquidityRiskEventSchemaCompatibilityTest.kt`
- **Cases per file:**
  1. A current producer payload deserialises correctly on the consumer side.
  2. All `@Serializable` required fields are present.
  3. An unknown field in the payload is ignored, not rejected (forward compatibility).
  4. A removed optional field falls back to its default (backward compatibility).
- **Acceptance criterion:** Run: `./gradlew :schema-tests:test`.
- **Note:** Group the 4 tests into a single commit if they're symmetrical; split if any event has quirks.

---

## Phase 4B — P1: high-risk gaps (~2 days)

### 4B.1 `ExposedModelVersionRepositoryIntegrationTest`
- **Module:** `regulatory-service`
- **File to create:** `regulatory-service/src/test/kotlin/com/kinetix/regulatory/persistence/ExposedModelVersionRepositoryIntegrationTest.kt`
- **Pattern to follow:** `ExposedBacktestResultRepositoryIntegrationTest`.
- **Cases:**
  1. `save` persists a DRAFT model version; `findById` round-trips all fields.
  2. Status transition DRAFT → IN_REVIEW → APPROVED persists correctly.
  3. Illegal transition APPROVED → DRAFT is rejected (either at repo or service layer — document which).
  4. `findByStatus(APPROVED)` returns only approved versions.
- **Acceptance criterion:** Run: `./gradlew :regulatory-service:integrationTest --tests ExposedModelVersionRepositoryIntegrationTest`.

### 4B.2 `ExposedSubmissionRepositoryIntegrationTest`
- **Module:** `regulatory-service`
- **Cases:**
  1. Save/find round-trip with preparer and approver fields populated.
  2. Four-eyes constraint: a submission with preparer == approver is rejected. Confirm which layer enforces this and test accordingly.
  3. Status filter queries return the right subset.

### 4B.3 `MarketRegimeEventConsumerTest`
- **Module:** `notification-service`
- **File to create:** `notification-service/src/test/kotlin/com/kinetix/notification/MarketRegimeEventConsumerTest.kt`
- **Pattern to follow:** `RiskResultConsumerTest`.
- **Cases:**
  1. A well-formed regime change event triggers the matching `RegimeChangeRule` and publishes an alert.
  2. A payload with an unexpected regime enum is routed to the DLQ, not swallowed.
  3. A payload with null required fields is rejected at deserialisation time.

### 4B.4 `PagerDutyDeliveryServiceTest` — DEFERRED (blocked on implementation)
- **Module:** `notification-service`
- **Status:** Deferred. The current `PagerDutyDeliveryService` is a stub
  (`TODO(ALT-04)` — no HTTP client, no retry, no routing key config). Testing
  retry/error paths against a stub would be shallow coverage.
- **Unblock when:** the real PagerDuty Events API v2 client lands. At that
  point revive this item with the originally scoped cases:
  1. A successful delivery sends the correctly formatted PagerDuty Events API v2 payload.
  2. A 429 response triggers one retry, then backs off.
  3. A 401 response is logged as error and NOT retried.
  4. The `routing_key` is read from config, not hard-coded.

### 4B.5 `GatewayExecutionProxyContractAcceptanceTest`
- **Module:** `gateway`
- **Pattern to follow:** any existing `Gateway*ContractAcceptanceTest` using MockEngine-backed upstream.
- **Cases:**
  1. Happy-path order submission proxies request/response faithfully.
  2. Upstream 400 surfaces as 400 with body preserved.
  3. Upstream 503 surfaces as 503 (no silent swallow).
  4. Upstream timeout surfaces as 504 or whatever the contract says — verify the existing behaviour, don't change it.
- **Note:** this is the most critical route in the system. The test must be strict about error propagation.

### 4B.6 `GatewayMarketRegimeContractAcceptanceTest` (margin split out — see note)
- **Module:** `gateway`
- **Cases:** Happy path, upstream 404, upstream 503. Minimal but enough to catch silent path changes.
- **Margin deferred (blocked on wiring):** `Route.marginRoutes` is defined in
  `gateway/src/main/kotlin/com/kinetix/gateway/routes/MarginRoutes.kt` but
  is not referenced from any `Application.module(...)` overload, so the
  gateway margin endpoint returns 404 at runtime. The UI also does not call
  it. Writing a contract test against an unwired route would conceal the
  fact that the feature is unreachable. Revive this item after the route
  is wired in (or the route is removed if the feature is shelved).

### 4B.7 `KafkaFIXSessionEventPublisherIntegrationTest`
- **Module:** `position-service`
- **Pattern to follow:** `KafkaTradeEventPublisherIntegrationTest` (Testcontainers Kafka).
- **Cases:**
  1. Publish a FIX session disconnected event; consume it with a plain KafkaConsumer; assert payload shape.
  2. Publish a session connected event; same.
  3. Serialization failure is surfaced as an exception, not swallowed.

### 4B.8 `KafkaReconciliationAlertPublisherIntegrationTest`
- **Module:** `position-service`
- **Cases:** Mirror 4B.7 for reconciliation break alerts.

---

## Phase 4C — P2: moderate gaps (~1.5 days)

### 4C.1 Reference-data repository integration tests
- **Module:** `reference-data-service`
- **Files to create:**
  1. `ExposedInstrumentRepositoryIntegrationTest.kt` — covers JSONB attribute round-trip, findByAssetClass, list pagination.
  2. `ExposedNettingAgreementRepositoryIntegrationTest.kt` — covers lookup by counterparty.
- **Deferred (add only if time permits):** Benchmark and Counterparty repos — less critical because they're read-mostly lookups with simple schemas.

### 4C.2 UI component Vitest tests — top 3 only
Do NOT attempt all 16 at once. Write these three first; if any reveal patterns worth applying to the others, then fan out.
- **Files to create:**
  1. `ui/src/components/__tests__/AlertDrillDownPanel.test.tsx` — renders alert detail, escalation state, and action buttons.
  2. `ui/src/components/__tests__/VaRAttributionPanel.test.tsx` — renders decomposed VaR contributions with correct number formatting.
  3. `ui/src/components/__tests__/EodTimelineTab.test.tsx` — renders timeline states and phase transitions.
- **Pattern to follow:** any existing `*.test.tsx` in `ui/src/components/__tests__/`.

### 4C.3 UI API module unit tests — top 2 only
- **Files to create:**
  1. `ui/src/api/__tests__/execution.test.ts` — order submission request/response mapping, error propagation.
  2. `ui/src/api/__tests__/regime.test.ts` — current and historical regime endpoints.

### 4C.4 Playwright: `margin.spec.ts`
- **File to create:** `ui/e2e/margin.spec.ts`
- **Pattern to follow:** `ui/e2e/counterparty.spec.ts` or similar data-panel specs.
- **Cases:**
  1. Margin panel renders `initialMargin`, `variationMargin`, `totalMargin`.
  2. Loading state visible during fetch.
  3. Error state visible when API returns 500.

### 4C.5 Playwright: `limit-management.spec.ts`
- **File to create:** `ui/e2e/limit-management.spec.ts`
- **Cases:**
  1. Limits render per hierarchy level (FIRM → DESK → TRADER → COUNTERPARTY).
  2. Breach indicator visible when a limit is exceeded.
  3. Temporary increase displays current expiry.

---

## Phase 4D — P3: nice-to-have (~0.5 day)

### 4D.1 `CounterpartyRoutesAcceptanceTest`
- **Module:** `position-service`
- **Cases:** `GET /api/v1/books/{bookId}/counterparty-exposure` happy path; `BigDecimal` field serialises as string; 404 on unknown book.

### 4D.2 Regulatory-service HTTP client unit tests
- **Module:** `regulatory-service`
- **Files to create:**
  1. `CorrelationServiceClientTest.kt`
  2. `PriceServiceClientTest.kt`
  3. `RiskOrchestratorClientTest.kt`
- **Pattern to follow:** any existing `*ClientTest.kt` in risk-orchestrator using MockEngine.
- **Cases per client:** Happy path; 404 handling; 500 handling; timeout handling.

### 4D.3 End-to-end: limit breach → alert escalation
- **Module:** `end2end-tests`
- **File to create:** `end2end-tests/src/test/kotlin/com/kinetix/e2e/LimitBreachEscalationEnd2EndTest.kt`
- **Cases:** Book a trade that breaches a FIRM limit; assert `LimitBreach` event flows position-service → risk-orchestrator → notification-service; assert an in-app alert is delivered; assert escalation timer starts.
- **Acceptance criterion:** Run: `./gradlew :end2end-tests:end2EndTest --tests LimitBreachEscalationEnd2EndTest`.

---

## Execution order

Work items in this order — earlier items unblock or de-risk later ones:

1. **4A (P0)** — close the critical gaps first. Any bugs exposed here are likely to affect later work.
2. **4B.1–4B.2** — regulatory repos. Independent of everything else, good to run in parallel.
3. **4B.3–4B.4** — notification-service consumers. Independent.
4. **4B.5–4B.6** — gateway contract tests. Independent.
5. **4B.7–4B.8** — Kafka publisher integration tests (Testcontainers — slower).
6. **4C** — UI + reference-data in whatever order is convenient.
7. **4D** — only if time remains.

Avoid batching items across priorities in the same commit. One test class per commit, following the existing convention.

---

## Done criteria for the phase

- All P0 items green on CI.
- All P1 items green on CI.
- P2 items tackled to the listed subset (3 components, 2 APIs, 2 Playwright specs, 2 repos); remaining items in a backlog issue.
- P3 items either done or explicitly deferred with a backlog issue referencing this plan.
- `MEMORY.md` updated with "Phase 4 complete" marker matching the existing `Phase 1–3` entries.

---

## Risks / unknowns

- **Testcontainers Kafka** — items 4B.7 and 4B.8 will be slower on CI. Confirm the position-service integration-test profile is already set up before starting (Phase 3 added `KafkaTradeEventPublisherIntegrationTest` so it should be).
- **Four-eyes enforcement location** — item 4B.2 requires confirming whether the constraint lives in the repo or the service. Read the existing code before writing the test.
- **Execution proxy error-propagation contract** — item 4B.5 assumes the current behaviour is intentional. If the test surfaces inconsistent behaviour, escalate before patching — this is a live-trade path.
- **Playwright feature visibility** — items 4C.4 and 4C.5 assume the margin panel and limit management UI are wired through to visible routes. If either is gated behind a feature flag or unreleased, move it to the backlog.
