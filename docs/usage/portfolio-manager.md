# Kinetix Portfolio Manager / Desk Head Guide

A practical guide to Kinetix from the seat of someone overseeing multiple books, allocating risk capital, and answering to the CRO. This is not about booking individual trades -- that is covered in the [Trader Guide](trader.md). This is about getting the firm-wide picture quickly, understanding where capital is being deployed and why, and making strategic decisions backed by numbers.

---

## At a Glance

The same 11 tabs the traders use are available to you, but the way you use them is different. You are working top-down: start at the firm or desk level, identify where risk is concentrated, drill into the books driving it, and decide whether to act.

| Tab | PM / Desk Head usage |
|-----|---------------------|
| **Positions** | Aggregate cross-book view; multi-currency NAV; exposure breakdown |
| **Trades** | Review desk-level trade flow; reconciliation status |
| **P&L** | Waterfall by Greek; Brinson benchmark attribution; high-water mark tracking |
| **Risk** | Cross-book VaR; diversification benefit; factor decomposition; hierarchy contribution tables |
| **EOD History** | Day-over-day risk trend; promote official EOD |
| **Scenarios** | Firm-wide stress testing; scenario governance sign-off |
| **Regulatory** | FRTB capital charge aggregation; submission workflow |
| **Counterparty Risk** | Firm-wide concentration; PFE and CVA across all books |
| **Reports** | Scheduled and ad-hoc report generation; audit trail |
| **Alerts** | Desk-level and firm-wide limit breach monitoring |
| **System** | Platform health; data quality indicator |

The **Hierarchy Selector** in the header is the most important control for you. Everything in Kinetix scopes to whatever level you select.

---

## 1. The Hierarchy Selector

The hierarchy selector in the header lets you scope every view:

```
Firm (All) > Division > Desk > Book
```

Click it to open the navigation panel. Start at **Firm (All)** to see the consolidated picture. Drill down to a Division, then a Desk, then a specific Book. A breadcrumb updates as you navigate so you always know where you are.

When you are at the Firm, Division, or Desk level, the Risk tab shows aggregated numbers and contribution tables rather than individual position Greeks. This is the view designed for you.

---

## 2. Multi-Portfolio Aggregation

### Cross-book VaR

The most important multi-portfolio number is **Cross-Book VaR**, found on the Risk tab when scoped at Desk level or above. The system calculates:

| Column | What it means |
|--------|--------------|
| **Standalone VaR** | Each book's VaR if it were the only book |
| **VaR Contribution** | Each book's actual contribution to desk VaR, accounting for correlation |
| **% of Total** | Share of desk-level risk |
| **Diversification Benefit** | Standalone VaR minus actual contribution -- the value of cross-book correlation |
| **Marginal VaR** | Sensitivity of desk VaR to a marginal increase in that book's positions |
| **Incremental VaR** | Change in desk VaR if that book were removed entirely |

The **diversification benefit** shown in green at the bottom of the table is the number to track. If two books that should diversify each other suddenly show a shrinking benefit, correlation has increased -- that is a regime signal worth investigating. In March 2020, cross-asset correlations converged to nearly 1.0 within days; diversification benefits that looked real in February evaporated. Watch this number.

Cross-book VaR polls automatically every 30 seconds. Hit **Refresh** to force a recalculation.

---

## 3. Hierarchy Views and Risk Budget Utilisation

### Hierarchy contribution table

When scoped at Firm, Division, or Desk level, the Risk tab shows a **Hierarchy Contribution Table** listing the top contributors at the next level down:

- At Firm level: Division contributions
- At Division level: Desk contributions
- At Desk level: Book contributions

Each row shows entity name, VaR contribution in dollars, and percentage of total. Clicking a row drills directly into that entity.

Below the table: **Marginal VaR** and **Incremental VaR** for each node. If a book has a high marginal VaR, it is the last place you want to add positions.

### Risk budget utilisation

The **Risk Budget Panel** shows current VaR as a percentage of allocated limit, colour-coded:

- **Green** below 80% -- normal utilisation
- **Amber** at 80-99% -- approaching limit
- **Red** at or above 100% -- limit breached; escalation required

Use this at Desk level to see how each book is using its VaR budget. Capital sitting idle (a book at 20% utilisation) is as much a problem as one running hot at 95%.

---

## 4. P&L Attribution and Benchmark Performance

