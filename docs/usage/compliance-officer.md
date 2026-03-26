# Kinetix Compliance Officer Guide

A practical guide to the Kinetix risk management platform from a compliance and regulatory perspective. This covers the governance workflows that a compliance officer owns: model approval, regulatory submissions, stress test authorisation, backtesting oversight, audit chain verification, and FRTB capital reporting.

---

## At a Glance

The tabs most relevant to a compliance officer are:

| Tab | Primary compliance use |
|-----|----------------------|
| **Regulatory** | FRTB capital charges, backtesting, model governance, CSV/XBRL export |
| **Scenarios** | Stress test scenario library, governance workflow, reverse stress |
| **Reports** | Templated report generation and report history |
| **Alerts** | Limit breach notifications, escalation tracking |
| **System** | Service health monitoring |

The platform's compliance-relevant logic lives in three backend services: `regulatory-service` (model governance, backtesting, submissions, FRTB), `audit-service` (hash-chained event trail), and `position-service` (limits).

---

## 1. Model Governance Framework

### Model lifecycle

Every risk model must be registered in the model inventory before it may be used in a governed capacity. The registry enforces a strict four-state lifecycle:

```
DRAFT  ->  VALIDATED  ->  APPROVED  ->  RETIRED
```

Transitions are unidirectional. A model cannot move backwards or skip states. Material changes require registering a new version rather than mutating an approved one.

### Self-approval is blocked

The system enforces that `approvedBy` cannot match `registeredBy` on the same record. An approval attempt where the approver and registrant are the same user is rejected. This is the four-eyes control for the model lifecycle.

### Working with the registry

| Action | Endpoint | Notes |
|--------|----------|-------|
| Register new version | `POST /api/v1/models` | Created in DRAFT status. Parameters field carries the full model configuration. |
| Transition status | `PATCH /api/v1/models/{id}/status` | For APPROVED transition, `approvedBy` must differ from `registeredBy`. |
| List all versions | `GET /api/v1/models` | Returns all versions with status, registration metadata, and approval timestamps. |

### Model review checklist

| Check | Expected evidence |
|-------|------------------|
| All production models have status APPROVED | No model in DRAFT or VALIDATED running against live books |
| Every approval has a distinct approver | `registeredBy` != `approvedBy` on every APPROVED record |
| Retired models are not referenced in active calculations | Risk-orchestrator is not routing to retired versions |
| Parameter changes result in a new version | Version string increments; prior version is RETIRED, not mutated |

---

## 2. Regulatory Submissions (Four-Eyes Workflow)

### Submission lifecycle

```
DRAFT  ->  PENDING_REVIEW  ->  APPROVED  ->  SUBMITTED  ->  ACKNOWLEDGED
```

The four-eyes control is enforced at the PENDING_REVIEW -> APPROVED transition: the system rejects an approval where `approverId` matches `preparerId`.

### Workflow

| Step | Action | Who |
|------|--------|-----|
| Create | `POST /api/v1/submissions` with report type, preparer ID, and deadline | Preparer |
| Submit for review | `PATCH /api/v1/submissions/{id}/review` | Preparer |
| Approve | `PATCH /api/v1/submissions/{id}/approve` with approver ID | Approver (different person) |
| Final submit | `PATCH /api/v1/submissions/{id}/submit` | Either |
| List all | `GET /api/v1/submissions` | Any authorised user |

Every approval event is published to the `governance.audit` Kafka topic and persisted in the hash-chained audit trail with `eventType = SUBMISSION_APPROVED`, `userId = approverId`, and `userRole = APPROVER`.

### Governance checks

Periodically query the submission list and confirm that `preparerId` and `approverId` are always distinct on any record with status APPROVED or beyond. The system blocks self-approval, but the control should be verified independently.

---

## 3. FRTB Capital Charges

### What the calculation covers

The Regulatory tab computes Standardised Approach FRTB charges:

