"""Factor-based risk decomposition.

Five risk factors:
  EQUITY_BETA    — proxy: IDX-SPX (S&P 500)
  RATES_DURATION — proxy: US10Y (10-year UST yield change)
  CREDIT_SPREAD  — proxy: CDX-IG (IG credit index)
  FX_DELTA       — proxy: EURUSD (or DXY)
  VOL_EXPOSURE   — proxy: VIX

Loadings:
  - OLS regression for equities (252-day window) when >= MIN_HISTORY_FOR_OLS days.
  - Analytical for bonds (DV01 from bond_pricing.py), options (vega), FX (1.0),
    new instruments (< MIN_HISTORY_FOR_OLS days), or non-matching factor/instrument
    combinations (loading = 0.0).

Factor VaR decomposition:
  1. exposure_f = sum_i(position_mv[i] * loading[i, f])
  2. systematic_variance = exposure^T @ factor_cov @ exposure
  3. idiosyncratic_variance = total_variance - systematic_variance (may be negative when systematic > total)
  4. Factor VaR per factor via Euler allocation
  5. r_squared = systematic_variance / total_variance

Factor P&L attribution:
  factor_pnl[f] = sum_i(position_mv[i] * loading[i, f] * factor_return[f])
  idiosyncratic_pnl = total_pnl - sum(factor_pnls)
"""

from __future__ import annotations

import math
from dataclasses import dataclass, field
from enum import Enum

import numpy as np

from kinetix_risk.bond_pricing import bond_dv01
from kinetix_risk.models import (
    AssetClass,
    BondPosition,
    FxPosition,
    OptionPosition,
    OptionType,
    PositionRisk,
    SwapPosition,
)

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

OLS_WINDOW = 252
MIN_HISTORY_FOR_OLS = 63
CONCENTRATION_WARNING_PCT = 0.60
EWMA_LAMBDA = 0.94

# ---------------------------------------------------------------------------
# Factor types and loading methods
# ---------------------------------------------------------------------------


class FactorType(str, Enum):
    EQUITY_BETA = "EQUITY_BETA"
    RATES_DURATION = "RATES_DURATION"
    CREDIT_SPREAD = "CREDIT_SPREAD"
    FX_DELTA = "FX_DELTA"
    VOL_EXPOSURE = "VOL_EXPOSURE"


# Convenience aliases matching the allium spec symbols
EQUITY_BETA = FactorType.EQUITY_BETA
RATES_DURATION = FactorType.RATES_DURATION
CREDIT_SPREAD = FactorType.CREDIT_SPREAD
FX_DELTA = FactorType.FX_DELTA
VOL_EXPOSURE = FactorType.VOL_EXPOSURE

ALL_FACTORS = list(FactorType)


class LoadingMethod(str, Enum):
    OLS_REGRESSION = "OLS_REGRESSION"
    ANALYTICAL = "ANALYTICAL"
    MANUAL = "MANUAL"


# ---------------------------------------------------------------------------
# Value types
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class InstrumentLoading:
    instrument_id: str
    factor: FactorType
    loading: float
    r_squared: float | None  # None for analytical loadings
    method: LoadingMethod


@dataclass(frozen=True)
class FactorContribution:
    factor: FactorType
    factor_exposure: float        # dollar exposure to the factor
    factor_var: float             # contribution to systematic VaR (Euler)
    pnl_attribution: float        # P&L attributed to this factor
    pct_of_total_var: float       # factor_var / total_var


@dataclass
class FactorDecompositionResult:
    book_id: str
    decomposition_date: str
    total_var: float
    systematic_var: float
    idiosyncratic_var: float
    r_squared: float
    factor_contributions: list[FactorContribution]
    job_id: str | None = None

    @property
    def systematic_pct(self) -> float:
        if self.total_var == 0:
            return 0.0
        return self.systematic_var / self.total_var

    @property
    def idiosyncratic_pct(self) -> float:
        if self.total_var == 0:
            return 0.0
        return self.idiosyncratic_var / self.total_var


@dataclass
class FactorPnlAttributionResult:
    book_id: str
    attribution_date: str
    total_pnl: float
    factor_pnl: list[FactorContribution]
    idiosyncratic_pnl: float


# ---------------------------------------------------------------------------
# OLS loading estimation
# ---------------------------------------------------------------------------


