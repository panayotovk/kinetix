# Kinetix Trader Guide

A practical guide to the Kinetix risk management platform from the trading desk's perspective. This covers the daily workflow: booking trades, monitoring positions, managing risk, and everything in between.

---

## At a Glance

Kinetix is organised into **11 tabs** across the top of the screen. Each maps to a stage of the trading and risk workflow:

| Tab | What it does |
|-----|-------------|
| **Positions** | Live portfolio view with real-time prices |
| **Trades** | Trade blotter, execution costs, prime broker reconciliation |
| **P&L** | Intraday and attributed profit & loss |
| **Risk** | VaR, Greeks, factor decomposition, hedge recommendations |
| **EOD History** | End-of-day snapshots and historical comparison |
| **Scenarios** | Stress tests, historical replays, reverse stress |
| **Regulatory** | FRTB capital charges and regulatory reporting |
| **Counterparty Risk** | Exposure by counterparty, PFE, CVA |
| **Reports** | Templated report generation (CSV / XBRL) |
| **Alerts** | Alert rules, breach notifications, escalation |
| **System** | Service health and observability links |

A **hierarchy selector** (Firm > Division > Desk > Book) in the header lets you scope every view to the level you care about. A **data quality indicator** and **market regime badge** sit in the header so you always know whether the numbers you're looking at are fresh and which regime the system is pricing into.

---

## 1. Booking and Managing Trades

### Book a new trade

From the **Positions** tab, submit a trade against a book. You specify the instrument, side (BUY/SELL), quantity, and price. Kinetix runs a **pre-trade limit check** in under 100ms before the trade is persisted. The check validates against:

- **Position limits** -- maximum quantity per instrument.
- **Notional limits** -- maximum dollar exposure, FX-normalised to base currency.
- **Concentration limits** -- maximum exposure to a single counterparty.
- **Hierarchy limits** -- limits cascade FIRM > DESK > TRADER > COUNTERPARTY, with intraday and overnight thresholds.

If a soft limit is breached, you get a warning but the trade goes through. A hard breach blocks the trade outright.

### Amend or cancel

On the **Trades** tab, you can amend a live trade (updating quantity, price, or other fields) or cancel it. The system tracks the full lifecycle -- every state transition is audited with a hash-chained record.

### Instrument coverage

Kinetix supports **11 instrument types**: equities, bonds, futures, FX, swaps, options (vanilla), commodities, ETFs, CDS, structured products, and repos. Each type carries its own attributes (e.g. strike/expiry for options, coupon/maturity for bonds) stored against the instrument master in reference data.

---

## 2. Monitoring Positions

The **Positions** tab is the home screen. It shows a paginated grid (50 rows per page) of your current positions with:

- **Real-time price updates** via WebSocket -- you see mark-to-market move as the market moves.
- **P&L** (unrealised and realised) colour-coded so you can spot winners and losers at a glance.
- **Greeks** per position -- Delta, Gamma, Vega -- for anything that has optionality.
- **VaR contribution** (%) so you know which positions are driving your risk.
- **Instrument type badges** colour-coded by asset class.

Use the **gear icon** to toggle column visibility. Your preferences persist across sessions. Hit **Export CSV** to pull the grid into a spreadsheet.

The **Portfolio Summary Card** at the top gives you the headline numbers: total NAV, unrealised P&L, and a multi-currency breakdown. You can switch base currency (USD, EUR, GBP, JPY) to see everything in whatever terms your mandate requires.

---

## 3. Trade Blotter and Execution

The **Trades** tab has three sub-tabs:

### Blotter

Your full trade history, newest first. Filter by instrument, side, or instrument type. Each row shows the instrument, trade time, quantity, price, and notional. Paginated and exportable.

### Execution Cost

For each order: arrival price vs. average fill, slippage, market impact, and timing cost -- all in basis points. This is where you assess execution quality.

### Reconciliation

Daily comparison of your internal positions against prime broker statements. The system highlights **breaks** (quantity mismatches) in amber so you can chase them down before they compound.

---

## 4. P&L

The **P&L** tab answers the question every trader asks ten times a day: "where am I making and losing money?"

