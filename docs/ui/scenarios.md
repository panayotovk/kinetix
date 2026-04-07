# Scenarios Tab — Stress Testing & What-If Analysis

The Scenarios tab lets traders and risk managers run stress tests against their current book, replay historical crises with actual return data, reverse-engineer minimum shock vectors, and manage the full governance lifecycle of custom scenarios.

---

## What it displays

The tab is composed of several panels that appear based on state and user interaction:

### ScenarioControlBar

The top control bar provides:
- **Run All Scenarios** button (red, lightning icon) — runs the full predefined scenario batch
- **Confidence level** dropdown — 95% CL, 97.5% CL, 99% CL
- **Time horizon** dropdown — 1 Day, 5 Days, 10 Days
- **+ Custom Scenario** button — opens the `CustomScenarioBuilder` dialog
- **Reverse Stress** button — opens the `ReverseStressDialog` slide-in panel
- **Compare (N)** button — visible when 2–3 scenarios are checked; opens `ScenarioComparisonView`
- **Export CSV** button — visible when results exist; exports the stress result table
- **Manage Scenarios** button — toggles the `ScenarioLibraryGrid` and `ScenarioGovernancePanel`

### ScenarioComparisonTable

The main results table. Columns:
- Checkbox (select up to 3 for comparison)
- Scenario name
- Status badge (from governance metadata: DRAFT / PENDING_APPROVAL / APPROVED / RETIRED)
- Type badge (PARAMETRIC / HISTORICAL_REPLAY / REVERSE_STRESS)
- Base VaR
- Stressed VaR
- P&L Impact
- Asset class impact breakdown

Clicking a row selects it as the active scenario for `ScenarioDetailPanel`.

### ScenarioDetailPanel

Expands below the table when a scenario is selected, showing:
- A bar chart of base vs. stressed exposure per asset class with P&L impact (EQUITY blue, FIXED_INCOME green, COMMODITY amber, FX purple, DERIVATIVE red)
- Full numeric breakdown (base exposure, stressed exposure, P&L impact per class)

### ScenarioComparisonView

Rendered when the user clicks **Compare (N)** with 2 or 3 checked scenarios. Shows a side-by-side comparison of the selected stress results.

---

## Scenario Governance (toggled by "Manage Scenarios")

When governance is open, two additional panels appear:

### ScenarioLibraryGrid

A searchable, filterable, sortable table of all scenarios from the regulatory service:

- **Search bar** — filters by name or description
- **Type filter** dropdown — All / Parametric / Historical Replay / Reverse Stress
- **Sortable columns** — Name, Type, Status (click header to toggle asc/desc)
- **Status badges** — DRAFT (grey), PENDING_APPROVAL (amber), APPROVED (green), RETIRED (red)
- **Type badges** — Parametric (blue), Historical Replay (violet), Reverse Stress (orange)
- Shows `createdBy` for each scenario

### ScenarioGovernancePanel

Workflow actions per scenario based on current status:
- **DRAFT** → "Submit for Approval" button (primary)
- **PENDING_APPROVAL** → "Approve" button (success)
- **APPROVED** → "Retire" button (danger)

Shows the `approvedBy` user where applicable.

---

## HistoricalReplayPanel

Rendered automatically below the main card when at least one APPROVED `HISTORICAL_REPLAY` scenario exists (sourced from the governance list). Provides:
- **Scenario selector** dropdown (only APPROVED HISTORICAL_REPLAY scenarios)
- **Run Replay** button — applies the historical return sequence to current positions
- **Results** — shows scenario name, total P&L impact, historical window (start → end date), and a per-position impact table with instrument ID, asset class, market value, and P&L impact
- **Proxy badge** — displayed inline when asset-class proxy returns were used for an instrument not in the historical dataset

---

## ReverseStressDialog

A slide-in panel (400 px wide, right edge) triggered by the **Reverse Stress** button:
- **Target loss** input — the loss amount to solve for (USD)
- **Maximum shock** input — the upper bound on the shock magnitude (default -1.0 = -100%)
- **Run Reverse Stress** button — calls the reverse stress API
- **Results** — convergence indicator (green if solved, amber if max shock constraint reached), target loss vs. achieved loss, and a per-instrument shock table
- Dismisses on Escape key or click on backdrop

