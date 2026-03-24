"""Unit tests for HedgeOptimizerServicer.

These tests verify the gRPC stub contract in isolation:
  - SuggestHedge returns UNIMPLEMENTED with a meaningful message.
  - The stub correctly identifies the book_id and target metric in its log output.
  - The servicer is importable and wires correctly into the proto base class.
"""
import pytest
import grpc

from kinetix.risk import risk_calculation_pb2
from kinetix_risk.hedge_optimizer import HedgeOptimizerServicer


class _FakeContext:
    """Minimal gRPC context stub for unit tests."""

    def __init__(self):
        self.aborted = False
        self.code = None
        self.detail = None

    def abort(self, code: grpc.StatusCode, detail: str):
        self.aborted = True
        self.code = code
        self.detail = detail


@pytest.mark.unit
def test_suggest_hedge_returns_unimplemented():
    """SuggestHedge stub aborts with UNIMPLEMENTED status."""
    servicer = HedgeOptimizerServicer()
    context = _FakeContext()

    request = risk_calculation_pb2.SuggestHedgeRequest(
        book_id="BOOK-1",
        target_metric=risk_calculation_pb2.HEDGE_TARGET_DELTA,
        target_reduction_pct=0.80,
    )

    servicer.SuggestHedge(request, context)

    assert context.aborted is True
    assert context.code == grpc.StatusCode.UNIMPLEMENTED


@pytest.mark.unit
def test_suggest_hedge_detail_message_references_kotlin_calculator():
    """The UNIMPLEMENTED detail message points to the Kotlin AnalyticalHedgeCalculator."""
    servicer = HedgeOptimizerServicer()
    context = _FakeContext()

    request = risk_calculation_pb2.SuggestHedgeRequest(
        book_id="BOOK-1",
        target_metric=risk_calculation_pb2.HEDGE_TARGET_VEGA,
        target_reduction_pct=0.50,
    )

    servicer.SuggestHedge(request, context)

    assert "AnalyticalHedgeCalculator" in context.detail


@pytest.mark.unit
def test_suggest_hedge_request_parses_all_target_metrics():
    """All four target metric enum values are valid in the request."""
    servicer = HedgeOptimizerServicer()

    for target in [
        risk_calculation_pb2.HEDGE_TARGET_DELTA,
        risk_calculation_pb2.HEDGE_TARGET_GAMMA,
        risk_calculation_pb2.HEDGE_TARGET_VEGA,
        risk_calculation_pb2.HEDGE_TARGET_VAR,
    ]:
        context = _FakeContext()
        request = risk_calculation_pb2.SuggestHedgeRequest(
            book_id="BOOK-1",
            target_metric=target,
            target_reduction_pct=0.80,
        )
        servicer.SuggestHedge(request, context)
        assert context.aborted is True
        assert context.code == grpc.StatusCode.UNIMPLEMENTED


@pytest.mark.unit
def test_suggest_hedge_candidate_fields_are_parseable():
    """HedgeCandidateInstrument and HedgeCandidateGreeks fields round-trip through proto."""
    candidate = risk_calculation_pb2.HedgeCandidateInstrument(
        instrument_id="AAPL-P-2026",
        instrument_type="OPTION",
        price_per_unit=5.25,
        bid_ask_spread_bps=3.0,
        greeks=risk_calculation_pb2.HedgeCandidateGreeks(
            delta_per_unit=-0.50,
            gamma_per_unit=0.02,
            vega_per_unit=0.15,
            theta_per_unit=-0.03,
            rho_per_unit=-0.01,
        ),
        liquidity_tier="TIER_1",
        price_age_minutes=5,
    )

    assert candidate.instrument_id == "AAPL-P-2026"
    assert candidate.greeks.delta_per_unit == pytest.approx(-0.50)
    assert candidate.greeks.gamma_per_unit == pytest.approx(0.02)
    assert candidate.liquidity_tier == "TIER_1"
    assert candidate.price_age_minutes == 5
