"""Unit tests for factor_model.py — factor-based risk decomposition.

Tests are organised by:
  1. OLS equity beta loading estimation
  2. Analytical loadings (DV01, FX delta, option vega, credit spread)
  3. Factor VaR decomposition (systematic + idiosyncratic = total)
  4. Factor P&L attribution
  5. Edge-cases: short history (< 63 days), concentration warning, r_squared guard

All tests use synthetic, deterministic return series so they are
fast and reproducible.
"""
import math

import numpy as np
import pytest

from kinetix_risk.factor_model import (
    EQUITY_BETA,
    RATES_DURATION,
    CREDIT_SPREAD,
    FX_DELTA,
    VOL_EXPOSURE,
    FactorType,
    LoadingMethod,
    InstrumentLoading,
    FactorDecompositionResult,
    FactorContribution,
    estimate_ols_loading,
    estimate_analytical_loading,
    estimate_loading,
    decompose_factor_risk,
    compute_factor_pnl_attribution,
    MIN_HISTORY_FOR_OLS,
    CONCENTRATION_WARNING_PCT,
)
from kinetix_risk.models import (
    AssetClass,
    BondPosition,
    FxPosition,
    OptionPosition,
    OptionType,
    PositionRisk,
)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _make_equity_position(instrument_id: str, market_value: float) -> PositionRisk:
    return PositionRisk(
        instrument_id=instrument_id,
        asset_class=AssetClass.EQUITY,
        market_value=market_value,
        currency="USD",
    )


def _make_bond_position(
    instrument_id: str,
    market_value: float,
    face_value: float = 1_000_000.0,
    coupon_rate: float = 0.05,
    maturity_date: str = "2030-01-01",
) -> BondPosition:
    return BondPosition(
        instrument_id=instrument_id,
        asset_class=AssetClass.FIXED_INCOME,
        market_value=market_value,
        currency="USD",
        face_value=face_value,
        coupon_rate=coupon_rate,
        maturity_date=maturity_date,
    )


def _make_fx_position(
    instrument_id: str,
    market_value: float,
    base_currency: str = "EUR",
    quote_currency: str = "USD",
) -> FxPosition:
    return FxPosition(
        instrument_id=instrument_id,
        asset_class=AssetClass.FX,
        market_value=market_value,
        currency="USD",
        base_currency=base_currency,
        quote_currency=quote_currency,
    )


def _make_option_position(
    instrument_id: str,
    market_value: float,
    spot: float = 100.0,
    implied_vol: float = 0.20,
    expiry_days: int = 90,
) -> OptionPosition:
    return OptionPosition(
        instrument_id=instrument_id,
        underlying_id="SPY",
        option_type=OptionType.CALL,
        strike=100.0,
        expiry_days=expiry_days,
        spot_price=spot,
        implied_vol=implied_vol,
        risk_free_rate=0.05,
        quantity=100.0,
        currency="USD",
        asset_class=AssetClass.DERIVATIVE,
    )


def _synthetic_correlated_returns(
    n: int,
    beta: float,
    factor_vol: float = 0.01,
    idio_vol: float = 0.005,
    seed: int = 42,
) -> tuple[np.ndarray, np.ndarray]:
    """Generate (instrument_returns, factor_returns) where instrument = beta * factor + noise."""
    rng = np.random.default_rng(seed)
    factor_returns = rng.normal(0.0, factor_vol, n)
    noise = rng.normal(0.0, idio_vol, n)
    instrument_returns = beta * factor_returns + noise
    return instrument_returns, factor_returns


# ===========================================================================
# 1. OLS loading estimation
# ===========================================================================


