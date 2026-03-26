"""Liquidity risk calculations.

This module provides:
  - compute_liquidation_horizon: classify a position by ADV fraction to determine
    how many days it takes to unwind without significant market impact.
  - compute_lvar: Basel-consistent sqrt(T) scaled Liquidity-adjusted VaR.
  - compute_stressed_liquidation_value: per-scenario, per-asset-class stress.
  - assess_concentration_flag: ADV concentration limit check with fail-safe on
    missing data (BREACHED) and warning on stale data.

Key behavioural invariants:
  - No ADV data -> ILLIQUID tier, 10-day default horizon (ILLIQUID_HORIZON_DAYS),
    adv_missing=True. Fail safe for pre-trade checks.
  - Stale ADV (adv_staleness_days > ADV_MAX_STALENESS_DAYS) -> adv_stale=True.
    Concentration check returns WARNING (not BREACHED) unless also over limit.
  - LVaR = base_var * sqrt(liquidation_days / base_holding_period).
  - data_completeness = fraction of portfolio (by notional) with ADV data present.
"""
from __future__ import annotations

import math
from dataclasses import dataclass, field
from enum import Enum

from kinetix_risk.models import AssetClass

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

ILLIQUID_HORIZON_DAYS = 10
ADV_MAX_STALENESS_DAYS = 2

# Fraction of ADV thresholds for tier classification
_TIER_HIGH_LIQUID_MAX = 0.10   # < 10% of ADV
_TIER_LIQUID_MAX = 0.25        # 10-25% of ADV
_TIER_SEMI_LIQUID_MAX = 0.50   # 25-50% of ADV
# > 50% -> ILLIQUID

_TIER_TO_HORIZON: dict[str, int] = {}  # populated after LiquidityTier is defined


class LiquidityTier(str, Enum):
    HIGH_LIQUID = "HIGH_LIQUID"
    LIQUID = "LIQUID"
    SEMI_LIQUID = "SEMI_LIQUID"
    ILLIQUID = "ILLIQUID"


_TIER_TO_HORIZON = {
    LiquidityTier.HIGH_LIQUID: 1,
    LiquidityTier.LIQUID: 3,
    LiquidityTier.SEMI_LIQUID: 5,
    LiquidityTier.ILLIQUID: ILLIQUID_HORIZON_DAYS,
}

# ---------------------------------------------------------------------------
# Input/Output dataclasses
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class LiquidityInput:
    instrument_id: str
    market_value: float
    adv: float | None
    adv_staleness_days: int | None
    asset_class: AssetClass


@dataclass(frozen=True)
class LiquidationHorizonResult:
    tier: LiquidityTier
    horizon_days: int
    adv_missing: bool = False
    adv_stale: bool = False


@dataclass(frozen=True)
class LVaRResult:
    lvar_value: float
    data_completeness: float


@dataclass(frozen=True)
class ConcentrationCheckResult:
    status: str          # "OK" | "WARNING" | "BREACHED"
    current_pct: float
    limit_pct: float
    adv_missing: bool = False
    adv_stale: bool = False


@dataclass(frozen=True)
class PositionLiquidityRisk:
    instrument_id: str
    asset_class: AssetClass
    market_value: float
    tier: LiquidityTier
    horizon_days: int
    adv: float | None
    adv_missing: bool
    adv_stale: bool
    lvar_contribution: float


@dataclass(frozen=True)
class LiquidityRiskResult:
    book_id: str
    portfolio_lvar: float
    data_completeness: float
    position_risks: list[PositionLiquidityRisk]
    concentration_status: str


# ---------------------------------------------------------------------------
# compute_liquidation_horizon
# ---------------------------------------------------------------------------


def compute_liquidation_horizon(
    market_value: float,
    adv: float | None,
    adv_staleness_days: int | None,
) -> LiquidationHorizonResult:
    """Classify a position into a liquidity tier and return the horizon in days.

    When ADV data is absent, returns ILLIQUID with adv_missing=True (fail-safe).
    When ADV is stale (>2 days), sets adv_stale=True but still classifies using
    the stale value.
    """
    adv_missing = adv is None
    adv_stale = (
        adv_staleness_days is not None
        and adv_staleness_days > ADV_MAX_STALENESS_DAYS
    )

    if adv_missing:
        return LiquidationHorizonResult(
            tier=LiquidityTier.ILLIQUID,
            horizon_days=ILLIQUID_HORIZON_DAYS,
            adv_missing=True,
            adv_stale=False,
        )

    adv_fraction = abs(market_value) / adv if adv > 0 else 1.0

    if adv_fraction < _TIER_HIGH_LIQUID_MAX:
        tier = LiquidityTier.HIGH_LIQUID
    elif adv_fraction < _TIER_LIQUID_MAX:
        tier = LiquidityTier.LIQUID
    elif adv_fraction < _TIER_SEMI_LIQUID_MAX:
        tier = LiquidityTier.SEMI_LIQUID
    else:
        tier = LiquidityTier.ILLIQUID

    return LiquidationHorizonResult(
        tier=tier,
        horizon_days=_TIER_TO_HORIZON[tier],
        adv_missing=False,
        adv_stale=adv_stale,
    )


