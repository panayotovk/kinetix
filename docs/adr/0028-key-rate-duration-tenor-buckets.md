# ADR-0028: Key Rate Duration — 4-Tenor Internal vs 12-Tenor FRTB GIRR

## Status
Accepted

## Context

Key Rate Duration (KRD) decomposes a bond portfolio's interest rate sensitivity into tenor-specific DV01 buckets. This is essential for rates trading desks managing curve trades (e.g., 2s10s steepeners) where flat DV01 provides no useful information about where on the curve the risk sits.

Two tenor grid standards exist:
- **4 tenors (2Y, 5Y, 10Y, 30Y):** Standard for internal risk management. Sufficient for trading desk risk monitoring, hedge construction, and P&L attribution for typical government and investment-grade corporate bond portfolios.
- **12 tenors (0.25Y, 0.5Y, 1Y, 2Y, 3Y, 5Y, 10Y, 15Y, 20Y, 25Y, 30Y, maturity):** Required by FRTB General Interest Rate Risk (GIRR) per BCBS 352 Table 4 for regulatory capital calculation. Each vertex has a prescribed risk weight.

The current implementation in `key_rate_duration.py` uses analytical tent-function bumping on the yield curve with DCF bond repricing — no Monte Carlo. This is computationally trivial (sub-millisecond per instrument).

## Decision

**Phase 1: Deploy with 4 analytical tenors (2Y, 5Y, 10Y, 30Y) for internal risk management.**

Rationale:
- Covers the standard tenor points used by rates trading desks for hedge sizing and curve trade P&L attribution
- Matches the 4-tenor grid used by the existing `rates-service` yield curve seeder
- Sufficient for internal risk limits and trader-facing dashboards
- The analytical computation path (tent-function DV01, no Monte Carlo) keeps latency under 1ms per instrument, safe for on-demand calculation

**Phase 2 (future): Extend to 12-tenor FRTB GIRR alignment when regulatory capital scope is confirmed.**

This requires:
- Extending `DEFAULT_TENORS` in `key_rate_duration.py` to 12 vertices
- Adding tenor-specific risk weights per BCBS 352 Table 4 to `sbm.py`
- Updating `daily_krd_snapshots` schema to accommodate 12 columns (or use a normalised tenor/value schema)
- Verifying that the yield curve data from `rates-service` contains all 12 tenor nodes

## Consequences

- Traders see DV01 bucketed at 2Y, 5Y, 10Y, and 30Y — sufficient for identifying curve exposure concentration
- Cash flows falling between tenor nodes are distributed proportionally via tent functions (Reitano/BARRA standard)
- The 10Y-30Y gap means sensitivity for 15Y and 20Y bonds is allocated across 10Y and 30Y buckets, which is imprecise but acceptable for internal use
- FRTB GIRR submissions cannot use 4-tenor KRD — the 12-tenor extension is a prerequisite for regulatory capital calculation
- The analytical computation approach scales to 12 tenors with zero performance impact