def estimate_ols_loading(
    instrument_returns: np.ndarray,
    factor_returns: np.ndarray,
) -> InstrumentLoading:
    """Estimate factor loading via OLS regression.

    Returns loading (beta) and r_squared.
    """
    if len(instrument_returns) != len(factor_returns):
        raise ValueError(
            f"instrument_returns and factor_returns must be equal length, "
            f"got {len(instrument_returns)} and {len(factor_returns)}"
        )
    if len(instrument_returns) < 2:
        raise ValueError("OLS requires at least 2 observations")

    x = factor_returns
    y = instrument_returns

    # Least-squares: beta = cov(x,y) / var(x)
    x_mean = x.mean()
    y_mean = y.mean()
    cov_xy = np.sum((x - x_mean) * (y - y_mean))
    var_x = np.sum((x - x_mean) ** 2)

    if var_x == 0:
        beta = 0.0
        r_squared = 0.0
    else:
        beta = float(cov_xy / var_x)
        ss_res = np.sum((y - (beta * x + (y_mean - beta * x_mean))) ** 2)
        ss_tot = np.sum((y - y_mean) ** 2)
        r_squared = float(1.0 - ss_res / ss_tot) if ss_tot > 0 else 0.0
        r_squared = max(0.0, min(1.0, r_squared))

    return InstrumentLoading(
        instrument_id="",  # caller sets this
        factor=EQUITY_BETA,  # caller overrides
        loading=beta,
        r_squared=r_squared,
        method=LoadingMethod.OLS_REGRESSION,
    )


# ---------------------------------------------------------------------------
# Analytical loading estimation
# ---------------------------------------------------------------------------


def estimate_analytical_loading(
    position: PositionRisk,
    factor: FactorType,
) -> InstrumentLoading:
    """Compute loading analytically based on instrument type and factor.

    Returns loading = 0.0 when the factor is not applicable to the position.
    """
    loading = _compute_analytical_loading(position, factor)
    return InstrumentLoading(
        instrument_id=position.instrument_id,
        factor=factor,
        loading=loading,
        r_squared=None,
        method=LoadingMethod.ANALYTICAL,
    )


def _compute_analytical_loading(position: PositionRisk, factor: FactorType) -> float:
    ac = position.asset_class

    if factor == EQUITY_BETA:
        # Equities assume market beta = 1.0; other asset classes = 0
        return 1.0 if ac == AssetClass.EQUITY else 0.0

    if factor == RATES_DURATION:
        # Bonds and swaps: DV01 loading; others = 0
        if isinstance(position, BondPosition):
            yield_rate = 0.05  # approximate yield for DV01 at par
            return bond_dv01(position, yield_rate)
        if isinstance(position, SwapPosition):
            # DV01 of swap approximated as notional * maturity_years / 10_000
            notional = position.notional or abs(position.market_value)
            try:
                from datetime import date
                mat = date.fromisoformat(position.maturity_date)
                maturity_years = max(0.0, (mat - date.today()).days / 365.25)
            except (ValueError, AttributeError):
                maturity_years = 1.0
            # Simple DV01 approximation for swaps
            return notional * maturity_years / 10_000.0
        return 0.0

    if factor == CREDIT_SPREAD:
        # Bonds: spread_duration approximated as modified_duration for credit instruments
        if isinstance(position, BondPosition):
            yield_rate = 0.05
            from kinetix_risk.bond_pricing import bond_modified_duration
            return bond_modified_duration(position, yield_rate)
        return 0.0

    if factor == FX_DELTA:
        # FX positions: loading = 1.0 (direct currency exposure)
        if ac == AssetClass.FX or isinstance(position, FxPosition):
            return 1.0
        return 0.0

    if factor == VOL_EXPOSURE:
        # Options: vega from Black-Scholes (per unit of market value)
        if isinstance(position, OptionPosition):
            return _option_vega(position)
        return 0.0

    return 0.0


def _option_vega(option: OptionPosition) -> float:
    """Black-Scholes vega: S * exp(-qT) * N'(d1) * sqrt(T)."""
    s = option.spot_price
    k = option.strike
    r = option.risk_free_rate
    sigma = option.implied_vol
    t = option.expiry_days / 365.0
    q = option.dividend_yield

    if t <= 0 or sigma <= 0 or s <= 0:
        return 0.0

    import math
    d1 = (math.log(s / k) + (r - q + 0.5 * sigma ** 2) * t) / (sigma * math.sqrt(t))
    # Standard normal PDF
    n_prime_d1 = math.exp(-0.5 * d1 ** 2) / math.sqrt(2 * math.pi)
    vega = s * math.exp(-q * t) * n_prime_d1 * math.sqrt(t)
    return vega * option.quantity * option.contract_multiplier