# ---------------------------------------------------------------------------
# compute_lvar
# ---------------------------------------------------------------------------


def compute_lvar(
    base_var: float,
    liquidation_horizon_days: int,
    base_holding_period: int = 1,
    inputs: list[LiquidityInput] | None = None,
) -> LVaRResult:
    """Compute Liquidity-adjusted VaR using Basel sqrt(T) scaling.

    lvar = base_var * sqrt(liquidation_horizon_days / base_holding_period)

    data_completeness: fraction of portfolio positions that have ADV data.
    When no inputs are provided, data_completeness defaults to 1.0.
    """
    lvar_value = base_var * math.sqrt(liquidation_horizon_days / base_holding_period)

    if not inputs:
        data_completeness = 1.0
    else:
        total_notional = sum(abs(inp.market_value) for inp in inputs)
        covered_notional = sum(abs(inp.market_value) for inp in inputs if inp.adv is not None)
        data_completeness = covered_notional / total_notional if total_notional > 0 else 1.0

    return LVaRResult(lvar_value=lvar_value, data_completeness=data_completeness)


# ---------------------------------------------------------------------------
# compute_stressed_liquidation_value
# ---------------------------------------------------------------------------


def compute_stressed_liquidation_value(
    market_value: float,
    horizon_days: int,
    daily_vol: float,
    stress_factor: float,
) -> float:
    """Compute the stressed liquidation value for a position.

    stressed_value = market_value * (1 - stress_factor * daily_vol * sqrt(horizon_days))

    Applies per-scenario, per-asset-class stress factors. A larger stress_factor
    represents a more adverse liquidity environment (e.g. GFC_2008).
    """
    vol_impact = daily_vol * math.sqrt(horizon_days)
    discount = stress_factor * vol_impact
    return market_value * (1.0 - discount)


# ---------------------------------------------------------------------------
# assess_concentration_flag
# ---------------------------------------------------------------------------


def assess_concentration_flag(
    market_value: float,
    adv: float | None,
    warning_pct: float = 0.05,
    hard_block_pct: float = 0.10,
    adv_staleness_days: int | None = None,
) -> ConcentrationCheckResult:
    """Check whether a position breaches the ADV concentration thresholds.

    Two-tier check with explicit warning and hard-block levels:
      - No ADV data -> BREACHED (blocks trade; fail-safe).
      - current_pct > hard_block_pct -> BREACHED.
      - current_pct > warning_pct -> WARNING.
      - Stale ADV (>2 days) and within warning -> WARNING.
      - Otherwise -> OK.

    Args:
        market_value: absolute market value of the position.
        adv: average daily volume in the same units as market_value (or None).
        warning_pct: fraction of ADV that triggers a WARNING (default 5%).
        hard_block_pct: fraction of ADV that triggers a BREACHED (default 10%).
        adv_staleness_days: age of the ADV data in days.
    """
    adv_missing = adv is None
    adv_stale = (
        adv_staleness_days is not None
        and adv_staleness_days > ADV_MAX_STALENESS_DAYS
    )

    if adv_missing:
        return ConcentrationCheckResult(
            status="BREACHED",
            current_pct=0.0,
            limit_pct=hard_block_pct,
            adv_missing=True,
            adv_stale=False,
        )

    current_pct = abs(market_value) / adv if adv > 0 else 1.0

    if current_pct > hard_block_pct:
        status = "BREACHED"
    elif current_pct > warning_pct:
        status = "WARNING"
    elif adv_stale:
        status = "WARNING"
    else:
        status = "OK"

    return ConcentrationCheckResult(
        status=status,
        current_pct=current_pct,
        limit_pct=hard_block_pct,
        adv_missing=False,
        adv_stale=adv_stale,
    )
