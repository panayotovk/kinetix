"""Tests for position_resolver — delta-adjusted VaR for options."""

import pytest

from kinetix_risk.black_scholes import bs_delta
from kinetix_risk.models import (
    AssetClass,
    BondPosition,
    FuturePosition,
    OptionPosition,
    OptionType,
    PositionRisk,
)
from kinetix_risk.position_resolver import resolve_positions


@pytest.mark.unit
class TestResolvePositionsNonOption:
    def test_plain_position_risk_passes_through(self):
        pos = PositionRisk("AAPL", AssetClass.EQUITY, 1_000_000.0, "USD")
        result = resolve_positions([pos])
        assert len(result) == 1
        assert result[0] is pos

    def test_bond_position_passes_through(self):
        pos = BondPosition("US10Y", AssetClass.FIXED_INCOME, 980_000.0, "USD", face_value=1e6)
        result = resolve_positions([pos])
        assert len(result) == 1
        assert result[0] is pos

    def test_future_position_passes_through(self):
        pos = FuturePosition("SPX-SEP26", AssetClass.EQUITY, 250_000.0, "USD")
        result = resolve_positions([pos])
        assert result[0] is pos

    def test_empty_list(self):
        result = resolve_positions([])
        assert result == []


@pytest.mark.unit
class TestResolvePositionsOption:
    def test_option_with_market_data_produces_delta_adjusted_exposure(self):
        opt = OptionPosition(
            instrument_id="AAPL-C-200",
            underlying_id="AAPL",
            option_type=OptionType.CALL,
            strike=200.0,
            expiry_days=30,
            spot_price=195.0,
            implied_vol=0.25,
            risk_free_rate=0.05,
            quantity=10.0,
            contract_multiplier=100.0,
            asset_class=AssetClass.EQUITY,
        )
        result = resolve_positions([opt])
        assert len(result) == 1
        resolved = result[0]
        assert type(resolved) is PositionRisk
        assert resolved.instrument_id == "AAPL-C-200"
        assert resolved.asset_class == AssetClass.EQUITY

        # Verify the exposure is delta * quantity * spot * multiplier
        expected_delta = bs_delta(opt)
        expected_mv = expected_delta * 10.0 * 195.0 * 100.0
        assert resolved.market_value == pytest.approx(expected_mv, rel=1e-10)

    def test_call_delta_adjusted_exposure_is_positive(self):
        opt = OptionPosition(
            instrument_id="CALL",
            underlying_id="X",
            option_type=OptionType.CALL,
            strike=100.0,
            expiry_days=90,
            spot_price=105.0,
            implied_vol=0.20,
            quantity=1.0,
            contract_multiplier=1.0,
        )
        result = resolve_positions([opt])
        assert result[0].market_value > 0

    def test_put_delta_adjusted_exposure_is_negative(self):
        opt = OptionPosition(
            instrument_id="PUT",
            underlying_id="X",
            option_type=OptionType.PUT,
            strike=100.0,
            expiry_days=90,
            spot_price=95.0,
            implied_vol=0.20,
            quantity=1.0,
            contract_multiplier=1.0,
        )
        result = resolve_positions([opt])
        assert result[0].market_value < 0

    def test_option_without_market_data_passes_through(self):
        """Options with spot=0 (not enriched) pass through unchanged."""
        opt = OptionPosition(
            instrument_id="OPT",
            underlying_id="X",
            option_type=OptionType.CALL,
            strike=100.0,
            expiry_days=0,
            spot_price=0.0,
            implied_vol=0.0,
        )
        result = resolve_positions([opt])
        assert result[0] is opt

    def test_contract_multiplier_scales_exposure(self):
        base = OptionPosition(
            instrument_id="OPT",
            underlying_id="X",
            option_type=OptionType.CALL,
            strike=100.0,
            expiry_days=30,
            spot_price=105.0,
            implied_vol=0.20,
            quantity=1.0,
            contract_multiplier=1.0,
        )
        scaled = OptionPosition(
            instrument_id="OPT",
            underlying_id="X",
            option_type=OptionType.CALL,
            strike=100.0,
            expiry_days=30,
            spot_price=105.0,
            implied_vol=0.20,
            quantity=1.0,
            contract_multiplier=100.0,
        )
        result_base = resolve_positions([base])
        result_scaled = resolve_positions([scaled])
        assert result_scaled[0].market_value == pytest.approx(
            result_base[0].market_value * 100.0, rel=1e-10
        )


@pytest.mark.unit
class TestResolvePositionsMixed:
    def test_mixed_portfolio_resolves_only_options(self):
        positions = [
            PositionRisk("AAPL", AssetClass.EQUITY, 500_000.0, "USD"),
            OptionPosition(
                instrument_id="AAPL-C",
                underlying_id="AAPL",
                option_type=OptionType.CALL,
                strike=200.0,
                expiry_days=30,
                spot_price=195.0,
                implied_vol=0.25,
                quantity=5.0,
                contract_multiplier=100.0,
                asset_class=AssetClass.EQUITY,
            ),
            BondPosition("US10Y", AssetClass.FIXED_INCOME, 980_000.0, "USD"),
        ]
        result = resolve_positions(positions)
        assert len(result) == 3
        # First and third pass through
        assert result[0] is positions[0]
        assert result[2] is positions[2]
        # Second is resolved
        assert type(result[1]) is PositionRisk
        assert result[1].instrument_id == "AAPL-C"