- **Intraday P&L chart** -- a time series of today's cumulative P&L, updated in real time via WebSocket.
- **P&L waterfall** -- decomposes your day's P&L into Delta, Gamma, Vega, Theta, Rho, and Unexplained. If your Unexplained bucket is large, something is off with your Greeks or market data -- investigate.
- **Attribution table** -- position-level breakdown of P&L components.
- **SOD baseline** -- you can snapshot the start-of-day state (or pick a historical job as your baseline) and measure everything against it.
- **Benchmark attribution** -- Brinson-style performance decomposition versus a benchmark. Useful for mandates measured against an index.

---

## 5. Risk

The **Risk** tab is the nerve centre. It has three sub-tabs: **Dashboard**, **Run Compare**, and **Market Data**.

### Dashboard

- **VaR gauge** -- current VaR with a confidence band and limit visualisation. Supports Parametric, Historical, and Monte Carlo methods at 95%, 99%, or 99.9% confidence.
- **Expected Shortfall** -- the average loss beyond VaR. This is what actually matters in a tail event.
- **Greeks summary** -- portfolio-level Delta, Gamma, Vega broken out by asset class, plus scalar Theta and Rho.
- **VaR component breakdown** -- horizontal bar chart showing which asset classes are contributing most to your risk.
- **VaR trend chart** -- historical VaR over time with zoom controls (1d, 1w, 1m, 3m).
- **Position risk table** -- every position's Delta, Gamma, Vega, and VaR contribution. Sort by any column to find the big risk drivers.
- **Book contribution table** -- per-book VaR, diversification benefit, and marginal VaR for multi-book views.
- **Risk budget panel** -- VaR versus your allocated limit. Are you using 30% of budget or 95%?
- **Liquidity risk panel** -- liquidity-adjusted VaR (LVaR), data completeness, and concentration warnings.
- **Factor decomposition** -- systematic vs. idiosyncratic risk split, with factor attribution trends over time.
- **Correlation heatmap** -- instrument-level correlation matrix, so you can see when diversification is real and when it isn't.
- **Hedge recommendations** -- the system suggests trades to reduce Delta, Gamma, Vega, or overall VaR. You can review and execute.
- **Vol surface viewer** -- strike-by-tenor volatility surface for any instrument with options exposure.

### Run Compare

Compare two VaR calculations side by side. Three modes:

- **Daily VaR** -- day-over-day comparison with attribution (position effect, vol effect, correlation effect, time decay).
- **Model comparison** -- same positions, different model versions. Essential for model validation.
- **Backtest comparison** -- Kupiec and Christoffersen test results, traffic-light zones.

### Market Data

- Volatility surfaces and correlation heatmaps for the instruments in your book.

---

## 6. What-If Analysis

The **What-If panel** (accessible from the Risk tab) lets you simulate hypothetical trades before committing:

1. Enter one or more hypothetical trades (instrument, side, quantity, price).
2. The system calculates the impact on VaR, Expected Shortfall, and all Greeks.
3. Compare base vs. hypothetical side by side.
4. If you like what you see, submit the trades directly as a rebalancing order.

This is how you answer "what happens to my risk if I put on this hedge?" without touching the live book.

---

## 7. EOD History

The **EOD History** tab gives you a timeline of end-of-day snapshots:

- **Date range picker** to select the window.
- **Trend chart** of EOD VaR over time.
- **Daily grid** -- one row per day showing VaR, ES, Greeks, and P&L. You can drill into any date to see the full job details and phase timing.
- **Promote to Official EOD** -- designate a specific calculation run as the official end-of-day number.
- **Side-by-side comparison** with any other date.

---

## 8. Stress Testing and Scenarios

The **Scenarios** tab is where you find out what happens when things go wrong.

### Scenario Library

A filterable grid of approved scenarios -- parametric shocks, historical replays, and reverse stress tests. Each has a governance status (Draft > Pending Approval > Approved > Retired). Only approved scenarios can be run against live books.

### Custom Scenarios

Build ad-hoc scenarios by specifying volume or price shocks for specific instruments. Save them for reuse.

### Historical Replay

Select a historical period (e.g. March 2020, the 2022 rates shock) and replay those daily returns against your current portfolio. The system shows position-level daily P&L impact and flags where proxy data was used for instruments that didn't exist during the historical period.

### Reverse Stress

Enter a target loss (e.g. "$50M") and the system works backwards to find the market shocks required to produce that loss. This is a regulatory requirement and a genuinely useful exercise.

### Scenario Comparison

Run up to 3 scenarios and compare their results side by side -- component breakdown, position-level impact, and limit breaches.