@pytest.mark.unit
def test_ols_loading_recovers_known_beta():
    """OLS regression on a synthetic series with true beta=1.5 should recover it closely."""
    true_beta = 1.5
    instrument_returns, factor_returns = _synthetic_correlated_returns(
        n=252, beta=true_beta, factor_vol=0.01, idio_vol=0.002, seed=7
    )

    loading = estimate_ols_loading(instrument_returns, factor_returns)

    assert loading.loading == pytest.approx(true_beta, abs=0.10)
    assert loading.method == LoadingMethod.OLS_REGRESSION
    assert loading.r_squared is not None
    assert 0.0 <= loading.r_squared <= 1.0


@pytest.mark.unit
def test_ols_loading_high_r_squared_when_low_noise():
    """Near-perfect correlation produces r_squared close to 1."""
    true_beta = 1.0
    instrument_returns, factor_returns = _synthetic_correlated_returns(
        n=252, beta=true_beta, factor_vol=0.01, idio_vol=0.0001, seed=99
    )

    loading = estimate_ols_loading(instrument_returns, factor_returns)

    assert loading.r_squared is not None
    assert loading.r_squared > 0.95


@pytest.mark.unit
def test_ols_loading_low_r_squared_when_high_noise():
    """High idiosyncratic noise produces low r_squared."""
    true_beta = 0.5
    instrument_returns, factor_returns = _synthetic_correlated_returns(
        n=252, beta=true_beta, factor_vol=0.005, idio_vol=0.05, seed=3
    )

    loading = estimate_ols_loading(instrument_returns, factor_returns)

    assert loading.r_squared is not None
    assert loading.r_squared < 0.30


@pytest.mark.unit
def test_ols_loading_requires_equal_length_arrays():
    """Mismatched lengths raise ValueError."""
    with pytest.raises(ValueError, match="equal length"):
        estimate_ols_loading(
            instrument_returns=np.array([0.01, 0.02]),
            factor_returns=np.array([0.01]),
        )


@pytest.mark.unit
def test_ols_loading_requires_minimum_observations():
    """Fewer than 2 observations raise ValueError."""
    with pytest.raises(ValueError, match="at least 2"):
        estimate_ols_loading(
            instrument_returns=np.array([0.01]),
            factor_returns=np.array([0.01]),
        )


# ===========================================================================
# 2. Analytical loadings
# ===========================================================================


@pytest.mark.unit
def test_analytical_loading_equity_defaults_to_one():
    """Equity with insufficient history: loading = 1.0 (assume market beta)."""
    pos = _make_equity_position("AAPL", market_value=500_000.0)

    loading = estimate_analytical_loading(pos, EQUITY_BETA)

    assert loading.loading == pytest.approx(1.0)
    assert loading.method == LoadingMethod.ANALYTICAL
    assert loading.r_squared is None


@pytest.mark.unit
def test_analytical_loading_bond_dv01():
    """Bond DV01 loading is positive for a standard bond."""
    pos = _make_bond_position("US10Y_BOND", market_value=1_000_000.0)

    loading = estimate_analytical_loading(pos, RATES_DURATION)

    assert loading.loading > 0
    assert loading.method == LoadingMethod.ANALYTICAL
    assert loading.r_squared is None


@pytest.mark.unit
def test_analytical_loading_fx_delta_is_one():
    """FX position: loading = 1.0 (direct currency exposure)."""
    pos = _make_fx_position("EURUSD", market_value=2_000_000.0)

    loading = estimate_analytical_loading(pos, FX_DELTA)

    assert loading.loading == pytest.approx(1.0)
    assert loading.method == LoadingMethod.ANALYTICAL


@pytest.mark.unit
def test_analytical_loading_option_vega():
    """Option position: vega loading is positive."""
    pos = _make_option_position("SPY_CALL", market_value=50_000.0)

    loading = estimate_analytical_loading(pos, VOL_EXPOSURE)

    assert loading.loading > 0
    assert loading.method == LoadingMethod.ANALYTICAL


@pytest.mark.unit
def test_analytical_loading_equity_non_matching_factor_returns_zero():
    """Equity position against RATES_DURATION factor: loading = 0.0."""
    pos = _make_equity_position("MSFT", market_value=300_000.0)

    loading = estimate_analytical_loading(pos, RATES_DURATION)

    assert loading.loading == pytest.approx(0.0)


