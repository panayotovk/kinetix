"""
gRPC tests for the new RunHistoricalReplay and RunReverseStress handlers
on StressTestServicer.
"""
import grpc
import pytest
from concurrent import futures

pytestmark = pytest.mark.integration

from kinetix.common import types_pb2
from kinetix.risk import stress_testing_pb2, stress_testing_pb2_grpc
from kinetix_risk.stress_server import StressTestServicer


@pytest.fixture
def grpc_channel():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=2))
    stress_testing_pb2_grpc.add_StressTestServiceServicer_to_server(
        StressTestServicer(), server
    )
    port = server.add_insecure_port("[::]:0")
    server.start()
    channel = grpc.insecure_channel(f"localhost:{port}")
    yield channel
    server.stop(grace=None)
    channel.close()


@pytest.fixture
def stub(grpc_channel):
    return stress_testing_pb2_grpc.StressTestServiceStub(grpc_channel)


def _sample_positions():
    return [
        types_pb2.Position(
            book_id=types_pb2.BookId(value="port-1"),
            instrument_id=types_pb2.InstrumentId(value="AAPL"),
            asset_class=types_pb2.EQUITY,
            quantity=100.0,
            market_value=types_pb2.Money(amount="1000000.00", currency="USD"),
        ),
        types_pb2.Position(
            book_id=types_pb2.BookId(value="port-1"),
            instrument_id=types_pb2.InstrumentId(value="UST10Y"),
            asset_class=types_pb2.FIXED_INCOME,
            quantity=1.0,
            market_value=types_pb2.Money(amount="500000.00", currency="USD"),
        ),
    ]


class TestHistoricalReplayGrpc:
    def test_replay_with_provided_returns_succeeds(self, stub):
        """Replay returns a valid response when instrument returns are provided."""
        request = stress_testing_pb2.HistoricalReplayRequest(
            scenario_name="GFC_FIVE_DAY",
            positions=_sample_positions(),
            instrument_returns=[
                stress_testing_pb2.InstrumentDailyReturns(
                    instrument_id="AAPL",
                    daily_returns=[-0.05, -0.03, 0.01, -0.04, -0.02],
                ),
                stress_testing_pb2.InstrumentDailyReturns(
                    instrument_id="UST10Y",
                    daily_returns=[0.002, -0.001, 0.003, -0.002, 0.001],
                ),
            ],
            window_start="2008-09-15",
            window_end="2008-09-19",
        )
        response = stub.RunHistoricalReplay(request)
        assert response.scenario_name == "GFC_FIVE_DAY"
        assert response.total_pnl_impact < 0.0  # equity crash produces negative P&L
        assert len(response.position_impacts) == 2
        assert response.window_start == "2008-09-15"
        assert response.window_end == "2008-09-19"
        assert response.calculated_at.seconds > 0

    def test_replay_without_returns_uses_proxy(self, stub):
        """Instruments without returns receive proxy returns; proxy_used flag is set."""
        request = stress_testing_pb2.HistoricalReplayRequest(
            scenario_name="PROXY_SCENARIO",
            positions=_sample_positions(),
            instrument_returns=[],  # no returns provided
        )
        response = stub.RunHistoricalReplay(request)
        assert len(response.position_impacts) == 2
        for impact in response.position_impacts:
            assert impact.proxy_used is True

    def test_replay_mixed_proxy_and_actual(self, stub):
        """AAPL has actual returns, UST10Y falls back to proxy."""
        request = stress_testing_pb2.HistoricalReplayRequest(
            scenario_name="MIXED",
            positions=_sample_positions(),
            instrument_returns=[
                stress_testing_pb2.InstrumentDailyReturns(
                    instrument_id="AAPL",
                    daily_returns=[-0.01, -0.01, -0.01, -0.01, -0.01],
                ),
            ],
        )
        response = stub.RunHistoricalReplay(request)
        aapl = next(i for i in response.position_impacts if i.instrument_id == "AAPL")
        ust = next(i for i in response.position_impacts if i.instrument_id == "UST10Y")
        assert aapl.proxy_used is False
        assert ust.proxy_used is True


class TestReverseStressGrpc:
    def test_reverse_stress_finds_shock_for_target_loss(self, stub):
        """Solver converges for a reachable target loss."""
        request = stress_testing_pb2.ReverseStressRequest(
            positions=_sample_positions(),
            target_loss=100_000.0,
            max_shock=-1.0,
        )
        response = stub.RunReverseStress(request)
        assert response.converged is True
        assert len(response.shocks) == 2
        assert response.achieved_loss >= response.target_loss * 0.95
        assert response.calculated_at.seconds > 0

    def test_reverse_stress_shocks_are_negative(self, stub):
        """All returned shocks are price declines (<= 0)."""
        request = stress_testing_pb2.ReverseStressRequest(
            positions=_sample_positions(),
            target_loss=200_000.0,
        )
        response = stub.RunReverseStress(request)
        for shock in response.shocks:
            assert shock.shock <= 0.0

    def test_reverse_stress_infeasible_target_not_converged(self, stub):
        """Target loss exceeding portfolio value returns converged=False."""
        # Total portfolio = $1.5M, target 150% > max possible loss
        request = stress_testing_pb2.ReverseStressRequest(
            positions=_sample_positions(),
            target_loss=2_500_000.0,
            max_shock=-1.0,
        )
        response = stub.RunReverseStress(request)
        assert response.converged is False

    def test_reverse_stress_instrument_ids_present(self, stub):
        """Shock entries carry instrument_id values."""
        request = stress_testing_pb2.ReverseStressRequest(
            positions=_sample_positions(),
            target_loss=50_000.0,
        )
        response = stub.RunReverseStress(request)
        ids = {s.instrument_id for s in response.shocks}
        assert "AAPL" in ids
        assert "UST10Y" in ids
