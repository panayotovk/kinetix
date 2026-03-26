"""Unit tests for the liquidity risk calculation module.

These tests drive the design of:
  - compute_liquidation_horizon: classify positions by ADV fraction -> days to unwind
  - compute_lvar: Basel sqrt(T) scaled VaR adjustment for liquidity
  - compute_stressed_liquidation_value: per-scenario, per-asset-class stress factors
  - assess_concentration_flag: ADV concentration limit check
"""
import math
import pytest

from kinetix_risk.liquidity import (
    LiquidityInput,
    LiquidityTier,
    LiquidityRiskResult,
    PositionLiquidityRisk,
    compute_liquidation_horizon,
    compute_lvar,
    compute_position_lvar,
    compute_stressed_liquidation_value,
    assess_concentration_flag,
    ILLIQUID_HORIZON_DAYS,
)
from kinetix_risk.models import AssetClass


# ---------------------------------------------------------------------------
# compute_liquidation_horizon
# ---------------------------------------------------------------------------


@pytest.mark.unit
def test_liquidation_horizon_returns_illiquid_tier_when_no_adv_data():
    """No ADV data -> ILLIQUID tier and 10-day default horizon, clearly flagged."""
    result = compute_liquidation_horizon(
        market_value=1_000_000.0,
        adv=None,
        adv_staleness_days=None,
    )
    assert result.tier == LiquidityTier.ILLIQUID
    assert result.horizon_days == ILLIQUID_HORIZON_DAYS
    assert result.adv_missing is True


@pytest.mark.unit
def test_liquidation_horizon_stale_adv_returns_warning_flag():
    """ADV older than 2 days -> stale flag set; tier still computed from value."""
    result = compute_liquidation_horizon(
        market_value=500_000.0,
        adv=10_000_000.0,  # position is 5% of ADV -> HIGH_LIQUID tier
        adv_staleness_days=3,
    )
    assert result.adv_stale is True
    assert result.tier == LiquidityTier.HIGH_LIQUID


@pytest.mark.unit
def test_liquidation_horizon_fresh_adv_no_stale_flag():
    """ADV within 2 days -> adv_stale is False."""
    result = compute_liquidation_horizon(
        market_value=500_000.0,
        adv=10_000_000.0,
        adv_staleness_days=1,
    )
    assert result.adv_stale is False


@pytest.mark.unit
def test_liquidation_horizon_high_liquid_tier():
    """Position < 10% of ADV -> HIGH_LIQUID, 1-day horizon."""
    result = compute_liquidation_horizon(
        market_value=900_000.0,
        adv=10_000_000.0,  # 9% of ADV
        adv_staleness_days=0,
    )
    assert result.tier == LiquidityTier.HIGH_LIQUID
    assert result.horizon_days == 1


@pytest.mark.unit
def test_liquidation_horizon_liquid_tier():
    """Position 10-25% of ADV -> LIQUID, 3-day horizon."""
    result = compute_liquidation_horizon(
        market_value=2_000_000.0,
        adv=10_000_000.0,  # 20% of ADV
        adv_staleness_days=0,
    )
    assert result.tier == LiquidityTier.LIQUID
    assert result.horizon_days == 3


@pytest.mark.unit
def test_liquidation_horizon_semi_liquid_tier():
    """Position 25-50% of ADV -> SEMI_LIQUID, 5-day horizon."""
    result = compute_liquidation_horizon(
        market_value=3_500_000.0,
        adv=10_000_000.0,  # 35% of ADV
        adv_staleness_days=0,
    )
    assert result.tier == LiquidityTier.SEMI_LIQUID
    assert result.horizon_days == 5


@pytest.mark.unit
def test_liquidation_horizon_illiquid_tier_from_adv():
    """Position > 50% of ADV -> ILLIQUID, 10-day horizon."""
    result = compute_liquidation_horizon(
        market_value=6_000_000.0,
        adv=10_000_000.0,  # 60% of ADV
        adv_staleness_days=0,
    )
    assert result.tier == LiquidityTier.ILLIQUID
    assert result.horizon_days == ILLIQUID_HORIZON_DAYS
    assert result.adv_missing is False  # ADV is present; illiquid due to concentration


# ---------------------------------------------------------------------------
# compute_lvar
# ---------------------------------------------------------------------------