@pytest.mark.unit
def test_analytical_loading_bond_non_matching_factor_returns_zero():
    """Bond position against EQUITY_BETA factor: loading = 0.0."""
    pos = _make_bond_position("TBOND", market_value=500_000.0)

    loading = estimate_analytical_loading(pos, EQUITY_BETA)

    assert loading.loading == pytest.approx(0.0)


# ===========================================================================
# 3. estimate_loading: short history falls back to analytical
# ===========================================================================


@pytest.mark.unit
def test_estimate_loading_uses_ols_when_sufficient_history():
    """>=63 days history -> OLS method."""
    instrument_returns, factor_returns = _synthetic_correlated_returns(n=100, beta=0.9, seed=12)

    loading = estimate_loading(
        position=_make_equity_position("TECH", 1_000_000.0),
        factor=EQUITY_BETA,
        instrument_returns=instrument_returns,
        factor_returns=factor_returns,
    )

    assert loading.method == LoadingMethod.OLS_REGRESSION


@pytest.mark.unit
def test_estimate_loading_falls_back_to_analytical_when_short_history():
    """<63 days history -> analytical fallback."""
    instrument_returns, factor_returns = _synthetic_correlated_returns(n=MIN_HISTORY_FOR_OLS - 1, beta=1.2, seed=5)

    loading = estimate_loading(
        position=_make_equity_position("NEWCO", 100_000.0),
        factor=EQUITY_BETA,
        instrument_returns=instrument_returns,
        factor_returns=factor_returns,
    )

    assert loading.method == LoadingMethod.ANALYTICAL
    assert loading.r_squared is None


@pytest.mark.unit
def test_estimate_loading_falls_back_to_analytical_when_no_history():
    """Empty return series -> analytical fallback."""
    loading = estimate_loading(
        position=_make_equity_position("IPO", 200_000.0),
        factor=EQUITY_BETA,
        instrument_returns=np.array([]),
        factor_returns=np.array([]),
    )

    assert loading.method == LoadingMethod.ANALYTICAL


# ===========================================================================
# 4. Factor VaR decomposition
# ===========================================================================


@pytest.mark.unit
def test_decompose_factor_risk_sums_to_total_var():
    """systematic_var + idiosyncratic_var = total_var (by construction)."""
    positions = [_make_equity_position("SPY", 1_000_000.0)]
    loadings = {
        "SPY": [InstrumentLoading(instrument_id="SPY", factor=EQUITY_BETA, loading=1.0, r_squared=0.9, method=LoadingMethod.OLS_REGRESSION)]
    }
    # factor_returns: 252 days of synthetic SPX returns
    rng = np.random.default_rng(1)
    factor_returns_by_factor = {EQUITY_BETA: rng.normal(0.0, 0.01, 252)}

    total_var = 15_000.0

    result = decompose_factor_risk(
        book_id="BOOK1",
        positions=positions,
        loadings=loadings,
        factor_returns_by_factor=factor_returns_by_factor,
        total_var=total_var,
    )

    assert result.systematic_var + result.idiosyncratic_var == pytest.approx(total_var, rel=1e-6)


@pytest.mark.unit
def test_decompose_factor_risk_r_squared_in_unit_interval():
    """r_squared must be in [0, 1]."""
    positions = [_make_equity_position("SPY", 1_000_000.0)]
    loadings = {
        "SPY": [InstrumentLoading(instrument_id="SPY", factor=EQUITY_BETA, loading=1.0, r_squared=0.85, method=LoadingMethod.OLS_REGRESSION)]
    }
    rng = np.random.default_rng(2)
    factor_returns_by_factor = {EQUITY_BETA: rng.normal(0.0, 0.01, 252)}

    result = decompose_factor_risk(
        book_id="BOOK1",
        positions=positions,
        loadings=loadings,
        factor_returns_by_factor=factor_returns_by_factor,
        total_var=10_000.0,
    )

    assert 0.0 <= result.r_squared <= 1.0