| Component | Description |
|-----------|-------------|
| **SBM** (Sensitivities-Based Method) | Delta, Vega, and Curvature charges per risk class |
| **DRC** (Default Risk Charge) | Jump-to-default exposure with 21-bucket credit rating granularity, seniority-adjusted LGD, maturity weighting, and sector concentration penalties |
| **RRAO** (Residual Risk Add-On) | Notional-based charge for exotic instruments not captured by SBM |

Total capital requirement = SBM + DRC + RRAO.

### Running a calculation

From the **Regulatory** tab, select a book and click **Calculate FRTB**. Results are persisted with a unique calculation ID and timestamp. The dashboard displays:

- **Total Capital** headline figure
- **SBM / DRC / RRAO** proportional breakdown bar
- **SBM by risk class** -- Delta, Vega, and Curvature charges per active risk class (GIRR, CSR, Equity, FX, Commodity)

### Downloading reports

- **CSV** -- flat tabular export for internal filing and reporting tool import.
- **XBRL** -- structured markup for COREP submissions under the EBA taxonomy.

Both are generated from the persisted calculation record (not recalculated at download time), so the submitted figure is reproducible from the stored record ID.

### Calculation history

| Endpoint | Purpose |
|----------|---------|
| `GET /api/v1/regulatory/frtb/{bookId}/history` | Paginated history, most recent first |
| `GET /api/v1/regulatory/frtb/{bookId}/latest` | Current regulatory position |

Retain the full calculation history. The platform retains valuation job data for one year by default; ensure FRTB records are subject to at least a five-year retention policy for capital reporting.

---

## 4. Stress Test Scenario Governance

### Scenario lifecycle

```
DRAFT  ->  PENDING_APPROVAL  ->  APPROVED  ->  RETIRED
```

A scenario cannot be run against a live book until it reaches APPROVED status. The service rejects execution requests against non-approved scenarios. This governance gate is enforced in code.

Modifying an APPROVED scenario resets it to DRAFT and clears approval metadata, forcing the workflow to repeat. The version counter increments on each update.

### Scenario categories

| Category | Meaning |
|----------|---------|
| `REGULATORY_MANDATED` | Required by prudential regulation (Basel III/FRTB) |
| `INTERNAL_APPROVED` | Designed by the risk committee for internal stress programmes |
| `SUPERVISORY_REQUESTED` | Commissioned by a supervisory authority for SREP or thematic review |

Only APPROVED scenarios appear in the approved-scenarios endpoint that the trading desk uses. The **Scenario Governance** panel on the **Scenarios** tab shows all scenarios with their status.

### Pre-seeded scenarios

The platform ships with 15 pre-seeded APPROVED scenarios:

**Regulatory Mandated (8):** GFC 2008, COVID March 2020, Taper Tantrum 2013, Euro Sovereign Crisis 2011, Black Monday 1987, LTCM/Russian Default 1998, Dot-com bust 2000, September 11 2001.

**Internal Approved (7):** CHF depeg 2015, Brexit 2016, Volmageddon 2018, Oil negative prices April 2020, Russia-Ukraine invasion 2022, China tech crackdown 2021, UK gilt crisis 2022.

Four calibrated historical replay periods are also available for return-based replay: GFC October 2008, COVID March 2020, Taper Tantrum 2013, Euro Crisis 2011.

### Approving and retiring

| Action | Endpoint | Audit event |
|--------|----------|-------------|
| Submit for approval | `PATCH /api/v1/stress-scenarios/{id}/submit` | -- |
| Approve | `PATCH /api/v1/stress-scenarios/{id}/approve` | `SCENARIO_APPROVED` |
| Retire | `PATCH /api/v1/stress-scenarios/{id}/retire` | `SCENARIO_RETIRED` |

A retired scenario cannot be updated or run. The record is permanent evidence of which scenarios were active during specific periods.

### Reverse stress testing

`POST /api/v1/stress-scenarios/reverse-stress` computes the minimum-norm shock vector that produces a specified target loss. This satisfies the reverse stress testing requirement under ICAAP/SREP. The system validates `targetLoss > 0` and returns a `converged` flag -- non-converged results cannot be saved as authoritative.