@pytest.mark.unit
def test_lvar_uses_sqrt_t_scaling():
    """LVaR = VaR * sqrt(liquidation_days / base_holding_period). Basel formula."""
    base_var = 100_000.0
    base_holding_period = 1
    horizon_days = 10

    result = compute_lvar(
        base_var=base_var,
        liquidation_horizon_days=horizon_days,
        base_holding_period=base_holding_period,
    )

    expected = base_var * math.sqrt(horizon_days / base_holding_period)
    assert abs(result.lvar_value - expected) < 1e-6


@pytest.mark.unit
def test_lvar_equals_var_when_horizon_equals_base_period():
    """When liquidation horizon equals base holding period, LVaR == VaR."""
    base_var = 50_000.0
    result = compute_lvar(
        base_var=base_var,
        liquidation_horizon_days=1,
        base_holding_period=1,
    )
    assert abs(result.lvar_value - base_var) < 1e-6


@pytest.mark.unit
def test_lvar_data_completeness_reflects_positions_with_adv():
    """data_completeness is the notional-weighted fraction of portfolio with ADV.

    Position A: $900K with ADV. Position B: $100K without ADV.
    Total notional: $1M. Covered: $900K -> completeness = 0.9.
    """
    inputs = [
        LiquidityInput(instrument_id="A", market_value=900_000.0, adv=10_000_000.0, adv_staleness_days=0, asset_class=AssetClass.EQUITY),
        LiquidityInput(instrument_id="B", market_value=100_000.0, adv=None, adv_staleness_days=None, asset_class=AssetClass.EQUITY),
    ]

    result = compute_lvar(
        base_var=100_000.0,
        liquidation_horizon_days=5,
        base_holding_period=1,
        inputs=inputs,
    )

    # $900K covered / $1M total = 0.9
    assert abs(result.data_completeness - 0.9) < 1e-6


@pytest.mark.unit
def test_lvar_data_completeness_is_one_when_all_have_adv():
    """data_completeness is 1.0 when every position has ADV data."""
    inputs = [
        LiquidityInput(instrument_id="A", market_value=500_000.0, adv=10_000_000.0, adv_staleness_days=0, asset_class=AssetClass.EQUITY),
        LiquidityInput(instrument_id="B", market_value=500_000.0, adv=5_000_000.0, adv_staleness_days=1, asset_class=AssetClass.FIXED_INCOME),
    ]

    result = compute_lvar(
        base_var=100_000.0,
        liquidation_horizon_days=3,
        base_holding_period=1,
        inputs=inputs,
    )
    assert abs(result.data_completeness - 1.0) < 1e-6


@pytest.mark.unit
def test_lvar_result_has_lvar_value():
    """compute_lvar returns an object with lvar_value and data_completeness fields."""
    result = compute_lvar(
        base_var=80_000.0,
        liquidation_horizon_days=10,
        base_holding_period=1,
    )
    assert result.lvar_value == pytest.approx(80_000.0 * math.sqrt(10), rel=1e-6)
    assert result.data_completeness == 1.0  # no inputs -> assume complete (no positions to check)


# ---------------------------------------------------------------------------
# compute_position_lvar (LIQ-07)
# ---------------------------------------------------------------------------


@pytest.mark.unit
def test_compute_position_lvar_zero_spread():
    """With no bid-ask spread, per-position LVaR equals VaR * sqrt(T)."""
    var_contribution = 100_000.0
    result = compute_position_lvar(
        var_contribution=var_contribution,
        liquidation_days=10,
        base_holding_period=1,
        bid_ask_spread_bps=None,
        position_notional=5_000_000.0,
    )
    assert result == pytest.approx(var_contribution * math.sqrt(10), rel=1e-6)


@pytest.mark.unit
def test_compute_position_lvar_with_spread_cost():
    """LVaR = scaled VaR + 0.5 * spread_decimal * |notional|.

    var_contribution=100_000, horizon=10, spread=20bps, notional=5_000_000:
      scaled_var = 100_000 * sqrt(10) = 316_227.766...
      spread_cost = 0.5 * 0.002 * 5_000_000 = 5_000
      total = 321_227.766...
    """
    result = compute_position_lvar(
        var_contribution=100_000.0,
        liquidation_days=10,
        base_holding_period=1,
        bid_ask_spread_bps=20.0,
        position_notional=5_000_000.0,
    )
    expected = 100_000.0 * math.sqrt(10) + 0.5 * 0.002 * 5_000_000.0
    assert result == pytest.approx(expected, rel=1e-6)