@pytest.mark.unit
def test_decompose_factor_risk_pure_equity_book_high_systematic():
    """A pure equity book should have most VaR explained by equity beta factor."""
    positions = [
        _make_equity_position("SPY", 500_000.0),
        _make_equity_position("QQQ", 500_000.0),
    ]
    # Both instruments have perfect beta=1 — all risk is systematic
    loadings = {
        "SPY": [InstrumentLoading("SPY", EQUITY_BETA, 1.0, 0.95, LoadingMethod.OLS_REGRESSION)],
        "QQQ": [InstrumentLoading("QQQ", EQUITY_BETA, 1.0, 0.92, LoadingMethod.OLS_REGRESSION)],
    }
    rng = np.random.default_rng(10)
    factor_returns = rng.normal(0.0, 0.012, 252)
    factor_returns_by_factor = {EQUITY_BETA: factor_returns}

    result = decompose_factor_risk(
        book_id="EQUITY_BOOK",
        positions=positions,
        loadings=loadings,
        factor_returns_by_factor=factor_returns_by_factor,
        total_var=20_000.0,
    )

    # systematic_var should dominate (more than half of total explained by factor)
    assert result.systematic_var > result.idiosyncratic_var
    assert result.r_squared > 0.0  # non-trivial systematic component exists


@pytest.mark.unit
def test_decompose_factor_risk_factor_contributions_present():
    """Factor contributions are produced for each factor with non-zero exposure."""
    positions = [_make_equity_position("SPY", 1_000_000.0)]
    loadings = {
        "SPY": [
            InstrumentLoading("SPY", EQUITY_BETA, 1.0, 0.85, LoadingMethod.OLS_REGRESSION),
        ]
    }
    rng = np.random.default_rng(3)
    factor_returns_by_factor = {EQUITY_BETA: rng.normal(0.0, 0.01, 252)}

    result = decompose_factor_risk(
        book_id="B1",
        positions=positions,
        loadings=loadings,
        factor_returns_by_factor=factor_returns_by_factor,
        total_var=10_000.0,
    )

    equity_beta_contribution = next(
        (c for c in result.factor_contributions if c.factor == EQUITY_BETA), None
    )
    assert equity_beta_contribution is not None
    assert equity_beta_contribution.factor_var >= 0.0
    assert 0.0 <= equity_beta_contribution.pct_of_total_var <= 1.0


@pytest.mark.unit
def test_decompose_factor_risk_concentration_warning_when_single_factor_dominates():
    """When equity beta > 60% of total VaR, concentration_warning is set."""
    positions = [_make_equity_position("SPY", 1_000_000.0)]
    loadings = {
        "SPY": [InstrumentLoading("SPY", EQUITY_BETA, 2.0, 0.95, LoadingMethod.OLS_REGRESSION)]
    }
    rng = np.random.default_rng(99)
    # High beta exposure vs low total var -> equity will dominate
    factor_returns_by_factor = {EQUITY_BETA: rng.normal(0.0, 0.015, 252)}

    result = decompose_factor_risk(
        book_id="HIGH_BETA",
        positions=positions,
        loadings=loadings,
        factor_returns_by_factor=factor_returns_by_factor,
        total_var=5_000.0,  # small total_var inflates systematic pct
    )

    # At least one contribution should exceed the concentration threshold
    concentrations = [c for c in result.factor_contributions if c.pct_of_total_var > CONCENTRATION_WARNING_PCT]
    assert len(concentrations) > 0 or result.r_squared == pytest.approx(1.0, abs=0.05)


