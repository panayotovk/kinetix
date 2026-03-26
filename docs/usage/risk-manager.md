# Kinetix Risk Manager Guide

A practical guide to the Kinetix risk management platform from the risk oversight perspective. This covers your daily workflow: monitoring firm-wide risk, managing limits, approving governance artefacts, investigating VaR changes, and producing the official end-of-day record.

---

## At a Glance

The same **11 tabs** the trading desk uses are available to you. Your primary tabs are different from theirs.

| Tab | Risk Manager's use |
|-----|--------------------|
| **Risk** | VaR/ES dashboard, limit utilisation, run comparison, attribution, market data quality |
| **Scenarios** | Scenario library governance, run all, historical replay, reverse stress |
| **Regulatory** | FRTB capital, backtesting, model governance, submission workflow |
| **Alerts** | Rule management, breach triage, escalation queue |
| **EOD History** | Promote official EOD, historical comparison, trend monitoring |
| **Counterparty Risk** | PFE, CVA, netting, concentration |
| **Positions** | Read-only portfolio view scoped to any book or desk |
| **P&L** | P&L attribution across desks |
| **Reports** | Generate and archive regulatory reports |
| **System** | Service health; first stop when numbers look wrong |

The **hierarchy selector** (Firm > Division > Desk > Book) in the header is your primary scoping control. Set it to FIRM for a consolidated view; drill to DESK or BOOK when investigating a specific issue. Every number on every tab respects the current scope.

The **data quality indicator** in the header is the first thing you check in the morning. If it is amber or red, the VaR numbers you are looking at may be based on stale or incomplete market data. Do not promote an EOD run until this is green.

The **market regime badge** tells you which volatility regime the system has detected. In elevated-vol or crisis regimes, VaR parameters adjust automatically. If the badge shows ELEVATED or CRISIS, treat every limit utilisation number as already under stress.

---

## 1. Risk Dashboard

The **Risk** tab is your command centre. Three sub-tabs: **Dashboard**, **Run Compare**, and **Market Data**.

### Dashboard

**VaR gauge** -- VaR at the current confidence level (95%, 99%, or 99.9%) with Expected Shortfall alongside it. ES is what matters in a tail event; VaR is just the threshold. The gauge shows where you stand against the limit for the selected scope.

**Greeks summary** -- Portfolio Delta, Gamma, Vega broken out by asset class, plus Theta and Rho. As a risk manager you are watching for outsized Vega (exposure to a vol spike) and Rho (exposure to a rates move). If a book is carrying large Vega heading into a central bank meeting, that is a conversation to have.

**VaR component breakdown** -- Chart showing which asset classes are contributing most to VaR. If Equity is 80% of the firm's VaR but the mandate is diversified multi-asset, something needs attention.

**VaR trend chart** -- Historical VaR and ES over your chosen window (1d, 1w, 1m, 3m). This is how you spot a book quietly drifting toward its limit. A VaR that has doubled in two weeks without a corresponding P&L move warrants a call to the desk.

**Risk budget panel** -- VaR versus the allocated budget for the current scope. The panel shows both absolute and percentage utilisation. A book at 95% of its VaR budget with a large position to put on needs either a limit increase or a risk reduction first.

**Liquidity risk panel** -- Liquidity-adjusted VaR (LVaR), data completeness score, and concentration warnings. LVaR accounts for the fact that you cannot unwind a large position at mid in a single day. If LVaR is materially higher than standard VaR, the book has a liquidity problem worth flagging.

**Factor decomposition** -- Systematic vs. idiosyncratic risk split with factor attribution trends. A book where idiosyncratic risk is growing relative to systematic risk is adding single-name exposure that may not show up dramatically in VaR until something blows up.

**Correlation heatmap** -- Instrument-level correlation matrix. In a crisis, correlations go to one. If the heatmap shows assumed diversification between two positions that historically move together in a selloff, that diversification benefit is illusory.