---

## 5. Backtesting Oversight

### Statistical tests

| Test | What it measures | Pass criterion |
|------|-----------------|----------------|
| **Kupiec POF** | Whether the observed violation rate is consistent with the model's confidence level | p-value > 0.05 |
| **Christoffersen** | Whether violations cluster in time (independence of exceptions) | p-value > 0.05 |

A violation is a day where the actual P&L loss exceeds the predicted VaR. Kupiec detects models that violate too often or too rarely. Christoffersen detects clustering -- a model that passes on frequency but fails in consecutive days is broken in exactly the conditions that matter.

### Traffic light zones (250-day window at 99% CL)

| Zone | Violations | Regulatory implication |
|------|-----------|----------------------|
| **GREEN** | 0-4 | Model acceptable. No supervisory action. |
| **YELLOW** | 5-9 | Increased scrutiny. Document investigation. Potential capital multiplier increase. |
| **RED** | 10+ | Model presumed defective. Immediate investigation and likely replacement. |

These thresholds implement the Basel Committee's standard traffic light approach (BCBS d457, paragraphs 183-189).

### Running and comparing backtests

| Endpoint | Purpose |
|----------|---------|
| `POST /api/v1/regulatory/backtest/{bookId}` | Submit VaR predictions and daily P&L for backtesting |
| `GET /api/v1/regulatory/backtest/{bookId}/latest` | Current backtest result |
| `GET /api/v1/regulatory/backtest/{bookId}/history` | Paginated history |
| `GET /api/v1/regulatory/backtest/{bookId}/compare?baseId=...&targetId=...` | Side-by-side comparison |

The response carries violation count, violation rate, both test statistics and p-values, pass/fail flags, the traffic light zone, and a SHA-256 digest of the input data. The digest provides tamper-evident proof that results were computed from the submitted inputs.

The **Run Compare** sub-tab on the **Risk** tab surfaces the backtest comparison in the UI, including traffic light indicators for both runs.

### Escalation thresholds

Any YELLOW or RED result requires documented investigation addressing: (1) Were market conditions outside the model's calibration window? (2) Were there data quality issues? (3) Is there violation clustering (Christoffersen failing while Kupiec passes)? Retain findings alongside the backtest result ID.

---

## 6. Audit Trail

### Hash chain mechanism

Every material event -- trade bookings, amendments, cancellations, model status changes, scenario approvals, submission approvals -- is persisted with a SHA-256 hash chain. Each record carries:

- `recordHash` -- SHA-256 of the event's material fields concatenated with the previous record's hash
- `previousHash` -- the hash of the immediately preceding record (null for the first record)

Any alteration to a historical record causes every subsequent hash to become invalid. Tampering is detectable without access to the original data.

### Event types captured

**Trade lifecycle:** TRADE_BOOKED, TRADE_AMENDED, TRADE_CANCELLED

**Governance events** (from `governance.audit` Kafka topic):

| Event type | Trigger | Key fields |
|------------|---------|------------|
| MODEL_STATUS_CHANGED | Model version status transition | modelName, transition detail |
| SCENARIO_APPROVED | Stress scenario approved | scenarioId |
| SCENARIO_RETIRED | Stress scenario retired | scenarioId |
| SUBMISSION_APPROVED | Regulatory submission approved | submissionId |
| STRESS_TEST_RUN | Scenario executed against a book | scenarioId, bookId |

Each event carries `userId`, `userRole`, and the specific identifiers needed to cross-reference governance records.

### Querying audit events

| Endpoint | Purpose |
|----------|---------|
| `GET /api/v1/audit/events?afterId=0&limit=1000` | Cursor-based paginated list (max 10,000 per page) |
| `GET /api/v1/audit/events?bookId={bookId}` | Events for a specific book |

### Chain verification

```
GET /api/v1/audit/verify

Response: { "valid": true, "eventCount": 24871 }
```

Walks the entire chain in batches of 10,000 records. `valid: false` means a broken chain -- treat as a serious control failure. Escalate to information security immediately. Do not promote any EOD or regulatory submission until resolved.