---

## Predefined historical scenarios

| Scenario | Description | Equity Shock | Commodity Shock | FX Shock | FI Shock | Vol Multiplier (Equity) |
|----------|-------------|-------------|----------------|----------|----------|------------------------|
| **GFC 2008** | Global Financial Crisis — severe equity/commodity selloff | -40% | -30% | -15% | -5% | 3.0x |
| **COVID 2020** | Pandemic — rapid equity decline, commodity crash | -35% | -25% | -10% | -3% | 2.5x |
| **Taper Tantrum 2013** | Fixed income selloff, moderate equity decline | -5% | -3% | -5% | -10% | 1.5x |
| **Euro Crisis 2011** | European sovereign debt crisis | -20% | -10% | -15% | -8% | 2.0x |

Each scenario includes:
- **Price shocks** per asset class (how much prices drop)
- **Volatility shocks** per asset class (how much vol spikes)
- **Correlation overrides** (optional) — crisis correlations reflecting assets moving together

---

## How stress tests are calculated

1. Compute **base VaR** with current volatilities and correlations
2. Apply **price shocks** — multiply each position's market value by the shock factor (e.g. 0.60 = 40% loss)
3. Build **stressed volatility matrix** — multiply default vols by scenario vol multipliers
4. Build **stressed correlation matrix** — use crisis-specific overrides if defined
5. Compute **stressed VaR** with the shocked inputs
6. Calculate **per-asset-class impact** — stressed exposure minus base exposure = P&L
7. Return results with base VaR, stressed VaR, total P&L impact, and per-class breakdown

---

## Custom hypothetical scenarios

The **+ Custom Scenario** button opens `CustomScenarioBuilder`, which lets users:
- Define arbitrary price shocks and volatility multipliers per asset class
- Save a named custom scenario to the regulatory service (goes to DRAFT status and is submitted for approval automatically)
- Run an ad-hoc stress test immediately without saving

---

## Why a trader / investment bank needs this

1. **Regulatory requirement** — Regulators require banks to demonstrate they understand how historical crises would impact their current book.
2. **Tail risk quantification** — VaR tells you about "normal bad days"; stress testing tells you about catastrophic days that fall outside VaR confidence intervals.
3. **Hedging validation** — A portfolio hedged against normal volatility may still blow up in a crisis if correlations spike to 1. Stress tests with correlation overrides reveal this.
4. **Scenario planning** — The desk can model custom shocks ("what if oil drops 40% overnight?") to prepare contingency plans.
5. **Reverse stress** — Finding the minimum shock that causes a given loss is more informative than asking "what happens if equities drop X%?" — it identifies how fragile a book is.
6. **Historical replay** — Replaying actual daily returns from past crises against the current book is more grounded than synthetic shocks.
7. **Governance** — The approval workflow ensures scenarios used for risk reporting have been reviewed by a second person before being marked official.

---

## Architecture

```
UI (ScenariosTab)
  ├── ScenarioControlBar — run controls, toggles
  ├── ScenarioComparisonTable — results grid
  ├── ScenarioDetailPanel — asset class bar chart
  ├── ScenarioComparisonView — side-by-side view (2–3 scenarios)
  ├── ScenarioLibraryGrid — governance library (toggled)
  ├── ScenarioGovernancePanel — submit/approve/retire (toggled)
  ├── CustomScenarioBuilder — dialog for custom scenario creation
  ├── HistoricalReplayPanel — Card rendered below main card
  └── ReverseStressDialog — slide-in panel

  → Risk Orchestrator (Ktor, HTTP REST)
    ├── Stress test endpoints → Risk Engine (Python, gRPC StressTestService)
    ├── Historical replay endpoint → Risk Engine
    └── Reverse stress endpoint → Risk Engine

  → Regulatory Service (Ktor, HTTP REST)
    └── Scenario governance endpoints (create, submit, approve, retire)
```

---

## Key files