**Hedge recommendations** -- System-suggested trades to reduce Delta, Gamma, Vega, or overall VaR. As a risk manager you review these when a book is approaching a limit. The desk can accept or reject them; you can monitor pending and actioned recommendations.

**Book contribution table** -- When scoped to Desk or above, this shows per-book VaR, standalone VaR, diversification benefit, and marginal VaR. Marginal VaR tells you which book is the most expensive unit of risk relative to its contribution -- the place to look if you need to find capacity quickly.

---

## 2. Limit Management

### Hierarchy

Limits cascade **FIRM > DIVISION > DESK > BOOK / TRADER / COUNTERPARTY**. Every pre-trade check walks the hierarchy bottom-up. A book-level trade that passes the book limit still fails if it pushes the desk over its desk limit.

Limits come in three types:

| Type | What it controls |
|------|-----------------|
| Position | Maximum quantity per instrument |
| Notional | Maximum dollar exposure, FX-normalised to base currency |
| Concentration | Maximum exposure to a single counterparty |

Each limit definition carries a separate **intraday limit** and **overnight limit**. A book may be permitted $50M intraday but only $30M overnight because overnight positions carry gap risk. The system selects the appropriate threshold automatically.

### Breach states

| State | Meaning | Action |
|-------|---------|--------|
| OK | Below 80% of limit | Monitor |
| WARNING | 80-100% of limit | Discuss with desk; consider pre-emptive reduction |
| BREACHED | Exceeds limit | Hard breach blocks new trades; immediate escalation required |

A WARNING does not block trading. A hard BREACHED state blocks the trade at submission and returns full breach detail to the trader -- level breached, effective limit, current exposure.

### Temporary limit increases

When a desk needs to exceed a limit for a defined window, a temporary increase can be granted. The increase is time-bounded and expires automatically -- no manual cleanup. Every grant and expiry is audited. When investigating an apparent breach, first check whether there is a valid temporary increase in place.

---

## 3. Risk Budgets

The risk budget panel on the Dashboard shows VaR against the allocated budget at whatever scope is selected. Budget allocation flows down the same hierarchy as limits.

The number you care about is **utilisation percentage**, not absolute VaR. A desk with $5M VaR against a $6M budget at 83% is closer to trouble than a desk with $20M VaR against a $100M budget at 20%.

When a desk is consistently running at 90%+ utilisation, that is a budget review conversation -- either the mandate has grown and the budget needs to increase, or the book is being managed aggressively and the constraint is intentional.

---

## 4. Run Comparison

**Run Compare** (second sub-tab on Risk) is how you investigate why VaR moved. Three comparison modes:

| Mode | Use case |
|------|----------|
| **Daily VaR** | Day-over-day: why did VaR change between yesterday's EOD and today? |
| **Model comparison** | Same portfolio, two different model versions -- use for model validation sign-off |
| **Backtest comparison** | Compare two backtest result sets side by side |

### Daily VaR -- investigation workflow

Select yesterday's Official EOD as the base and today's latest run as the target. The comparison shows:

- **Output diff** -- VaR delta, ES delta, Greeks changes, component shifts.
- **Input changes summary** -- a one-line diagnostic: "3 position changes, spot prices moved up to Large, model version unchanged." This is the fastest possible answer to "why did VaR move?" Expand for detail.
- **Position changes** -- ADDED, REMOVED, QUANTITY_CHANGED, PRICE_CHANGED with notional deltas, top 10 by absolute impact.
- **Market data changes** -- magnitude indicators per instrument for spot prices, vol surface shifts, yield curve changes, and correlation changes. Expand any row to get the quantitative diff.
- **VaR attribution panel** -- first-order attribution to position effect, vol effect, and correlation effect. The residual "Other / Unexplained" bucket is always shown explicitly. If Unexplained is large and the method is Monte Carlo, simulation sampling variance may be contributing -- the panel flags this.