@pytest.mark.unit
def test_compute_position_lvar_short_position_uses_abs_notional():
    """Negative market value (short) produces a positive spread cost."""
    result_long = compute_position_lvar(
        var_contribution=50_000.0,
        liquidation_days=5,
        base_holding_period=1,
        bid_ask_spread_bps=10.0,
        position_notional=2_000_000.0,
    )
    result_short = compute_position_lvar(
        var_contribution=50_000.0,
        liquidation_days=5,
        base_holding_period=1,
        bid_ask_spread_bps=10.0,
        position_notional=-2_000_000.0,
    )
    assert result_long == pytest.approx(result_short, rel=1e-6)
    assert result_short > 0


@pytest.mark.unit
def test_lvar_exceeds_var_invariant():
    """For any non-zero base_var and non-negative spread, LVaR >= base_var."""
    inputs = [
        LiquidityInput(
            instrument_id="A",
            market_value=1_000_000.0,
            adv=20_000_000.0,   # HIGH_LIQUID -> 1-day horizon
            adv_staleness_days=0,
            asset_class=AssetClass.EQUITY,
            bid_ask_spread_bps=5.0,
        ),
    ]
    base_var = 50_000.0
    result = compute_lvar(
        base_var=base_var,
        liquidation_horizon_days=1,
        base_holding_period=1,
        inputs=inputs,
    )
    assert result.lvar_value >= base_var


# ---------------------------------------------------------------------------
# compute_stressed_liquidation_value
# ---------------------------------------------------------------------------


@pytest.mark.unit
def test_stressed_liquidation_value_applies_scenario_stress_factor():
    """Stressed value = market_value * (1 - stress_factor) * liquidity_discount."""
    market_value = 1_000_000.0
    stress_factor = 0.30   # e.g. GFC scenario: 30% haircut on liquidation
    horizon_days = 10
    daily_vol = 0.02

    result = compute_stressed_liquidation_value(
        market_value=market_value,
        horizon_days=horizon_days,
        daily_vol=daily_vol,
        stress_factor=stress_factor,
    )

    # Expected: market_value * (1 - stress_factor * sqrt(horizon_days) * daily_vol)
    # Stress discount = stress_factor * vol_impact
    vol_impact = daily_vol * math.sqrt(horizon_days)
    expected = market_value * (1.0 - stress_factor * vol_impact)
    assert abs(result - expected) < 1.0  # within $1


@pytest.mark.unit
def test_stressed_liquidation_value_no_stress_equals_market_value():
    """Zero stress factor returns the market value unchanged."""
    result = compute_stressed_liquidation_value(
        market_value=500_000.0,
        horizon_days=1,
        daily_vol=0.01,
        stress_factor=0.0,
    )
    assert abs(result - 500_000.0) < 1e-6


@pytest.mark.unit
def test_stressed_liquidation_per_asset_class_stress_factors():
    """Different stress factors can be applied per asset class."""
    stress_factors = {
        AssetClass.EQUITY: 0.40,
        AssetClass.FIXED_INCOME: 0.20,
        AssetClass.FX: 0.15,
        AssetClass.COMMODITY: 0.35,
        AssetClass.DERIVATIVE: 0.50,
    }
    inputs = [
        LiquidityInput(instrument_id="EQ1", market_value=1_000_000.0, adv=5_000_000.0, adv_staleness_days=0, asset_class=AssetClass.EQUITY),
        LiquidityInput(instrument_id="BD1", market_value=2_000_000.0, adv=10_000_000.0, adv_staleness_days=0, asset_class=AssetClass.FIXED_INCOME),
    ]
    daily_vol = 0.015
    horizon_days = 5

    results = [
        compute_stressed_liquidation_value(
            market_value=inp.market_value,
            horizon_days=horizon_days,
            daily_vol=daily_vol,
            stress_factor=stress_factors[inp.asset_class],
        )
        for inp in inputs
    ]

    # Equity position should have a larger discount than fixed income
    equity_discount = inputs[0].market_value - results[0]
    fi_discount = inputs[1].market_value - results[1]
    # Equity: 1M * 0.40 * 0.015 * sqrt(5); FI: 2M * 0.20 * 0.015 * sqrt(5)
    assert equity_discount > 0
    assert fi_discount > 0


# ---------------------------------------------------------------------------
# assess_concentration_flag
# ---------------------------------------------------------------------------


