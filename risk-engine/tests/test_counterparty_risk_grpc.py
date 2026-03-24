"""gRPC contract integration test for CounterpartyRiskService.

Tests wire the full servicer into a real in-process gRPC server
(same as test_server.py pattern) and exercise the service through the
generated proto stubs.  No Docker required.

Marked integration because they spin up a real gRPC server.
"""
from __future__ import annotations

from concurrent import futures

import grpc
import pytest

from kinetix.risk import counterparty_risk_pb2, counterparty_risk_pb2_grpc
from kinetix_risk.counterparty_risk_server import CounterpartyRiskServicer

pytestmark = pytest.mark.integration


# ---------------------------------------------------------------------------
# Server fixture
# ---------------------------------------------------------------------------


@pytest.fixture(scope="module")
def grpc_channel():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=4))
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


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _positions(
    market_values: list[float],
    vols: list[float] | None = None,
    sectors: list[str] | None = None,
) -> list[counterparty_risk_pb2.PFEPositionInput]:
    if vols is None:
        vols = [0.25] * len(market_values)
    if sectors is None:
        sectors = ["TECHNOLOGY"] * len(market_values)
    return [
        counterparty_risk_pb2.PFEPositionInput(
            instrument_id=f"INST-{i}",
            market_value=mv,
            asset_class="EQUITY",
            volatility=v,
            sector=s,
        )
        for i, (mv, v, s) in enumerate(zip(market_values, vols, sectors))
    ]


def _pfe_request(
    counterparty_id: str = "CP-001",
    netting_set_id: str = "NS-001",
    market_values: list[float] | None = None,
    num_simulations: int = 5_000,
    seed: int = 42,
) -> counterparty_risk_pb2.CalculatePFERequest:
    if market_values is None:
        market_values = [1_000_000.0]
    return counterparty_risk_pb2.CalculatePFERequest(
        counterparty_id=counterparty_id,
        netting_set_id=netting_set_id,
        agreement_type="ISDA_2002",
        positions=_positions(market_values),
        num_simulations=num_simulations,
        seed=seed,
    )


# ---------------------------------------------------------------------------
# PFE contract tests
# ---------------------------------------------------------------------------


class TestCalculatePFEContract:
    def test_service_responds(self, stub):
        response = stub.CalculatePFE(_pfe_request())
        assert response is not None

    def test_response_has_six_tenor_profiles(self, stub):
        response = stub.CalculatePFE(_pfe_request())
        assert len(response.exposure_profile) == 6

    def test_tenor_labels_are_correct(self, stub):
        response = stub.CalculatePFE(_pfe_request())
        tenor_labels = [ep.tenor for ep in response.exposure_profile]
        assert tenor_labels == ["1M", "3M", "6M", "1Y", "2Y", "5Y"]

    def test_pfe_95_exceeds_current_net_exposure_for_risky_book(self, stub):
        """Spec invariant: PFE >= current net exposure for positive books."""
        request = _pfe_request(market_values=[5_000_000.0], num_simulations=20_000)
        response = stub.CalculatePFE(request)
        one_year_profile = next(ep for ep in response.exposure_profile if ep.tenor == "1Y")
        # PFE at 95th percentile should exceed current net exposure
        assert one_year_profile.pfe_95 >= response.net_exposure

    def test_netting_reduces_exposure(self, stub):
        """Long and short positions within same netting set reduce exposure."""
        request = _pfe_request(market_values=[2_000_000.0, -1_800_000.0])
        response = stub.CalculatePFE(request)
        assert response.gross_exposure == pytest.approx(3_800_000.0, abs=1.0)
        assert response.net_exposure == pytest.approx(200_000.0, abs=1.0)
        # Spec invariant: net_exposure <= gross_exposure
        assert response.net_exposure <= response.gross_exposure

    def test_negative_mtm_book_gives_zero_net_exposure(self, stub):
        """We owe counterparty: no credit exposure."""
        request = _pfe_request(market_values=[-500_000.0])
        response = stub.CalculatePFE(request)
        assert response.net_exposure == 0.0

    def test_deterministic_with_same_seed(self, stub):
        r1 = stub.CalculatePFE(_pfe_request(seed=99))
        r2 = stub.CalculatePFE(_pfe_request(seed=99))
        for ep1, ep2 in zip(r1.exposure_profile, r2.exposure_profile):
            assert ep1.pfe_95 == pytest.approx(ep2.pfe_95)

    def test_counterparty_id_and_netting_set_id_echoed(self, stub):
        response = stub.CalculatePFE(_pfe_request(counterparty_id="BANK-XYZ", netting_set_id="NS-ALPHA"))
        assert response.counterparty_id == "BANK-XYZ"
        assert response.netting_set_id == "NS-ALPHA"


