---
name: demo
description: Set up realistic demo data for Kinetix — portfolios, positions, trades, market data, and risk results for demonstrations or local development. Invoke with /demo optionally followed by a scenario (e.g. "equity portfolio", "multi-asset", "stress scenario").
user-invocable: true
allowed-tools: Read, Write, Edit, Glob, Grep, Bash, Task, WebFetch, WebSearch
---

# Demo Data Setup

You are setting up realistic demo data for Kinetix. The goal is to create a convincing, internally consistent dataset that showcases the platform's capabilities.

## Step 1 — Choose the scenario

If the user specified a scenario, use that. Otherwise offer these options:

1. **Equity Long/Short** — 50 positions across US/EU equities, demonstrating portfolio risk, sector concentration, and P&L
2. **Multi-Asset** — equities, options, fixed income across 3 currencies, demonstrating cross-asset risk aggregation and FX effects
3. **Options Book** — 30 equity options positions demonstrating full Greeks (delta, gamma, vega, theta, rho), vol surface, and cross-Greeks
4. **Stress Scenario** — a portfolio near limit breaches, demonstrating alerts, limit management, and stress testing
5. **Regulatory Demo** — positions with full audit trail, model governance records, and regulatory submission workflow

Ask the user to confirm before proceeding.

## Step 2 — Understand the data model

Read the current schema and API to understand how to insert data:

- Check position-service API for trade booking endpoints
- Check price-service API for market data ingestion
- Check gateway routes for available endpoints
- Check database migration files for current schema

```bash
# Find API route definitions
grep -r "route\|routing\|get(\|post(\|put(\|delete(" --include="*.kt" -l | grep -i route
```

## Step 3 — Generate the data

Create data that is:
- **Internally consistent** — prices, positions, and P&L numbers agree with each other
- **Realistic** — use real instrument names, realistic prices, plausible portfolio sizes
- **Time-aware** — include some historical trades to show lifecycle, not just current positions
- **Multi-portfolio** — at least 2 portfolios to demonstrate aggregation

### Market data
- Current prices for all instruments
- Historical prices for at least 5 days (for VaR calculation)
- Yield curve data (risk-free rates)
- Volatility data (for options)

### Trades and positions
- Mix of trade types (BUY, SELL, for options)
- Include at least one amended and one cancelled trade (for audit trail demo)
- Position sizes that produce meaningful risk numbers

### Risk results
- Trigger risk calculations after data is loaded
- Verify VaR, Greeks, and P&L numbers are reasonable

## Step 4 — Load the data

Create a script or series of API calls to load the data. Prefer API calls over direct database inserts to ensure all downstream effects (Kafka events, audit trail, risk recalculation) are triggered.

## Step 5 — Verify

After loading:
- [ ] Positions appear in the UI
- [ ] Prices are fresh
- [ ] Risk calculations complete
- [ ] P&L numbers are reasonable
- [ ] Audit trail has entries
- [ ] At least one alert rule is triggered

## Step 6 — Summary

Report:
- Scenario loaded
- Number of portfolios, positions, trades
- Key numbers to mention during demo (total VaR, largest position, P&L)
- Any setup steps needed (e.g. "start the risk-engine first")

## Reminders

- Use realistic but not real company names if generating fictional data
- Ensure options have valid expiry dates in the future
- Include at least one position near a limit to show limit monitoring
- Create trades over multiple days to show historical trends
- Always trigger risk recalculation after loading positions