Run chain verification monthly at minimum, and before any regulatory submission or examination. Record the `eventCount` alongside the `valid` flag.

### Retention

The audit service retention policy is **7 years**, consistent with Basel capital reporting, MiFID II transaction reporting, and most national prudential requirements. Confirm this against your specific regulatory perimeter before reducing.

---

## 7. Regulatory Reporting

### Report generation

The **Reports** tab provides templated report generation. Select a template, book, and date, then Generate. Output downloads as CSV. History is retained for prior access.

For FRTB-specific reporting, use the export buttons on the **Regulatory** tab -- these produce book-specific CSV and XBRL directly from the most recent FRTB calculation.

### XBRL submission readiness

Before submitting to a supervisor:

1. Confirm the calculation was triggered against the correct book scope.
2. Confirm the `calculatedAt` timestamp falls within the reporting reference period.
3. Reconcile SBM, DRC, and RRAO figures against your internal capital model output.
4. Validate the XBRL file against the applicable EBA taxonomy version.

The XBRL is generated from the persisted record, not recalculated at download time. The submitted figure is reproducible from the stored record ID.

---

## 8. Data Quality Monitoring

### The data quality indicator

A traffic light indicator in the header is visible on every screen:

| State | Meaning |
|-------|---------|
| Green | All data quality checks passing |
| Amber | One or more checks degraded but not critical |
| Red | Critical failure -- risk numbers may be unreliable |

Click to expand a dropdown listing each check by name, status, and message.

### Compliance implications of CRITICAL status

A CRITICAL data quality status means risk calculations are based on stale or incomplete inputs. Do not submit FRTB figures, sign off on capital returns, or use stress test outputs for regulatory purposes when the indicator is red. The sequence is: investigate, resolve, re-run calculations, confirm green, then produce the submission. Document this sequence when it occurs.

---

## 9. Limit Management and Breach Oversight

### Limit hierarchy

Limits cascade: **FIRM > DESK > TRADER > COUNTERPARTY**. Each level has intraday and overnight thresholds. Hard limits block trades; soft limits allow execution but trigger warnings and audit records.

The hierarchy is the operational implementation of the board-approved risk appetite. From a compliance perspective, it is the primary quantitative control between risk tolerance and individual trading activity.

### Alert management

The **Alerts** tab surfaces breaches with severity (INFO / WARNING / CRITICAL) and lifecycle (TRIGGERED > ACKNOWLEDGED > ESCALATED > RESOLVED).

Ensure that:

- Every CRITICAL alert is acknowledged within your escalation policy's time standard.
- ESCALATED alerts have a documented resolution path.
- Alert history is exported monthly for compliance record-keeping.

---

## 10. Daily Compliance Workflow

### Morning

1. **Data quality.** Confirm the indicator is green. Risk figures under degraded data state must not be used for regulatory reporting.
2. **Overnight alert review.** Check **Alerts** for TRIGGERED or ESCALATED alerts. Confirm hard limit breaches have documented investigations.
3. **Scenario governance queue.** Review PENDING_APPROVAL scenarios on the **Scenarios** tab. Assess shock vector magnitudes and category classification before approving.

### During the day

4. **Model registry.** Check for models in VALIDATED status awaiting your approval. Confirm validation documentation in the `parameters` field is sufficient.
5. **Submission pipeline.** Check submissions approaching deadlines. Verify no submission has the same individual as both preparer and approver.

### End of day / periodic

6. **FRTB (daily).** Trigger FRTB for all in-scope books. Review the SBM/DRC/RRAO breakdown for material movements. Download CSV.
7. **Backtest review (weekly).** Submit updated VaR/P&L series. Review traffic light zone. Investigate any GREEN-to-YELLOW transition immediately.
8. **Alert history export (monthly).** Export to CSV for the month's limit monitoring evidence.
9. **Audit chain verification (monthly at minimum).** `GET /api/v1/audit/verify`. Record `valid` flag, `eventCount`, and timestamp in your control evidence file.