### Greek P&L waterfall

The **P&L** tab waterfall decomposes today's P&L into:

| Component | What drives it |
|-----------|---------------|
| **Delta** | Directional moves |
| **Gamma** | Convexity -- how Delta changed as the market moved |
| **Vega** | Volatility moves |
| **Theta** | Time decay |
| **Rho** | Rate sensitivity |
| **Unexplained** | Anything not accounted for by the model |

The Unexplained bucket is a red flag. A small residual is normal. A large unexplained P&L (more than 5-10% of total) means stale market data, wrong Greeks, or something the model is not capturing. Investigate before signing off on EOD.

The attribution table below the waterfall lets you expand each factor to see which positions drove it.

### Brinson benchmark attribution

For mandates benchmarked against an index, the P&L tab includes **Brinson-Hood-Beebower attribution**:

| Effect | Meaning |
|--------|---------|
| **Allocation Effect** | Did you overweight the right sectors? |
| **Selection Effect** | Within each sector, did you pick better instruments than the benchmark? |
| **Interaction Effect** | Combined impact of allocation and selection decisions |
| **Total Active Return** | Sum of the above -- your alpha versus the benchmark |

Broken down by sector with a totals row. Green values are positive contributions; red are performance drags.

---

## 5. Factor Risk Decomposition

The **Factor Decomposition Panel** on the Risk tab answers what VaR alone cannot: *where does your risk come from?*

### Systematic versus idiosyncratic

| Metric | Description |
|--------|-------------|
| **Total VaR** | Portfolio VaR as calculated |
| **Systematic VaR** | Portion explainable by market factors |
| **Idiosyncratic VaR** | Residual specific to individual positions |
| **R-squared** | How well the factor model explains portfolio variance |

A high R-squared (above 0.85) means standard macro hedges work. A low R-squared means a lot of idiosyncratic risk that factor hedges won't reduce -- you need position-level cuts.

### Factor contributions

The system decomposes risk across factors including equity beta, rates duration, credit spread, FX delta, and volatility exposure. Each shows dollar contribution, percentage of total, and factor loading (sensitivity).

If the system fires a **Concentration Warning**, one factor is dominating the risk profile. Fine when intentional -- a rates desk should be concentrated in rates duration. A problem when it's in a factor you thought you were neutral to.

### Factor trend chart

The **Factor VaR Attribution History** chart shows factor contributions over the last 30 days. This is how you spot factor drift -- a book that was balanced between equity beta and credit spread six weeks ago but has gradually accumulated a lopsided equity exposure.

---

## 6. Counterparty Exposure

The **Counterparty Risk** tab shows firm-wide exposure. As desk head, you're looking for concentration and wrong-way risk.

### Exposure summary

| Column | Description |
|--------|-------------|
| **Net Exposure** | Current mark-to-market after netting |
| **Peak PFE** | 95th percentile Potential Future Exposure |
| **CVA** | Credit Valuation Adjustment -- expected loss given counterparty credit quality |
| **WWR** | Wrong-Way Risk flag -- amber when net exposure exceeds threshold |

Click any row to see the detail panel with PFE profile (exposure curve over time showing 95th percentile and Expected Exposure at each tenor), netting set details, and the ability to trigger fresh PFE or CVA calculations.

---

## 7. Stress Testing at Scale

At the desk level, stress testing is about firm-wide P&L impact and limit breach identification.

### Running firm-wide scenarios

From the **Scenarios** tab, select an approved scenario and run it. The system calculates:

- **Total P&L impact** across all books in scope
- **Asset class breakdown** -- which asset classes take the most damage
- **Position-level impact** sorted by dollar P&L
- **Limit breaches** -- which limits would be violated, at which hierarchy level, and with what severity

The limit breach table is the most operationally relevant output. Before a scenario like a 200bp parallel rate shift or a 2008-style credit event, you want to know exactly which books would breach which limits and by how much.

### Scenario comparison

Run up to three scenarios and compare side by side. Useful for CRO conversations: "under GFC conditions we lose $X, under 2022 rates shock we lose $Y, and the binding constraint in both cases is the desk-level VaR limit."

### Governance

Only **Approved** scenarios can be run against live books. The four-eyes principle is enforced -- you cannot approve your own scenario.

---

## 8. Hedge Recommendations