| Component | Location |
|-----------|----------|
| Scenarios Tab | `ui/src/components/ScenariosTab.tsx` |
| Control Bar | `ui/src/components/ScenarioControlBar.tsx` |
| Comparison Table | `ui/src/components/ScenarioComparisonTable.tsx` |
| Detail Panel | `ui/src/components/ScenarioDetailPanel.tsx` |
| Comparison View | `ui/src/components/ScenarioComparisonView.tsx` |
| Library Grid | `ui/src/components/ScenarioLibraryGrid.tsx` |
| Governance Panel | `ui/src/components/ScenarioGovernancePanel.tsx` |
| Historical Replay Panel | `ui/src/components/HistoricalReplayPanel.tsx` |
| Reverse Stress Dialog | `ui/src/components/ReverseStressDialog.tsx` |
| Custom Scenario Builder | `ui/src/components/CustomScenarioBuilder.tsx` |
| Scenario Tooltip | `ui/src/components/ScenarioTooltip.tsx` |
| Governance Hook | `ui/src/hooks/useScenarioGovernance.ts` |
| Custom Scenario Hook | `ui/src/hooks/useCustomScenario.ts` |
| Stress Test API | `ui/src/api/stress.ts` |
| Scenarios API | `ui/src/api/scenarios.ts` |
| Historical Replay API | `ui/src/api/historicalReplay.ts` |
| Risk Orchestrator Routes | `risk-orchestrator/src/main/kotlin/com/kinetix/risk/routes/RiskRoutes.kt` |
| Stress Engine | `risk-engine/src/kinetix_risk/stress/engine.py` |
| Scenario Definitions | `risk-engine/src/kinetix_risk/stress/scenarios.py` |
| Proto Definitions | `proto/src/main/proto/kinetix/risk/stress_testing.proto` |

---

## API Endpoints

| Route | Method | Purpose |
|-------|--------|---------|
| `/api/v1/risk/stress/scenarios` | GET | List available predefined scenario names |
| `/api/v1/risk/stress/{bookId}` | POST | Run a single stress test (named or custom) |
| `/api/v1/risk/stress/{bookId}/batch` | POST | Run all scenarios, ranked by worst P&L |
| `/api/v1/risk/stress/{bookId}/historical-replay` | POST | Apply historical returns to current positions |
| `/api/v1/risk/stress/{bookId}/reverse` | POST | Reverse stress — find minimum shock for target loss |
| `/api/v1/stress-scenarios` | GET | List all governance scenarios |
| `/api/v1/stress-scenarios/approved` | GET | List approved scenarios |
| `/api/v1/stress-scenarios` | POST | Create a new scenario (DRAFT) |
| `/api/v1/stress-scenarios/{id}/submit` | PATCH | Submit scenario for approval |
| `/api/v1/stress-scenarios/{id}/approve` | PATCH | Approve a scenario |
| `/api/v1/stress-scenarios/{id}/retire` | PATCH | Retire a scenario |

---

## Request / Response

### Stress test request
```json
{
  "scenarioName": "GFC_2008",
  "calculationType": "PARAMETRIC",
  "confidenceLevel": "CL_99",
  "timeHorizonDays": "1"
}
```

Custom scenario:
```json
{
  "scenarioName": "CUSTOM_OIL_SHOCK",
  "description": "Oil price collapse scenario",
  "priceShocks": { "COMMODITY": -0.40, "EQUITY": -0.10 },
  "volShocks": { "COMMODITY": 2.5, "EQUITY": 1.5 }
}
```

### Stress test response
```json
{
  "scenarioName": "GFC_2008",
  "baseVar": "100000.00",
  "stressedVar": "300000.00",
  "pnlImpact": "-550000.00",
  "assetClassImpacts": [
    {
      "assetClass": "EQUITY",
      "baseExposure": "1000000.00",
      "stressedExposure": "600000.00",
      "pnlImpact": "-400000.00"
    }
  ],
  "calculatedAt": "2026-04-07T14:00:00Z"
}
```

### Reverse stress request
```json
{
  "targetLoss": 100000,
  "maxShock": -1.0
}
```

### Reverse stress response
```json
{
  "targetLoss": "100000.00",
  "achievedLoss": "98500.00",
  "converged": true,
  "shocks": [
    { "instrumentId": "AAPL", "shock": "-0.12" },
    { "instrumentId": "MSFT", "shock": "-0.12" }
  ]
}
```