# ---------------------------------------------------------------------------
# Combined loading estimator: OLS or analytical based on history length
# ---------------------------------------------------------------------------


def estimate_loading(
    position: PositionRisk,
    factor: FactorType,
    instrument_returns: np.ndarray,
    factor_returns: np.ndarray,
) -> InstrumentLoading:
    """Estimate loading, preferring OLS when sufficient history available.

    Falls back to analytical when:
      - len(instrument_returns) < MIN_HISTORY_FOR_OLS
      - arrays are empty
    """
    has_sufficient_history = (
        len(instrument_returns) >= MIN_HISTORY_FOR_OLS
        and len(factor_returns) >= MIN_HISTORY_FOR_OLS
    )

    if has_sufficient_history:
        try:
            loading = estimate_ols_loading(instrument_returns, factor_returns)
            return InstrumentLoading(
                instrument_id=position.instrument_id,
                factor=factor,
                loading=loading.loading,
                r_squared=loading.r_squared,
                method=LoadingMethod.OLS_REGRESSION,
            )
        except ValueError:
            pass  # fall through to analytical

    analytical = estimate_analytical_loading(position, factor)
    return InstrumentLoading(
        instrument_id=position.instrument_id,
        factor=factor,
        loading=analytical.loading,
        r_squared=None,
        method=LoadingMethod.ANALYTICAL,
    )


# ---------------------------------------------------------------------------
# Factor covariance matrix
# ---------------------------------------------------------------------------


def _estimate_factor_covariance(
    factor_returns_by_factor: dict[FactorType, np.ndarray],
    factors: list[FactorType],
) -> np.ndarray:
    """Estimate factor covariance matrix using Ledoit-Wolf shrinkage.

    If only one factor has returns, returns a 1x1 variance matrix.
    """
    n_factors = len(factors)
    if n_factors == 0:
        return np.zeros((0, 0))

    # Build returns matrix — align lengths
    lengths = [len(factor_returns_by_factor[f]) for f in factors if f in factor_returns_by_factor]
    if not lengths:
        return np.eye(n_factors) * (0.01 ** 2)  # default 1% daily vol

    min_len = min(lengths)
    if min_len < 2:
        return np.eye(n_factors) * (0.01 ** 2)

    returns_matrix = np.column_stack([
        factor_returns_by_factor.get(f, np.zeros(min_len))[:min_len]
        for f in factors
    ])

    if n_factors == 1:
        return np.array([[float(np.var(returns_matrix[:, 0], ddof=1))]])

    try:
        from sklearn.covariance import LedoitWolf
        lw = LedoitWolf()
        lw.fit(returns_matrix)
        return lw.covariance_
    except Exception:
        return np.cov(returns_matrix.T)


# ---------------------------------------------------------------------------
# Factor VaR decomposition
# ---------------------------------------------------------------------------