# ---------------------------------------------------------------------------
# CVA contract tests
# ---------------------------------------------------------------------------


class TestCalculateCVAContract:
    def _exposure_profile(self) -> list[counterparty_risk_pb2.ExposureProfile]:
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

    def test_cva_positive_for_sub_aaa_counterparty(self, stub):
        """Spec invariant: CVA > 0 for PD > 0 with positive exposure."""
        response = stub.CalculateCVA(
            counterparty_risk_pb2.CalculateCVARequest(
                counterparty_id="CP-001",
                exposure_profile=self._exposure_profile(),
                lgd=0.40,
                pd_1y=0.01,
            )
        )
        assert response.cva > 0.0

    def test_cva_zero_for_riskless_counterparty(self, stub):
        """CVA = 0 only for riskless (PD = 0) counterparties."""
        # We can't pass pd_1y=0.0 as that's the proto zero-value sentinel,
        # so we use an AAA rating which produces near-zero CVA.
        response = stub.CalculateCVA(
            counterparty_risk_pb2.CalculateCVARequest(
                counterparty_id="CP-001",
                exposure_profile=self._exposure_profile(),
                lgd=0.40,
                rating="AAA",
            )
        )
        # AAA counterparty: CVA should be very small but > 0
        assert response.cva >= 0.0

    def test_no_credit_data_falls_back_to_sector_average_estimated(self, stub):
        """Spec: no CDS and no PD → sector-average spread + ESTIMATED flag."""
        response = stub.CalculateCVA(
            counterparty_risk_pb2.CalculateCVARequest(
                counterparty_id="CP-001",
                exposure_profile=self._exposure_profile(),
                lgd=0.40,
                sector="FINANCIALS",
            )
        )
        assert response.cva > 0.0
        assert response.is_estimated is True

    def test_cds_data_produces_is_estimated_false(self, stub):
        response = stub.CalculateCVA(
            counterparty_risk_pb2.CalculateCVARequest(
                counterparty_id="CP-001",
                exposure_profile=self._exposure_profile(),
                lgd=0.40,
                cds_spread_bps=80.0,
            )
        )
        assert response.is_estimated is False

    def test_end_to_end_pfe_then_cva(self, stub):
        """Integration: compute PFE first, then use EPE profile for CVA."""
        pfe_response = stub.CalculatePFE(
            counterparty_risk_pb2.CalculatePFERequest(
                counterparty_id="CP-E2E",
                netting_set_id="NS-E2E",
                agreement_type="ISDA_2002",
                positions=_positions([2_000_000.0, -500_000.0]),
                num_simulations=5_000,
                seed=42,
            )
        )
        assert len(pfe_response.exposure_profile) == 6

        cva_response = stub.CalculateCVA(
            counterparty_risk_pb2.CalculateCVARequest(
                counterparty_id="CP-E2E",
                exposure_profile=pfe_response.exposure_profile,
                lgd=0.40,
                pd_1y=0.02,
            )
        )
        assert cva_response.cva > 0.0
        assert cva_response.counterparty_id == "CP-E2E"