The **Hedge Recommendation Panel** on the Risk tab suggests trades to reduce a specified risk metric. Configure:

- **Target Metric**: Delta, Gamma, Vega, or VaR
- **Reduction %**: How much to neutralise (default 80%)
- **Max Results**: Number of suggestions (up to 20)

Each suggestion shows the instrument, side, quantity, estimated execution cost, percentage reduction achieved, and a Greek impact table. The liquidity tier of each instrument is shown -- a suggestion using an illiquid instrument is technically correct but practically dangerous.

Use **Send to What-If** to push any suggestion into the What-If panel for detailed impact analysis before committing.

---

## 9. Regulatory and Capital

### FRTB capital charges

The **Regulatory** tab shows FRTB capital under the Sensitivities-Based Method:

| Component | Description |
|-----------|-------------|
| **SBM Charge** | Sensitivities-Based Method -- Delta, Vega, Curvature by risk class |
| **DRC** | Default Risk Charge -- 21 credit rating buckets, seniority LGD, maturity weighting, sector concentration |
| **RRAO** | Residual Risk Add-On -- exotic and hard-to-model instruments |
| **Total Capital** | SBM + DRC + RRAO |

A high DRC relative to SBM suggests concentrated single-name credit exposure. A high RRAO suggests exotic positions penalised heavily under FRTB. Export as CSV or XBRL.

### Submission workflow

Submissions follow a four-eyes workflow. A preparer creates; a separate approver signs off. As desk head you are typically the approver. The submission record stores who prepared and approved it, and is immutable.

---

## 10. Limits and Alerts

### Limit hierarchy

Limits cascade: **FIRM > DIVISION > DESK > BOOK / TRADER / COUNTERPARTY**. Pre-trade checks validate bottom-up -- a trade can pass a book limit and still be blocked at desk or firm level.

Each limit has a base threshold, intraday and overnight variants, and a warning threshold at 80%. Temporary limit increases expire automatically.

### Alert rules

Create alert rules scoped to the desk level:

- VaR utilisation approaching 90% across any book (WARNING)
- Counterparty exposure exceeding firm limit (CRITICAL, email + in-app)
- Data quality degrading below threshold (INFO, webhook to ops)

Acknowledge alerts to stop escalation. Unacknowledged CRITICAL alerts escalate automatically.

---

## 11. Data Quality and Market Regime

Two header indicators affect every number on the screen.

### Data quality indicator

Traffic light colours:

- **Green**: All data fresh, completeness above threshold
- **Amber**: Some instruments stale or ADV data missing -- risk numbers may be understated
- **Red**: Significant data degradation -- do not use risk numbers for decisions without investigation

### Market regime badge

Shows the system's current regime: NORMAL, ELEVATED_VOL, CRISIS, or RECOVERY. The risk engine adjusts VaR parameters automatically -- more conservative in elevated and crisis conditions.

When the badge shows CRISIS, VaR numbers will be higher even for an unchanged portfolio. This is intentional. Do not interpret it as the portfolio suddenly becoming riskier -- the model is being appropriately conservative.

---

## Daily Workflow

1. **Pre-open** -- Set hierarchy to your Desk. Check Data Quality indicator. Review overnight VaR versus limits. Check Alert History for overnight fires.

2. **Open** -- Check the Hierarchy Contribution Table for overnight P&L drivers and risk budget changes. Look at Diversification Benefit in cross-book VaR -- if it shrank materially, correlations are moving.

3. **Morning meeting** -- Pull the P&L Waterfall and Factor Decomposition for the desk roll-up. Have Brinson Attribution ready if managing against a benchmark.

4. **Intraday** -- Monitor Risk Budget Panel across books. If a book approaches 80% utilisation, alert the trader before it hits 100%. Use Hedge Recommendations if exposure needs reducing. Run firm-wide stress scenarios if markets are moving significantly.

5. **Pre-close** -- Check Counterparty Risk for concentration built during the day. Review Regulatory tab for FRTB capital impact.

6. **EOD** -- Promote the final VaR run as Official EOD on the EOD History tab. Verify Reconciliation for breaks. Export FRTB report. Resolve or escalate all CRITICAL alerts.

7. **Weekly** -- Review Factor VaR Attribution History for trend drift. **Monthly** -- Review VaR Backtest results (Kupiec/Christoffersen) on Run Compare.
