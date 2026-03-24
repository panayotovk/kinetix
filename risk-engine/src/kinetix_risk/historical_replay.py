"""
Historical scenario replay: applies actual historical returns to current positions.

Instruments without historical data fall back to asset-class proxy returns and are
flagged with proxy_used=True in the result.
"""
from dataclasses import dataclass, field

import numpy as np

from kinetix_risk.models import AssetClass, PositionRisk

# ---------------------------------------------------------------------------
# Proxy return series (one representative crisis week per asset class)
# These are the 5-day synthetic returns used when instrument-level history is absent.
# ---------------------------------------------------------------------------

ASSET_CLASS_PROXY_RETURNS: dict[AssetClass, np.ndarray] = {
    AssetClass.EQUITY: np.array([-0.035, -0.055, 0.010, -0.025, -0.020]),
    AssetClass.FIXED_INCOME: np.array([0.005, -0.003, -0.002, 0.001, -0.004]),
    AssetClass.FX: np.array([-0.015, -0.020, 0.005, -0.010, -0.005]),
    AssetClass.COMMODITY: np.array([-0.040, -0.030, 0.008, -0.015, -0.025]),
    AssetClass.DERIVATIVE: np.array([-0.050, -0.060, 0.015, -0.030, -0.035]),
}


# ---------------------------------------------------------------------------
# Domain models
# ---------------------------------------------------------------------------

@dataclass
class HistoricalReplayRequest:
    """Input to a historical replay run.

    instrument_returns maps instrument_id -> daily return array (length N).
    All arrays must have the same length N.  Instruments not present in
    instrument_returns receive proxy returns from ASSET_CLASS_PROXY_RETURNS.
    """
    scenario_name: str
    positions: list[PositionRisk]
    instrument_returns: dict[str, np.ndarray]
    window_start: str | None = None
    window_end: str | None = None


@dataclass
class PositionReplayImpact:
    """Per-position result of a historical replay."""
    instrument_id: str
    asset_class: AssetClass
    market_value: float
    pnl_impact: float
    daily_pnl: list[float]
    proxy_used: bool


@dataclass
class HistoricalReplayResult:
    """Aggregate result of a historical replay."""
    scenario_name: str
    total_pnl_impact: float
    position_impacts: list[PositionReplayImpact]
    window_start: str | None = None
    window_end: str | None = None


# ---------------------------------------------------------------------------
# Core calculation
# ---------------------------------------------------------------------------

def run_historical_replay(request: HistoricalReplayRequest) -> HistoricalReplayResult:
    """Apply historical (or proxy) daily returns to current positions.

    The approach is deliberately simple: for each position we sum
    daily_return_i * market_value across the return window.  This gives
    approximate P&L under the assumption that the position is held static
    across the scenario window — consistent with stress testing semantics
    ("what would this current book have lost during X event?").
    """
    if not request.positions:
        raise ValueError("Cannot run historical replay on empty positions list")

    position_impacts: list[PositionReplayImpact] = []

    for position in request.positions:
        raw_returns = request.instrument_returns.get(position.instrument_id)

        if raw_returns is not None:
            returns = raw_returns
            proxy_used = False
        else:
            returns = ASSET_CLASS_PROXY_RETURNS[position.asset_class]
            proxy_used = True

        daily_pnl = [float(r) * position.market_value for r in returns]
        total_pnl = sum(daily_pnl)

        position_impacts.append(PositionReplayImpact(
            instrument_id=position.instrument_id,
            asset_class=position.asset_class,
            market_value=position.market_value,
            pnl_impact=total_pnl,
            daily_pnl=daily_pnl,
            proxy_used=proxy_used,
        ))

    total_pnl = sum(p.pnl_impact for p in position_impacts)

    return HistoricalReplayResult(
        scenario_name=request.scenario_name,
        total_pnl_impact=total_pnl,
        position_impacts=position_impacts,
        window_start=request.window_start,
        window_end=request.window_end,
    )