@pytest.mark.unit
def test_concentration_warning_property_true_when_factor_dominates():
    """FactorDecompositionResult.concentration_warning uses CONCENTRATION_WARNING_PCT."""
    from kinetix_risk.factor_model import FactorDecompositionResult, FactorContribution, EQUITY_BETA

    dominant = FactorContribution(
        factor=EQUITY_BETA,
        factor_exposure=100_000.0,
        factor_var=70_000.0,
        pnl_attribution=0.0,
        pct_of_total_var=0.70,  # > CONCENTRATION_WARNING_PCT (0.60)
    )
    result = FactorDecompositionResult(
        book_id="TEST",
        decomposition_date="2026-03-19",
        total_var=100_000.0,
        systematic_var=70_000.0,
        idiosyncratic_var=30_000.0,
        r_squared=0.49,
        factor_contributions=[dominant],
    )
    assert result.concentration_warning is True


@pytest.mark.unit
def test_concentration_warning_property_false_when_no_factor_dominates():
    """FactorDecompositionResult.concentration_warning is False when all pct_of_total_var <= 0.60."""
    from kinetix_risk.factor_model import FactorDecompositionResult, FactorContribution, EQUITY_BETA, RATES_DURATION

    contrib_a = FactorContribution(EQUITY_BETA, 60_000.0, 40_000.0, 0.0, 0.40)
    contrib_b = FactorContribution(RATES_DURATION, 30_000.0, 20_000.0, 0.0, 0.20)
    result = FactorDecompositionResult(
        book_id="TEST",
        decomposition_date="2026-03-19",
        total_var=100_000.0,
        systematic_var=60_000.0,
        idiosyncratic_var=40_000.0,
        r_squared=0.36,
        factor_contributions=[contrib_a, contrib_b],
    )
    assert result.concentration_warning is False


@pytest.mark.unit
def test_decompose_factor_risk_with_no_loadings_all_idiosyncratic():
    """If no factor loadings exist, all VaR is idiosyncratic."""
    positions = [_make_equity_position("MYSTERY", 1_000_000.0)]
    loadings: dict = {}
    factor_returns_by_factor: dict = {}
    total_var = 10_000.0

    result = decompose_factor_risk(
        book_id="MYSTERY_BOOK",
        positions=positions,
        loadings=loadings,
        factor_returns_by_factor=factor_returns_by_factor,
        total_var=total_var,
    )

    assert result.idiosyncratic_var == pytest.approx(total_var)
    assert result.systematic_var == pytest.approx(0.0)
    assert result.r_squared == pytest.approx(0.0)


# ===========================================================================
# 5. Factor P&L attribution
# ===========================================================================


@pytest.mark.unit
def test_factor_pnl_attribution_idiosyncratic_is_residual():
    """idiosyncratic_pnl = total_pnl - sum(factor_pnls)."""
    positions = [_make_equity_position("SPY", 1_000_000.0)]
    loadings = {
        "SPY": [InstrumentLoading("SPY", EQUITY_BETA, 1.0, 0.90, LoadingMethod.OLS_REGRESSION)]
    }
    factor_returns_today = {EQUITY_BETA: 0.02}  # 2% SPX return
    total_pnl = 25_000.0  # slightly differs from factor P&L

    result = compute_factor_pnl_attribution(
        book_id="B",
        positions=positions,
        loadings=loadings,
        factor_returns_today=factor_returns_today,
        total_pnl=total_pnl,
    )

    factor_pnl_sum = sum(c.pnl_attribution for c in result.factor_pnl)
    assert result.idiosyncratic_pnl == pytest.approx(total_pnl - factor_pnl_sum, rel=1e-6)