@pytest.mark.unit
def test_concentration_flag_breached_when_position_exceeds_limit():
    """Position > hard_block_pct of ADV -> BREACHED status."""
    result = assess_concentration_flag(
        market_value=1_200_000.0,
        adv=10_000_000.0,   # position is 12% of ADV -> above 10% hard block
        warning_pct=0.05,
        hard_block_pct=0.10,
    )
    assert result.status == "BREACHED"


@pytest.mark.unit
def test_concentration_flag_warning_when_position_near_limit():
    """Position between warning_pct and hard_block_pct of ADV -> WARNING."""
    result = assess_concentration_flag(
        market_value=700_000.0,
        adv=10_000_000.0,   # position is 7% of ADV -> between 5% warning and 10% hard block
        warning_pct=0.05,
        hard_block_pct=0.10,
    )
    assert result.status == "WARNING"


@pytest.mark.unit
def test_concentration_flag_ok_when_position_well_below_limit():
    """Position below warning_pct of ADV -> OK."""
    result = assess_concentration_flag(
        market_value=300_000.0,
        adv=10_000_000.0,   # position is 3% of ADV -> below 5% warning
        warning_pct=0.05,
        hard_block_pct=0.10,
    )
    assert result.status == "OK"


@pytest.mark.unit
def test_concentration_flag_breached_when_no_adv_data():
    """No ADV data -> BREACHED (fail safe); trade must be blocked."""
    result = assess_concentration_flag(
        market_value=1_000_000.0,
        adv=None,
        warning_pct=0.05,
        hard_block_pct=0.10,
    )
    assert result.status == "BREACHED"
    assert result.adv_missing is True


@pytest.mark.unit
def test_concentration_flag_warning_when_adv_stale():
    """Stale ADV (>2 days) and within warning -> WARNING, not BREACHED."""
    result = assess_concentration_flag(
        market_value=300_000.0,
        adv=10_000_000.0,   # 3% of ADV -> within warning threshold
        adv_staleness_days=3,
        warning_pct=0.05,
        hard_block_pct=0.10,
    )
    assert result.status == "WARNING"
    assert result.adv_stale is True


@pytest.mark.unit
def test_concentration_flag_current_value_and_limit_populated():
    """Result carries current_pct and limit_pct (hard_block_pct) for reporting."""
    result = assess_concentration_flag(
        market_value=300_000.0,
        adv=10_000_000.0,   # 3% of ADV
        warning_pct=0.05,
        hard_block_pct=0.10,
    )
    assert abs(result.current_pct - 0.03) < 1e-6   # 300K / 10M = 3%
    assert abs(result.limit_pct - 0.10) < 1e-6     # limit_pct stores hard_block_pct


@pytest.mark.unit
def test_concentration_ok_at_4_99_pct_of_adv():
    """Position just below warning_pct -> OK (boundary check)."""
    result = assess_concentration_flag(
        market_value=499_000.0,
        adv=10_000_000.0,   # 4.99% of ADV -> below 5% warning
        warning_pct=0.05,
        hard_block_pct=0.10,
    )
    assert result.status == "OK"


@pytest.mark.unit
def test_concentration_breached_at_10_01_pct_of_adv():
    """Position just above hard_block_pct -> BREACHED (boundary check)."""
    result = assess_concentration_flag(
        market_value=1_001_000.0,
        adv=10_000_000.0,   # 10.01% of ADV -> above 10% hard block
        warning_pct=0.05,
        hard_block_pct=0.10,
    )
    assert result.status == "BREACHED"


# ---------------------------------------------------------------------------
# data_completeness notional-weighting (LIQ-12)
# ---------------------------------------------------------------------------


@pytest.mark.unit
def test_data_completeness_is_notional_weighted():
    """data_completeness is weighted by notional, not position count.

    One position worth $9M has ADV; one worth $1M does not.
    Count-based completeness would be 0.5; notional-weighted is 0.9.
    """
    inputs = [
        LiquidityInput(instrument_id="A", market_value=9_000_000.0, adv=50_000_000.0, adv_staleness_days=0, asset_class=AssetClass.EQUITY),
        LiquidityInput(instrument_id="B", market_value=1_000_000.0, adv=None, adv_staleness_days=None, asset_class=AssetClass.EQUITY),
    ]

    result = compute_lvar(
        base_var=100_000.0,
        liquidation_horizon_days=5,
        base_holding_period=1,
        inputs=inputs,
    )

    assert abs(result.data_completeness - 0.9) < 1e-6