Magnitude indicators:

| Indicator | Input change size |
|-----------|-----------------|
| LARGE (amber) | > 5% for prices/rates/vols; > 0.10 mean off-diagonal for correlation |
| MEDIUM (blue) | 1-5% |
| SMALL (grey) | Under 1% |

When VaR has jumped and you need to explain it to the CRO in 10 minutes: open Run Compare, select yesterday vs. today, read the Input Changes summary bar. One line tells you whether it was positions, market data, or a model change. Then drill.

### Model comparison

Use this when signing off a new model version. Run the same portfolio through version A (current) and version B (proposed). The comparison highlights the model version change in a prominent banner. If the new model changes firm VaR by more than your materiality threshold, that is a governance event requiring documentation.

---

## 5. Stress Testing Governance

### Scenario lifecycle

Scenarios follow a four-eyes approval workflow:

```
DRAFT -> PENDING_APPROVAL -> APPROVED -> RETIRED
```

The person who creates a scenario cannot be the one who approves it. Only APPROVED scenarios can be run against live books for official purposes. DRAFT scenarios can be run ad-hoc but are clearly labelled.

If an APPROVED scenario is updated (shocks or correlation override modified), it automatically reverts to DRAFT and the version counter increments. The prior version's approval record is preserved in the audit trail.

### Approving a scenario

Pending-approval scenarios appear in the governance section of the Scenarios tab. Before approving:

- Review the shock vector -- price shocks per asset class, vol multiplier, correlation override.
- A scenario without a correlation override underestimates tail risk. In a crisis, correlations converge toward one.
- Check the version history. If this is a re-approval of an amended scenario, what changed?
- Confirm you are not approving your own submission.

Your user ID is recorded as `approvedBy` with a timestamp.

### Running scenarios

For official runs, use only APPROVED scenarios:

- **Run All** -- executes all approved scenarios against the current portfolio and returns a comparison table sorted by worst P&L impact. The view for 7am and risk committee meetings.
- **Position drill-down** -- click any asset class bar to see which positions drive that stress loss.
- **Limit breach highlighting** -- colour-coded green / amber / red flags showing which limits would breach under each scenario.
- **Scenario comparison** -- run up to 3 scenarios side by side. Different crises stress different parts of the book; understanding which is binding for each desk shapes the hedging conversation.

### Historical replay

Select a historical period and replay actual daily returns against the current portfolio. The system shows daily P&L impact and flags proxy data usage for instruments that didn't exist during the historical period.

This is the honest stress test. Parametric scenarios approximate; the replay uses the actual return sequence. Run both. If the parametric GFC scenario shows $10M but the actual 2008 replay shows $25M, the parametric shock is understating the risk.

### Reverse stress testing

Enter a target loss and the system finds the minimum-norm shock vector that produces it. Basel and the PRA require this. If the required shocks are plausible market moves, that is a very different risk position from one requiring historically unprecedented conditions.

Results include a `converged` flag. Non-converged results (target loss outside the feasible range) return a 422 and cannot be saved as authoritative.

---

## 6. Backtesting

Backtesting is under the **Regulatory** tab. It answers: is the VaR model actually predicting losses correctly?

### What is computed

| Test | What it measures |
|------|-----------------|
| **Kupiec POF** | Whether the observed violation rate is statistically consistent with the model's confidence level |
| **Christoffersen independence** | Whether violations cluster in time -- a clustering model fails in exactly the conditions where it matters |
| **Traffic light zone** | Basel classification based on violation count in a 250-day window |

### Traffic light zones (250-day window at 99% CL)

| Zone | Violations | Meaning |
|------|-----------|---------|
| GREEN | 0-4 | Model acceptable |
| YELLOW | 5-9 | Increased scrutiny; document and monitor |
| RED | 10+ | Model must be reviewed; likely replacement required |