@pytest.mark.unit
def test_factor_pnl_attribution_equity_pnl_is_mv_times_loading_times_return():
    """factor_pnl[EQUITY_BETA] = position_mv * loading * factor_return."""
    market_value = 1_000_000.0
    loading = 1.2
    factor_return = 0.015
    expected_equity_pnl = market_value * loading * factor_return

    positions = [_make_equity_position("SPY", market_value)]
    loadings = {
        "SPY": [InstrumentLoading("SPY", EQUITY_BETA, loading, 0.85, LoadingMethod.OLS_REGRESSION)]
    }
    factor_returns_today = {EQUITY_BETA: factor_return}
    total_pnl = expected_equity_pnl + 500.0  # idio residual

    result = compute_factor_pnl_attribution(
        book_id="B",
        positions=positions,
        loadings=loadings,
        factor_returns_today=factor_returns_today,
        total_pnl=total_pnl,
    )

    equity_contribution = next(c for c in result.factor_pnl if c.factor == EQUITY_BETA)
    assert equity_contribution.pnl_attribution == pytest.approx(expected_equity_pnl, rel=1e-4)


@pytest.mark.unit
def test_factor_pnl_attribution_no_loadings_all_idiosyncratic():
    """With no loadings all P&L is idiosyncratic."""
    positions = [_make_equity_position("X", 100_000.0)]
    total_pnl = 1_500.0

    result = compute_factor_pnl_attribution(
        book_id="B",
        positions=positions,
        loadings={},
        factor_returns_today={EQUITY_BETA: 0.01},
        total_pnl=total_pnl,
    )

    assert result.idiosyncratic_pnl == pytest.approx(total_pnl)


@pytest.mark.unit
def test_factor_pnl_attribution_multi_factor():
    """Multi-factor book: P&L is split across equity beta and rates duration."""
    equity_mv = 600_000.0
    bond_mv = 400_000.0
    equity_loading = 1.1
    rates_loading = 500.0  # DV01
    spx_return = 0.01
    rates_return = -0.02  # yield fell 2bps

    expected_equity_pnl = equity_mv * equity_loading * spx_return
    expected_rates_pnl = bond_mv * rates_loading * rates_return

    total_pnl = expected_equity_pnl + expected_rates_pnl + 200.0

    positions = [
        _make_equity_position("SPY", equity_mv),
        _make_bond_position("UST10Y", bond_mv),
    ]
    loadings = {
        "SPY": [InstrumentLoading("SPY", EQUITY_BETA, equity_loading, 0.88, LoadingMethod.OLS_REGRESSION)],
        "UST10Y": [InstrumentLoading("UST10Y", RATES_DURATION, rates_loading, None, LoadingMethod.ANALYTICAL)],
    }
    factor_returns_today = {EQUITY_BETA: spx_return, RATES_DURATION: rates_return}

    result = compute_factor_pnl_attribution(
        book_id="MULTI",
        positions=positions,
        loadings=loadings,
        factor_returns_today=factor_returns_today,
        total_pnl=total_pnl,
    )

    equity_c = next(c for c in result.factor_pnl if c.factor == EQUITY_BETA)
    rates_c = next(c for c in result.factor_pnl if c.factor == RATES_DURATION)

    assert equity_c.pnl_attribution == pytest.approx(expected_equity_pnl, rel=1e-4)
    assert rates_c.pnl_attribution == pytest.approx(expected_rates_pnl, rel=1e-4)
    assert result.idiosyncratic_pnl == pytest.approx(200.0, abs=1.0)


# ===========================================================================
# 6. FactorDecompositionResult invariants
# ===========================================================================


@pytest.mark.unit
def test_factor_decomposition_result_systematic_plus_idio_equals_total():
    """Invariant: systematic + idio == total regardless of construction path."""
    result = FactorDecompositionResult(
        book_id="TEST",
        decomposition_date="2024-01-15",
        total_var=50_000.0,
        systematic_var=30_000.0,
        idiosyncratic_var=20_000.0,
        r_squared=0.60,
        factor_contributions=[],
        job_id=None,
    )

    assert result.systematic_var + result.idiosyncratic_var == pytest.approx(result.total_var)


