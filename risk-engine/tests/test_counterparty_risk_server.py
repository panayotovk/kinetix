"""Unit tests for CounterpartyRiskServicer gRPC handler.

Tests exercise the servicer in isolation using an in-process gRPC server.
They verify proto translation, error handling, and delegation to the domain
functions in credit_exposure.py.  No Docker required.
"""
from __future__ import annotations

from concurrent import futures

import grpc
import pytest

from kinetix.risk import counterparty_risk_pb2, counterparty_risk_pb2_grpc
from kinetix_risk.counterparty_risk_server import CounterpartyRiskServicer

pytestmark = pytest.mark.unit


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------


@pytest.fixture(scope="module")
def grpc_channel():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=2))
    counterparty_risk_pb2_grpc.add_CounterpartyRiskServiceServicer_to_server(
        CounterpartyRiskServicer(), server
    )
    port = server.add_insecure_port("[::]:0")
    server.start()
    channel = grpc.insecure_channel(f"localhost:{port}")
    yield channel
    server.stop(grace=None)
    channel.close()


@pytest.fixture(scope="module")
def stub(grpc_channel):
    return counterparty_risk_pb2_grpc.CounterpartyRiskServiceStub(grpc_channel)


def _equity_position(
    instrument_id: str = "AAPL",
    market_value: float = 1_000_000.0,
    vol: float = 0.25,
    sector: str = "TECHNOLOGY",
) -> counterparty_risk_pb2.PFEPositionInput:
    return counterparty_risk_pb2.PFEPositionInput(
        instrument_id=instrument_id,
        market_value=market_value,
        asset_class="EQUITY",
        volatility=vol,
        sector=sector,
    )


def _pfe_request(**kwargs) -> counterparty_risk_pb2.CalculatePFERequest:
    defaults = dict(
        counterparty_id="CP-001",
        netting_set_id="NS-001",
        agreement_type="ISDA_2002",
        positions=[_equity_position()],
        num_simulations=1000,   # small for speed in unit tests
        seed=42,
    )
    defaults.update(kwargs)
    return counterparty_risk_pb2.CalculatePFERequest(**defaults)


def _sample_exposure_profile() -> list[counterparty_risk_pb2.ExposureProfile]:
    return [
        counterparty_risk_pb2.ExposureProfile(
            tenor="1Y", tenor_years=1.0,
            expected_exposure=500_000.0, pfe_95=750_000.0, pfe_99=900_000.0,
        ),
        counterparty_risk_pb2.ExposureProfile(
            tenor="5Y", tenor_years=5.0,
            expected_exposure=300_000.0, pfe_95=500_000.0, pfe_99=620_000.0,
        ),
    ]


def _cva_request(**kwargs) -> counterparty_risk_pb2.CalculateCVARequest:
    defaults = dict(
        counterparty_id="CP-001",
        exposure_profile=_sample_exposure_profile(),
        lgd=0.40,
        pd_1y=0.02,
    )
    defaults.update(kwargs)
    return counterparty_risk_pb2.CalculateCVARequest(**defaults)


# ---------------------------------------------------------------------------
# CalculatePFE tests
# ---------------------------------------------------------------------------


class TestCalculatePFE:
    def test_returns_six_tenor_profiles(self, stub):
        response = stub.CalculatePFE(_pfe_request())
        assert len(response.exposure_profile) == 6

    def test_counterparty_id_echoed(self, stub):
        response = stub.CalculatePFE(_pfe_request(counterparty_id="MY-BANK"))
        assert response.counterparty_id == "MY-BANK"

    def test_netting_set_id_echoed(self, stub):
        response = stub.CalculatePFE(_pfe_request(netting_set_id="NS-XYZ"))
        assert response.netting_set_id == "NS-XYZ"

    def test_gross_exposure_equals_sum_of_abs_market_values(self, stub):
        request = _pfe_request(
            positions=[
                _equity_position("A", market_value=1_000_000.0),
                _equity_position("B", market_value=-500_000.0),
            ]
        )
        response = stub.CalculatePFE(request)
        assert abs(response.gross_exposure - 1_500_000.0) < 1.0

    def test_net_exposure_non_negative_for_positive_book(self, stub):
        request = _pfe_request(positions=[_equity_position("A", market_value=1_000_000.0)])
        response = stub.CalculatePFE(request)
        assert response.net_exposure >= 0.0

    def test_pfe_95_in_profile_is_non_negative(self, stub):
        response = stub.CalculatePFE(_pfe_request())
        for ep in response.exposure_profile:
            assert ep.pfe_95 >= 0.0

    def test_pfe_99_at_least_pfe_95(self, stub):
        response = stub.CalculatePFE(_pfe_request())
        for ep in response.exposure_profile:
            assert ep.pfe_99 >= ep.pfe_95

    def test_empty_positions_returns_zero_exposure(self, stub):
        request = _pfe_request(positions=[])
        response = stub.CalculatePFE(request)
        assert response.gross_exposure == 0.0
        assert response.net_exposure == 0.0

    def test_calculated_at_populated(self, stub):
        response = stub.CalculatePFE(_pfe_request())
        assert response.calculated_at.seconds > 0

    def test_missing_counterparty_id_returns_invalid_argument(self, stub):
        request = _pfe_request(counterparty_id="")
        with pytest.raises(grpc.RpcError) as exc:
            stub.CalculatePFE(request)
        assert exc.value.code() == grpc.StatusCode.INVALID_ARGUMENT

    def test_missing_netting_set_id_returns_invalid_argument(self, stub):
        request = _pfe_request(netting_set_id="")
        with pytest.raises(grpc.RpcError) as exc:
            stub.CalculatePFE(request)
        assert exc.value.code() == grpc.StatusCode.INVALID_ARGUMENT

    def test_zero_num_simulations_uses_default(self, stub):
        # num_simulations=0 should default to 10_000 internally; just verify no error
        request = _pfe_request(num_simulations=0)
        response = stub.CalculatePFE(request)
        assert len(response.exposure_profile) == 6

    def test_invalid_correlation_matrix_size_returns_invalid_argument(self, stub):
        request = _pfe_request(
            positions=[_equity_position("A"), _equity_position("B")],
            correlation_matrix=[1.0, 0.5],  # should be 2*2 = 4 entries
        )
        with pytest.raises(grpc.RpcError) as exc:
            stub.CalculatePFE(request)
        assert exc.value.code() == grpc.StatusCode.INVALID_ARGUMENT

    def test_valid_correlation_matrix_accepted(self, stub):
        request = _pfe_request(
            positions=[_equity_position("A"), _equity_position("B")],
            correlation_matrix=[1.0, 0.3, 0.3, 1.0],  # 2x2, row-major
        )
        response = stub.CalculatePFE(request)
        assert len(response.exposure_profile) == 6

    def test_zero_volatility_defaults_to_20pct(self, stub):
        pos = counterparty_risk_pb2.PFEPositionInput(
            instrument_id="X",
            market_value=1_000_000.0,
            asset_class="EQUITY",
            volatility=0.0,  # should default to 0.20
            sector="OTHER",
        )
        request = _pfe_request(positions=[pos])
        response = stub.CalculatePFE(request)
        # Just assert it doesn't fail and returns a profile
        assert len(response.exposure_profile) == 6