def decompose_factor_risk(
    book_id: str,
    positions: list[PositionRisk],
    loadings: dict[str, list[InstrumentLoading]],
    factor_returns_by_factor: dict[FactorType, np.ndarray],
    total_var: float,
    decomposition_date: str = "",
    job_id: str | None = None,
) -> FactorDecompositionResult:
    """Decompose portfolio VaR into systematic (factor) and idiosyncratic components.

    Algorithm:
      1. Build factor exposure vector: exposure_f = sum_i(mv_i * loading_i_f)
      2. Estimate factor covariance matrix (Ledoit-Wolf).
      3. systematic_variance = exposure^T @ Sigma @ exposure
      4. total_variance = total_var^2 (assumes VaR ~ 1.65 * sigma; we use variance proxy)
      5. idiosyncratic_variance = total_variance - systematic_variance (may be negative: factor explains more than total)
      6. Euler allocation per factor: contribution_f = (Sigma @ exposure)_f * exposure_f / systematic_var
      7. r_squared = systematic_variance / total_variance
    """
    import datetime
    if not decomposition_date:
        decomposition_date = datetime.date.today().isoformat()

    # Collect factors with non-zero exposures
    factors_with_loadings: set[FactorType] = set()
    for instrument_loadings in loadings.values():
        for il in instrument_loadings:
            if il.loading != 0.0:
                factors_with_loadings.add(il.factor)

    # Also include factors for which we have return data
    for f in factor_returns_by_factor:
        factors_with_loadings.add(f)

    active_factors = sorted(factors_with_loadings, key=lambda f: f.value)

    if not active_factors or not positions:
        return FactorDecompositionResult(
            book_id=book_id,
            decomposition_date=decomposition_date,
            total_var=total_var,
            systematic_var=0.0,
            idiosyncratic_var=total_var,
            r_squared=0.0,
            factor_contributions=[],
            job_id=job_id,
        )

    # Build exposure vector
    n_factors = len(active_factors)
    exposure = np.zeros(n_factors)
    for pos in positions:
        pos_loadings = loadings.get(pos.instrument_id, [])
        mv = pos.market_value
        for il in pos_loadings:
            if il.factor in active_factors:
                idx = active_factors.index(il.factor)
                exposure[idx] += mv * il.loading

    # Estimate factor covariance
    factor_cov = _estimate_factor_covariance(factor_returns_by_factor, active_factors)

    # Systematic variance = exposure^T @ Sigma @ exposure
    systematic_variance = float(exposure @ factor_cov @ exposure)
    systematic_variance = max(0.0, systematic_variance)

    # Total variance proxy: we use total_var^2 as a scale
    # (VaR ≈ z * sigma, so sigma ≈ total_var / z; variance ~ total_var^2 / z^2)
    # Since we only need ratios, we work in VaR-squared space.
    total_var_sq = total_var ** 2

    r_squared = min(1.0, systematic_variance / total_var_sq) if total_var_sq > 0 else 0.0

    systematic_var_dollar = total_var * math.sqrt(max(0.0, r_squared))
    idiosyncratic_var_dollar = total_var - systematic_var_dollar

    # Euler allocation: contribution_f = (Sigma @ exposure)_f * exposure_f
    sigma_times_exposure = factor_cov @ exposure
    factor_var_raw = sigma_times_exposure * exposure  # element-wise
    total_raw = float(np.sum(factor_var_raw))

    factor_contributions: list[FactorContribution] = []
    for idx, factor in enumerate(active_factors):
        if total_raw > 0:
            factor_var = systematic_var_dollar * (factor_var_raw[idx] / total_raw)
        else:
            factor_var = 0.0

        pct_of_total_var = (factor_var / total_var) if total_var > 0 else 0.0

        factor_contributions.append(FactorContribution(
            factor=factor,
            factor_exposure=float(exposure[idx]),
            factor_var=float(factor_var),
            pnl_attribution=0.0,  # populated separately by compute_factor_pnl_attribution
            pct_of_total_var=float(pct_of_total_var),
        ))

    return FactorDecompositionResult(
        book_id=book_id,
        decomposition_date=decomposition_date,
        total_var=total_var,
        systematic_var=systematic_var_dollar,
        idiosyncratic_var=idiosyncratic_var_dollar,
        r_squared=r_squared,
        factor_contributions=factor_contributions,
        job_id=job_id,
    )


# ---------------------------------------------------------------------------
# Factor P&L attribution
# ---------------------------------------------------------------------------


def compute_factor_pnl_attribution(
    book_id: str,
    positions: list[PositionRisk],
    loadings: dict[str, list[InstrumentLoading]],
    factor_returns_today: dict[FactorType, float],
    total_pnl: float,
    attribution_date: str = "",
) -> FactorPnlAttributionResult:
    """Attribute P&L to factors.

    factor_pnl[f] = sum_i(mv_i * loading_i_f * factor_return_f)
    idiosyncratic_pnl = total_pnl - sum(factor_pnls)
    """
    import datetime
    if not attribution_date:
        attribution_date = datetime.date.today().isoformat()

    factor_pnl_by_factor: dict[FactorType, float] = {f: 0.0 for f in factor_returns_today}

    for pos in positions:
        pos_loadings = loadings.get(pos.instrument_id, [])
        mv = pos.market_value
        for il in pos_loadings:
            if il.factor in factor_returns_today:
                factor_pnl_by_factor[il.factor] += mv * il.loading * factor_returns_today[il.factor]

    total_factor_pnl = sum(factor_pnl_by_factor.values())
    idiosyncratic_pnl = total_pnl - total_factor_pnl

    factor_contributions = [
        FactorContribution(
            factor=factor,
            factor_exposure=0.0,  # not used in P&L attribution context
            factor_var=0.0,
            pnl_attribution=pnl,
            pct_of_total_var=0.0,
        )
        for factor, pnl in factor_pnl_by_factor.items()
    ]

    return FactorPnlAttributionResult(
        book_id=book_id,
        attribution_date=attribution_date,
        total_pnl=total_pnl,
        factor_pnl=factor_contributions,
        idiosyncratic_pnl=idiosyncratic_pnl,
    )