### Governance

Scenarios follow a four-eyes approval workflow. The person who creates a scenario cannot be the one who approves it.

---

## 9. Regulatory

The **Regulatory** tab surfaces FRTB (Fundamental Review of Trading Book) capital charges:

- **Total capital requirement** with proportional breakdown: SBM (Sensitivities-Based Method), DRC (Default Risk Charge), and RRAO (Residual Risk Add-On).
- **Risk class detail** -- Delta, Vega, and Curvature charges per risk class.
- **Export** to CSV or XBRL for regulatory submission.

The underlying DRC calculation uses 21 credit rating buckets, seniority-adjusted LGD, maturity weighting, and sector concentration penalties.

---

## 10. Counterparty Risk

The **Counterparty Risk** tab shows exposure aggregated by counterparty:

- **Counterparty list** with current exposure, netting benefits, and collateral.
- **PFE profile** -- 95th percentile Potential Future Exposure and Expected Exposure curves over time.
- **Tenor profile** -- exposure by maturity bucket.
- **Netting set details** -- which trades net against each other.
- **CVA** (Credit Valuation Adjustment) for each counterparty.
- **SA-CCR** standardised capital charge calculation.

---

## 11. Alerts

The **Alerts** tab keeps you informed without having to stare at every screen:

### Alert Rules

Create rules that fire when a metric crosses a threshold. For example:

- VaR exceeds $10M (CRITICAL, notify in-app + email).
- Concentration limit hits 80% (WARNING, in-app only).
- Data quality degrades (INFO, webhook to ops channel).

Each rule has a severity (INFO / WARNING / CRITICAL) and one or more delivery channels (in-app, email, webhook).

### Alert History

All fired alerts with their status: TRIGGERED, ACKNOWLEDGED, ESCALATED, or RESOLVED. Filter by severity. Acknowledge alerts to stop escalation. Export the history for audit.

---

## 12. Reports

The **Reports** tab generates reports from backend templates:

1. Select a template, book, and date.
2. Hit Generate.
3. Download the output as CSV.

Report history is retained so you can access prior outputs.

---

## 13. System Health

The **System** tab shows the operational status of every service in the platform (Gateway, Position Service, Price Service, Risk Orchestrator, etc.) with colour-coded health indicators. Each service links to its Grafana dashboard for deeper investigation. Additional dashboards cover trade flow, Kafka health, database health, and service logs.

If the system is degraded, a red dot appears on the System tab and connection banners appear at the top of every screen.

---

## Daily Workflow

A typical day on the desk looks like this:

1. **Morning** -- Open the **Positions** tab to review overnight positions. Check the **P&L** tab for SOD baseline. Glance at the **Risk** dashboard for overnight VaR and any limit breaches. Check the **Data Quality** indicator in the header -- if it's amber or red, your numbers aren't trustworthy yet.

2. **Pre-trade** -- Before putting on a new position, use the **What-If panel** to see the risk impact. The pre-trade limit check runs automatically on submission.

3. **During the day** -- Monitor **P&L** in real time. Watch for alerts on the **Alerts** tab. If VaR is creeping up, use the **Hedge Recommendations** panel on the Risk tab to identify offsetting trades. Check the **Market Regime** badge -- if the system detects elevated vol or crisis conditions, VaR parameters adjust automatically.

4. **Stress check** -- Run approved stress scenarios periodically, especially when markets are moving. Use **Historical Replay** to stress-test against analogous past events.

5. **End of day** -- Review the **EOD History** tab. Promote the final calculation as Official EOD. Check **Counterparty Risk** for any exposure concentration. Export regulatory reports from the **Regulatory** tab. Verify the **Reconciliation** sub-tab against prime broker statements.

6. **Ongoing** -- Review fired alerts, acknowledge and resolve them. Use **Run Compare** to understand day-over-day VaR changes and attribute them to position moves, vol changes, or correlation shifts.

---

## Keyboard and Customisation

- **Dark mode** -- toggle via the sun/moon icon in the header.
- **Column visibility** -- gear icon on grids to show/hide columns.
- **Workspace persistence** -- save your preferred default tab, book, time range, and chart settings.
- **Keyboard navigation** -- arrow keys to move between tabs, Escape to close dropdowns.
- **CSV export** -- available on positions, trades, P&L, risk, alerts, and stress results.

All preferences are saved to your browser and persist across sessions.
