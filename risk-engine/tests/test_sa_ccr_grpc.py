"""Unit tests for SA-CCR gRPC servicer.

Tests exercise the SaCcrServicer in isolation via an in-process gRPC server.
They verify proto translation, error handling, and delegation to sa_ccr.py.
No Docker required.
"""
from __future__ import annotations

from concurrent import futures

import grpc
import pytest

from kinetix.risk import counterparty_risk_pb2, counterparty_risk_pb2_grpc
from kinetix_risk.sa_ccr_server import SaCcrServicer

pytestmark = pytest.mark.unit


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------


@pytest.fixture(scope="module")
def grpc_channel():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=2))
    counterparty_risk_pb2_grpc.add_SaCcrServiceServicer_to_server(
        SaCcrServicer(), server
    )
    port = server.add_insecure_port("[::]:0")
    server.start()
    channel = grpc.insecure_channel(f"localhost:{port}")
    yield channel
    server.stop(grace=None)
    channel.close()


@pytest.fixture(scope="module")
def stub(grpc_channel):
    return counterparty_risk_pb2_grpc.SaCcrServiceStub(grpc_channel)


def _ir_position(
    instrument_id: str = "SWAP-001",
    market_value: float = 100_000.0,
    notional: float = 10_000_000.0,
    asset_class: str = "IR",
    currency: str = "USD",
    pay_receive: str = "PAY_FIXED",
) -> counterparty_risk_pb2.SaCcrPositionInput:
    return counterparty_risk_pb2.SaCcrPositionInput(
        instrument_id=instrument_id,
        asset_class=asset_class,
        market_value=market_value,
        notional=notional,
        currency=currency,
        pay_receive=pay_receive,
    )


def _equity_position(
    instrument_id: str = "AAPL",
    market_value: float = 1_000_000.0,
) -> counterparty_risk_pb2.SaCcrPositionInput:
    return counterparty_risk_pb2.SaCcrPositionInput(
        instrument_id=instrument_id,
        asset_class="EQUITY",
        market_value=market_value,
        notional=market_value,
        currency="USD",
    )


def _sa_ccr_request(**kwargs) -> counterparty_risk_pb2.CalculateSaCcrRequest:
    defaults = dict(
        netting_set_id="NS-001",
        counterparty_id="CP-001",
        positions=[_ir_position()],
        collateral_net=0.0,
    )
    defaults.update(kwargs)
    return counterparty_risk_pb2.CalculateSaCcrRequest(**defaults)


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------


class TestCalculateSaCcr:
    def test_positive_ead_for_ir_swap(self, stub):
        response = stub.CalculateSaCcr(_sa_ccr_request())
        assert response.ead > 0.0

    def test_ead_equals_1_4_times_rc_plus_pfe(self, stub):
        response = stub.CalculateSaCcr(_sa_ccr_request())
        expected = 1.4 * (response.replacement_cost + response.pfe_addon)
        assert abs(response.ead - expected) < 0.01

    def test_alpha_is_1_4(self, stub):
        response = stub.CalculateSaCcr(_sa_ccr_request())
        assert abs(response.alpha - 1.4) < 1e-9

    def test_netting_set_id_echoed(self, stub):
        response = stub.CalculateSaCcr(_sa_ccr_request(netting_set_id="NS-XYZ"))
        assert response.netting_set_id == "NS-XYZ"

    def test_counterparty_id_echoed(self, stub):
        response = stub.CalculateSaCcr(_sa_ccr_request(counterparty_id="CP-ABC"))
        assert response.counterparty_id == "CP-ABC"

    def test_over_collateralised_has_zero_rc(self, stub):
        request = _sa_ccr_request(
            positions=[_ir_position(market_value=500_000.0)],
            collateral_net=1_000_000.0,
        )
        response = stub.CalculateSaCcr(request)
        assert response.replacement_cost == pytest.approx(0.0)

    def test_multiplier_between_0_05_and_1(self, stub):
        response = stub.CalculateSaCcr(_sa_ccr_request())
        assert 0.05 <= response.multiplier <= 1.0

    def test_empty_positions_returns_zero_ead(self, stub):
        request = _sa_ccr_request(positions=[])
        response = stub.CalculateSaCcr(request)
        assert response.ead == pytest.approx(0.0)

    def test_missing_netting_set_id_returns_invalid_argument(self, stub):
        request = _sa_ccr_request(netting_set_id="")
        with pytest.raises(grpc.RpcError) as exc:
            stub.CalculateSaCcr(request)
        assert exc.value.code() == grpc.StatusCode.INVALID_ARGUMENT

    def test_equity_position_uses_equity_supervisory_factor(self, stub):
        """Equity SF=0.32 >> IR SF=0.005, so equity addon should be much larger."""
        ir_response = stub.CalculateSaCcr(_sa_ccr_request(
            positions=[_ir_position(notional=1_000_000.0)]
        ))
        eq_response = stub.CalculateSaCcr(_sa_ccr_request(
            positions=[_equity_position(market_value=1_000_000.0)]
        ))
        assert eq_response.pfe_addon > ir_response.pfe_addon

    def test_calculated_at_is_populated(self, stub):
        response = stub.CalculateSaCcr(_sa_ccr_request())
        assert response.calculated_at.seconds > 0