# ---------------------------------------------------------------------------
# CalculateCVA tests
# ---------------------------------------------------------------------------


class TestCalculateCVA:
    def test_positive_cva_for_risky_counterparty(self, stub):
        response = stub.CalculateCVA(_cva_request(pd_1y=0.02))
        assert response.cva > 0.0

    def test_counterparty_id_echoed(self, stub):
        response = stub.CalculateCVA(_cva_request(counterparty_id="RISKY-BANK"))
        assert response.counterparty_id == "RISKY-BANK"

    def test_estimated_false_when_pd_provided(self, stub):
        response = stub.CalculateCVA(_cva_request(pd_1y=0.01))
        assert response.is_estimated is False

    def test_estimated_false_when_cds_spread_provided(self, stub):
        response = stub.CalculateCVA(_cva_request(pd_1y=0.0, cds_spread_bps=100.0))
        assert response.is_estimated is False

    def test_estimated_true_when_no_credit_data(self, stub):
        response = stub.CalculateCVA(_cva_request(pd_1y=0.0, sector="FINANCIALS"))
        assert response.is_estimated is True

    def test_higher_pd_gives_higher_cva(self, stub):
        r_low = stub.CalculateCVA(_cva_request(pd_1y=0.005))
        r_high = stub.CalculateCVA(_cva_request(pd_1y=0.05))
        assert r_high.cva > r_low.cva

    def test_zero_lgd_returns_invalid_argument(self, stub):
        with pytest.raises(grpc.RpcError) as exc:
            stub.CalculateCVA(_cva_request(lgd=0.0))
        assert exc.value.code() == grpc.StatusCode.INVALID_ARGUMENT

    def test_lgd_above_one_returns_invalid_argument(self, stub):
        with pytest.raises(grpc.RpcError) as exc:
            stub.CalculateCVA(_cva_request(lgd=1.5))
        assert exc.value.code() == grpc.StatusCode.INVALID_ARGUMENT

    def test_missing_counterparty_id_returns_invalid_argument(self, stub):
        with pytest.raises(grpc.RpcError) as exc:
            stub.CalculateCVA(_cva_request(counterparty_id=""))
        assert exc.value.code() == grpc.StatusCode.INVALID_ARGUMENT

    def test_empty_exposure_profile_gives_zero_cva(self, stub):
        response = stub.CalculateCVA(
            counterparty_risk_pb2.CalculateCVARequest(
                counterparty_id="CP-001",
                exposure_profile=[],
                lgd=0.40,
                pd_1y=0.02,
            )
        )
        assert response.cva == 0.0

    def test_hazard_rate_positive(self, stub):
        response = stub.CalculateCVA(_cva_request(pd_1y=0.02))
        assert response.hazard_rate > 0.0

    def test_pd_1y_positive_in_response(self, stub):
        response = stub.CalculateCVA(_cva_request(pd_1y=0.02))
        assert response.pd_1y > 0.0

    def test_calculated_at_populated(self, stub):
        response = stub.CalculateCVA(_cva_request())
        assert response.calculated_at.seconds > 0

    def test_zero_risk_free_rate_uses_default(self, stub):
        # risk_free_rate=0 should default to 0.05 internally
        response = stub.CalculateCVA(_cva_request(risk_free_rate=0.0))
        assert response.cva > 0.0
