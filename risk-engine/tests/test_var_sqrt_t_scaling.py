"""
Specification tests for the sqrt(T) VaR scaling rule.

These tests document and verify the Basel square-root-of-time assumption that
underpins all three VaR calculation paths (parametric, Monte Carlo, historical).

The rule VaR(T) = VaR(1) * sqrt(T) holds exactly when daily returns are i.i.d.
Under autocorrelation the actual multi-day quantile exceeds the scaled estimate,
which these tests demonstrate explicitly.
"""

import numpy as np
import pytest
from scipy.stats import norm

from kinetix_risk.models import AssetClass, AssetClassExposure, ConfidenceLevel
from kinetix_risk.var_parametric import calculate_parametric_var
from kinetix_risk.var_historical import calculate_historical_var


@pytest.mark.unit
def test_parametric_var_sqrt_t_scaling_assumes_iid_returns():
    """Parametric VaR(T) equals VaR(1) * sqrt(T) for any T under i.i.d. normality.

    This is an exact algebraic property of the parametric formula: the portfolio
    standard deviation is computed for a single day, then multiplied by sqrt(T).
    Verifying across three horizons (1, 5, 10) confirms the implementation encodes
    the i.i.d. assumption rather than simulating independent T-day paths.
    """
    exposures = [AssetClassExposure(AssetClass.EQUITY, 1_000_000.0, 0.20)]
    corr = np.array([[1.0]])
    confidence = ConfidenceLevel.CL_99

    var_1d = calculate_parametric_var(exposures, confidence, 1, corr).var_value
    var_5d = calculate_parametric_var(exposures, confidence, 5, corr).var_value
    var_10d = calculate_parametric_var(exposures, confidence, 10, corr).var_value

    assert var_5d == pytest.approx(var_1d * np.sqrt(5), rel=1e-9), (
        "5-day VaR must equal 1-day VaR * sqrt(5) under i.i.d. assumption"
    )
    assert var_10d == pytest.approx(var_1d * np.sqrt(10), rel=1e-9), (
        "10-day VaR must equal 1-day VaR * sqrt(10) under i.i.d. assumption"
    )


@pytest.mark.unit
def test_historical_var_sqrt_t_diverges_under_autocorrelation():
    """sqrt(T)-scaled 1-day VaR underestimates true multi-day risk under autocorrelation.

    We generate an AR(1) return series with strong positive autocorrelation
    (phi = 0.6).  Positive autocorrelation means a bad day tends to be followed
    by another bad day, so actual 10-day losses are larger than the i.i.d.
    sqrt(10) scaling of the 1-day quantile would predict.

    The test shows that the empirically observed 95th-percentile of actual 10-day
    losses (computed from non-overlapping windows of the AR(1) series) exceeds
    the sqrt(10)-scaled 1-day VaR by a statistically meaningful margin.
    """
    rng = np.random.default_rng(seed=0)

    # AR(1) parameters: phi close to 1 gives strong positive autocorrelation.
    phi = 0.6
    sigma_innovation = 0.01  # daily innovation std dev
    n_days = 100_000

    # Generate AR(1) series: r_t = phi * r_{t-1} + epsilon_t
    innovations = rng.normal(0.0, sigma_innovation, n_days)
    returns = np.empty(n_days)
    returns[0] = innovations[0]
    for t in range(1, n_days):
        returns[t] = phi * returns[t - 1] + innovations[t]

    # --- 1-day VaR via the historical method (pass returns as historical_returns) ---
    # Single-asset portfolio: $1 long position with effectively unit market value.
    # Returns are already dollar P&L per unit.
    market_value = 1.0
    exposures = [AssetClassExposure(AssetClass.EQUITY, market_value, 0.20)]
    corr = np.array([[1.0]])
    confidence = ConfidenceLevel.CL_95

    # historical_returns shape must be (n_scenarios, n_assets)
    historical_returns_2d = returns.reshape(-1, 1)
    result_1d = calculate_historical_var(
        exposures,
        confidence,
        time_horizon_days=1,
        correlation_matrix=corr,
        historical_returns=historical_returns_2d,
    )
    var_1d_scaled_to_10d = result_1d.var_value * np.sqrt(10)

    # --- True 10-day empirical VaR from non-overlapping windows ---
    horizon = 10
    n_windows = n_days // horizon
    window_returns = returns[: n_windows * horizon].reshape(n_windows, horizon)
    ten_day_losses = -window_returns.sum(axis=1)  # positive = loss
    empirical_10d_var = float(np.percentile(ten_day_losses, confidence.value * 100))

    # Under positive autocorrelation the true 10-day VaR must be strictly larger
    # than the sqrt(10)-scaled 1-day estimate.  We require a margin of at least
    # 5% to distinguish signal from noise at this sample size.
    assert empirical_10d_var > var_1d_scaled_to_10d * 1.05, (
        f"Expected autocorrelated 10-day empirical VaR ({empirical_10d_var:.6f}) "
        f"to exceed sqrt(10)-scaled 1-day VaR ({var_1d_scaled_to_10d:.6f}) by >5%, "
        "but it did not.  The sqrt(T) rule understates risk under autocorrelation."
    )