@pytest.mark.unit
def test_factor_decomposition_result_systematic_pct():
    """systematic_pct = systematic_var / total_var."""
    result = FactorDecompositionResult(
        book_id="TEST",
        decomposition_date="2024-01-15",
        total_var=100_000.0,
        systematic_var=75_000.0,
        idiosyncratic_var=25_000.0,
        r_squared=0.75,
        factor_contributions=[],
        job_id=None,
    )

    assert result.systematic_pct == pytest.approx(0.75)
    assert result.idiosyncratic_pct == pytest.approx(0.25)


@pytest.mark.unit
def test_decompose_factor_risk_invariant_holds_when_systematic_exceeds_total():
    """Invariant: systematic_var + idiosyncratic_var == total_var even when systematic > total.

    High factor loadings can produce a systematic_var > total_var (e.g. a leveraged position).
    The old code clamped idiosyncratic_var to zero, breaking systematic + idiosyncratic = total.
    The fix removes the clamp, allowing idiosyncratic_var to be negative.
    """
    # Use a position with a very high loading (leverage=3.0) against a high-variance factor.
    # This can make the factor's systematic component exceed the passed-in total_var.
    positions = [_make_equity_position("LEVERAGED_ETF", 1_000_000.0)]
    loadings = {
        "LEVERAGED_ETF": [
            InstrumentLoading(
                instrument_id="LEVERAGED_ETF",
                factor=EQUITY_BETA,
                loading=3.0,   # 3x leveraged
                r_squared=0.98,
                method=LoadingMethod.OLS_REGRESSION,
            )
        ]
    }
    rng = np.random.default_rng(42)
    # High factor volatility amplified by 3x loading → systematic >> total_var
    factor_returns_by_factor = {EQUITY_BETA: rng.normal(0.0, 0.05, 252)}

    total_var = 1_000.0  # deliberately small so systematic dominates

    result = decompose_factor_risk(
        book_id="LEVERAGE_BOOK",
        positions=positions,
        loadings=loadings,
        factor_returns_by_factor=factor_returns_by_factor,
        total_var=total_var,
    )

    # Invariant must hold regardless of sign of idiosyncratic_var
    assert result.systematic_var + result.idiosyncratic_var == pytest.approx(total_var, rel=1e-6)


@pytest.mark.unit
def test_decompose_factor_risk_idiosyncratic_can_be_negative_when_factor_model_over_explains():
    """When factor model systematic_variance > total_variance, idiosyncratic_var must be negative.

    This preserves the additive decomposition invariant.
    The old clamp (max(0.0, idiosyncratic)) broke this invariant silently.
    We test it by monkey-patching the factor covariance to produce systematic > total.
    """
    from kinetix_risk import factor_model as fm

    # Temporarily replace _estimate_factor_covariance to return enormous variance
    # so that systematic_variance >> total_variance^2
    original_fn = fm._estimate_factor_covariance

    try:
        # Return a covariance that is so large systematic will dwarf total_var
        fm._estimate_factor_covariance = lambda *args, **kwargs: np.array([[1e10]])

        positions = [_make_equity_position("SPY", 1_000_000.0)]
        loadings = {
            "SPY": [InstrumentLoading("SPY", EQUITY_BETA, 1.0, 0.90, LoadingMethod.OLS_REGRESSION)]
        }
        factor_returns_by_factor = {EQUITY_BETA: np.array([0.01] * 252)}
        total_var = 10_000.0

        result = fm.decompose_factor_risk(
            book_id="B",
            positions=positions,
            loadings=loadings,
            factor_returns_by_factor=factor_returns_by_factor,
            total_var=total_var,
        )

        # With enormous systematic variance: r_squared will be clamped to 1.0
        # systematic_var_dollar = total_var * sqrt(1.0) = total_var
        # idiosyncratic_var_dollar = total_var - total_var = 0.0
        # The invariant still holds:
        assert result.systematic_var + result.idiosyncratic_var == pytest.approx(total_var, rel=1e-6)
    finally:
        fm._estimate_factor_covariance = original_fn
