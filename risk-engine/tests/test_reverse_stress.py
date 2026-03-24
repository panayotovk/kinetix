"""
Tests for reverse stress: find the minimum-norm shock vector that causes
a portfolio loss >= a given target.

Analytical cases used to verify correctness:
- Single position: minimum shock to lose $X from $M market value is X/M.
- Non-convergence (infeasible target) returns converged=False gracefully.
"""
import numpy as np
import pytest

from kinetix_risk.models import AssetClass, PositionRisk
from kinetix_risk.reverse_stress import (
    ReverseStressRequest,
    ReverseStressResult,
    run_reverse_stress,
)


def _single_equity(mv: float = 1_000_000.0) -> list[PositionRisk]:
    return [PositionRisk("AAPL", AssetClass.EQUITY, mv, "USD")]


def _two_positions() -> list[PositionRisk]:
    return [
        PositionRisk("AAPL", AssetClass.EQUITY, 1_000_000.0, "USD"),
        PositionRisk("UST10Y", AssetClass.FIXED_INCOME, 500_000.0, "USD"),
    ]


@pytest.mark.unit
class TestReverseStressRequest:
    def test_request_stores_target_loss(self):
        req = ReverseStressRequest(
            positions=_single_equity(),
            target_loss=100_000.0,
        )
        assert req.target_loss == 100_000.0
        assert len(req.positions) == 1


@pytest.mark.unit
class TestRunReverseStress:
    def test_single_position_analytical_shock(self):
        """For a single position of $1M, a target loss of $100k requires a -10% shock."""
        mv = 1_000_000.0
        target = 100_000.0
        req = ReverseStressRequest(
            positions=_single_equity(mv),
            target_loss=target,
        )
        result = run_reverse_stress(req)
        assert result.converged is True
        assert len(result.shocks) == 1
        shock = result.shocks[0]
        # The minimum norm shock to a single position is exactly -target/mv (negative = price decline)
        expected_shock = -target / mv
        assert shock == pytest.approx(expected_shock, rel=0.05)

    def test_achieved_loss_meets_target(self):
        """The achieved loss in the result must be >= the requested target loss."""
        req = ReverseStressRequest(
            positions=_single_equity(1_000_000.0),
            target_loss=200_000.0,
        )
        result = run_reverse_stress(req)
        assert result.converged is True
        assert result.achieved_loss >= result.target_loss * 0.95  # allow 5% tolerance

    def test_shocks_length_matches_positions(self):
        """One shock entry per position."""
        positions = _two_positions()
        req = ReverseStressRequest(positions=positions, target_loss=150_000.0)
        result = run_reverse_stress(req)
        assert len(result.shocks) == len(positions)

    def test_shocks_are_negative_or_zero(self):
        """Shocks represent price declines so all values must be <= 0."""
        req = ReverseStressRequest(
            positions=_two_positions(),
            target_loss=100_000.0,
        )
        result = run_reverse_stress(req)
        assert result.converged is True
        for shock in result.shocks:
            assert shock <= 0.0

    def test_minimum_norm_prefers_balanced_shocks(self):
        """With two equal-value positions, the solver distributes shocks evenly."""
        positions = [
            PositionRisk("A", AssetClass.EQUITY, 1_000_000.0, "USD"),
            PositionRisk("B", AssetClass.EQUITY, 1_000_000.0, "USD"),
        ]
        req = ReverseStressRequest(positions=positions, target_loss=200_000.0)
        result = run_reverse_stress(req)
        assert result.converged is True
        # Both shocks should have similar magnitude (minimum-norm spreads evenly)
        ratio = abs(result.shocks[0]) / abs(result.shocks[1])
        assert ratio == pytest.approx(1.0, abs=0.2)

    def test_infeasible_target_returns_not_converged(self):
        """When the target is impossible (e.g. larger than total portfolio value),
        the solver must return converged=False rather than hang or raise."""
        mv = 1_000_000.0
        # Target loss > 100% of portfolio value cannot be achieved by price shocks bounded at -100%
        req = ReverseStressRequest(
            positions=_single_equity(mv),
            target_loss=mv * 1.5,  # 150% of portfolio — impossible
            max_shock=-1.0,        # shocks capped at -100%
        )
        result = run_reverse_stress(req)
        assert result.converged is False

    def test_empty_positions_raises(self):
        req = ReverseStressRequest(positions=[], target_loss=100_000.0)
        with pytest.raises(ValueError, match="empty"):
            run_reverse_stress(req)

    def test_non_positive_target_raises(self):
        req = ReverseStressRequest(positions=_single_equity(), target_loss=0.0)
        with pytest.raises(ValueError, match="positive"):
            run_reverse_stress(req)

    def test_result_contains_shock_instrument_ids(self):
        """Result maps each shock back to its instrument_id."""
        req = ReverseStressRequest(
            positions=_two_positions(),
            target_loss=100_000.0,
        )
        result = run_reverse_stress(req)
        assert len(result.instrument_ids) == 2
        assert "AAPL" in result.instrument_ids
        assert "UST10Y" in result.instrument_ids