Both tests should pass at p > 0.05. A model that passes Kupiec but fails Christoffersen is predicting the right overall rate but failing in market stress -- which is exactly when it matters.

A YELLOW outcome does not automatically mean the model is wrong, but it requires documentation. A RED outcome triggers a mandatory model governance review.

All results are persisted with a SHA-256 digest of the input data. The history gives you a trend: a model drifting from GREEN to YELLOW over six months is telling you something about regime change.

### Backtest comparison

Compare two backtest result sets side by side -- violation count delta, rate delta, p-value shifts, and traffic light zone changes. Use this to demonstrate to the regulator that a model change improved the backtest result.

---

## 7. Model Governance

The model governance register under the **Regulatory** tab provides version control and approval workflow for all risk models.

### Model lifecycle

```
DRAFT -> VALIDATED -> APPROVED -> RETIRED
```

- **DRAFT** -- registered but not validated. Testing only.
- **VALIDATED** -- quant team has run validation and signed off technically.
- **APPROVED** -- you have approved the model for production use.
- **RETIRED** -- superseded or withdrawn.

Each version records: model name, version string, parameters (JSON), who registered it, who approved it, and when.

### Your approval role

The VALIDATED -> APPROVED transition requires your sign-off. Before approving:

1. Run the new version against the current version in **Run Compare** model mode on a representative book.
2. Check the backtest result for the new version -- it should be GREEN zone.
3. Confirm the parameters match what the quant team presented. Parameters are immutable once approved.
4. Confirm you are not approving your own registration.

Your approval is recorded with user ID and timestamp -- the evidence trail for model risk management review or regulatory examination.

---

## 8. Regulatory Submissions

Regulatory submissions follow a formal workflow under the **Regulatory** tab:

```
DRAFT -> PENDING_REVIEW -> APPROVED -> SUBMITTED -> ACKNOWLEDGED
```

The preparer cannot be the approver -- four-eyes is enforced in code.

As the risk manager you are typically the approver. Before approving:

- The FRTB capital charge should be consistent with recent history. A sudden spike warrants investigation.
- Check the SBM/DRC/RRAO breakdown. If DRC suddenly dominates, check whether credit positions were added.
- Confirm the CSV or XBRL export matches the dashboard numbers -- the export is what goes to the regulator.

Once approved, the submission moves to SUBMITTED and eventually ACKNOWLEDGED. The full history is retained and audited.

---

## 9. Counterparty Risk

The **Counterparty Risk** tab provides the firm-level exposure view.

- **Counterparty list** -- current exposure, netting benefit, and collateral per counterparty.
- **PFE profile** -- 95th percentile Potential Future Exposure curve over time. A PFE peak at year 3 on a rates swap book tells you when most exposure matures.
- **Expected Exposure** -- probability-weighted average alongside PFE.
- **Netting set details** -- which trades net against each other.
- **CVA** -- Credit Valuation Adjustment per counterparty. Rising CVA is an early warning signal.
- **SA-CCR** -- standardised capital charge.
- **Tenor profile** -- exposure by maturity bucket.

Watch for concentration: a single counterparty representing more than 20% of net exposure warrants attention regardless of formal limits.

---

## 10. Alerts

### Creating rules

From the **Alerts** tab, create rules against risk metrics:

| Rule type | Input metric |
|-----------|-------------|
| VAR_BREACH | VaR value from each risk calculation |
| PNL_THRESHOLD | Expected Shortfall from each risk calculation |
| RISK_LIMIT | VaR value with a general limit threshold |

Per rule: threshold, operator, severity (CRITICAL / WARNING / INFO), and delivery channels (IN_APP / EMAIL / WEBHOOK).

As a risk manager you own firm-level and desk-level rules. Name them clearly -- "FIRM VaR > $50M CRITICAL" vs "Rates Desk VaR > $15M WARNING" -- to prevent confusion during triage.

### Triage

Alerts show status: TRIGGERED, ACKNOWLEDGED, ESCALATED, or RESOLVED. Acknowledge to confirm awareness and stop escalation. Resolve when the breach is cleared. Export for audit packs.

A CRITICAL alert that goes unacknowledged will escalate. A cascade of unacknowledged CRITICALs means something needs immediate attention.

---

## 11. EOD Promotion

Promoting the official EOD run is a daily governance action requiring the `PROMOTE_EOD_RUN` permission.

### Rules

- Only COMPLETED runs can be promoted.
- The promoter cannot be the same person who triggered the run (four-eyes).
- Promoting automatically supersedes the previous Official EOD for that date.
- Promotion emits a Kafka event consumed by downstream reporting.

### Run labels

| Label | Meaning |
|-------|---------|
| ADHOC | On-demand, outside the formal sequence |
| SOD | Start-of-day snapshot |
| INTRADAY | Scheduled intraday calculation |
| PRE_CLOSE | Final pre-close run |
| OFFICIAL_EOD | The promoted, authoritative result |
| SUPERSEDED_EOD | A previously promoted EOD replaced by a later promotion |

### EOD workflow

1. Wait for the PRE_CLOSE run to complete.
2. Check data quality is green.
3. Compare PRE_CLOSE to yesterday's OFFICIAL_EOD via Run Compare. Satisfy yourself the VaR move is explainable.
4. Confirm you are not the user who triggered the run.
5. Promote. The run becomes OFFICIAL_EOD.

If a bad run is promoted, demote it and promote the correct run. Supersession and demotion are tracked in the audit trail.

---

## 12. Daily Workflow

**Before the market opens (7:00-7:30)**

1. Check the **data quality indicator**. If amber or red, chase operations before the desk starts trading.
2. Open the **Risk** dashboard at FIRM scope. Check overnight VaR, ES, and limit utilisation. Any desk in WARNING is a first call.
3. For desks in WARNING, open **Run Compare** (yesterday's OFFICIAL_EOD vs. latest). Read the Input Changes summary -- 30 seconds to know if it was positions, market data, or a regime shift.
4. Open **Scenarios**, Run All. Review by worst P&L impact. Note the binding worst-case for the morning risk meeting.
5. Check **Alerts** for overnight CRITICALs. Acknowledge or escalate.

**During the day**

- Monitor **Alerts** for intraday breaches. CRITICALs require immediate acknowledgement and a call to the desk.
- Watch the **market regime badge**. A mid-day shift from NORMAL to ELEVATED means VaR numbers are changing for model reasons. Alert the desk.
- Review temporary limit increase requests against current utilisation. Grant or decline; the record is audited.
- Review pending scenario and model version approvals. You cannot approve your own submissions.

**End of day (16:30-17:30)**

1. Wait for PRE_CLOSE to complete.
2. Confirm data quality is green.
3. Run Compare: PRE_CLOSE vs. yesterday's OFFICIAL_EOD.
4. Check FRTB capital charge changes in the **Regulatory** tab.
5. Check **Counterparty Risk** for concentration built during the day.
6. Promote PRE_CLOSE as OFFICIAL_EOD.
7. Resolve or escalate all open alerts.

**Weekly**

- Review backtesting results per book. Is the model drifting toward YELLOW?
- Check the model governance register for VALIDATED models awaiting approval.
- Review pending regulatory submissions.

---

## Permissions Reference

| Action | Required role |
|--------|-------------|
| Promote/demote Official EOD | RISK_MANAGER, ADMIN |
| Approve stress scenarios | Anyone other than the creator |
| Approve model versions | Anyone other than the registrant |
| Approve regulatory submissions | Anyone other than the preparer |
| Grant temporary limit increases | RISK_MANAGER, ADMIN |
| Create/delete alert rules | RISK_MANAGER, TRADER, ADMIN |

Four-eyes is enforced in code, not just policy. The system rejects self-approvals.
