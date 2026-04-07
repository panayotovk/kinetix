import math
from collections import defaultdict
from dataclasses import dataclass, field

import numpy as np

from kinetix_risk.market_data_models import VolSurface, VolSurfacePoint, YieldCurveData
from kinetix_risk.models import AssetClass
from kinetix_risk.volatility import VolatilityProvider


@dataclass(frozen=True)
class MarketDataBundle:
    volatility_provider: VolatilityProvider | None = None
    correlation_matrix: np.ndarray | None = None
    spot_prices: dict[str, float] = field(default_factory=dict)
    vol_surfaces: dict[str, VolSurface] = field(default_factory=dict)
    yield_curves: dict[str, YieldCurveData] = field(default_factory=dict)
    historical_returns: np.ndarray | None = None  # (n_observations, n_asset_classes) return matrix


_ASSET_CLASS_NAME_TO_DOMAIN = {ac.value: ac for ac in AssetClass}


def _log_returns_from_prices(prices: list[float]) -> np.ndarray | None:
    """Compute log returns from a price series, skipping zero/negative prices."""
    if len(prices) < 2:
        return None
    log_returns = []
    for i in range(1, len(prices)):
        if prices[i] > 0 and prices[i - 1] > 0:
            log_returns.append(math.log(prices[i] / prices[i - 1]))
    if len(log_returns) < 2:
        return None
    return np.array(log_returns)


def _annualized_vol_from_prices(prices: list[float]) -> float | None:
    if len(prices) < 2:
        return None
    log_returns = [
        math.log(prices[i] / prices[i - 1])
        for i in range(1, len(prices))
        if prices[i] > 0 and prices[i - 1] > 0
    ]
    if len(log_returns) < 2:
        return None
    return float(np.std(log_returns, ddof=1)) * math.sqrt(252)


def _build_historical_return_matrix(
    returns_by_ac: dict[AssetClass, list[np.ndarray]],
    vols_by_ac: dict[AssetClass, list[float]],
) -> np.ndarray | None:
    """Build an (N, M) return matrix for historical VaR.

    N = number of return observations (uses the shortest series length across asset classes).
    M = number of asset classes present.
    Each column is the average return across instruments in that asset class.
    Asset classes are sorted by value to match the order used in portfolio_risk.py.
    """
    if not returns_by_ac:
        return None

    asset_classes = sorted(returns_by_ac.keys(), key=lambda ac: ac.value)
    # Average returns per asset class, truncate to common length
    averaged: list[np.ndarray] = []
    for ac in asset_classes:
        series_list = returns_by_ac[ac]
        min_len = min(len(s) for s in series_list)
        if min_len < 2:
            return None
        truncated = np.column_stack([s[:min_len] for s in series_list])
        averaged.append(truncated.mean(axis=1))

    min_obs = min(len(a) for a in averaged)
    if min_obs < 2:
        return None

    matrix = np.column_stack([a[:min_obs] for a in averaged])
    return matrix


def _vol_surface_from_matrix(matrix_data: dict) -> list[VolSurfacePoint]:
    """Reconstruct VolSurfacePoints from a proto Matrix encoding.

    The matrix is row-major with rows=maturities, cols=strikes.
    Labels contain maturity strings followed by strike strings.
    """
    num_rows = matrix_data.get("rows", 0)
    num_cols = matrix_data.get("cols", 0)
    values = matrix_data.get("values", [])
    labels = matrix_data.get("labels", [])

    if num_rows == 0 or num_cols == 0 or len(values) != num_rows * num_cols:
        return []
    if len(labels) < num_rows + num_cols:
        return []

    maturities = labels[:num_rows]
    strikes = labels[num_rows : num_rows + num_cols]

    points = []
    for i, mat_str in enumerate(maturities):
        maturity_days = int(float(mat_str))
        for j, strike_str in enumerate(strikes):
            strike = float(strike_str)
            implied_vol = values[i * num_cols + j]
            points.append(VolSurfacePoint(
                strike=strike,
                maturity_days=maturity_days,
                implied_vol=implied_vol,
            ))
    return points


def consume_market_data(market_data: list[dict]) -> MarketDataBundle:
    if not market_data:
        return MarketDataBundle()

    spot_prices: dict[str, float] = {}
    vols_by_asset_class: dict[AssetClass, list[float]] = defaultdict(list)
    returns_by_asset_class: dict[AssetClass, list[np.ndarray]] = defaultdict(list)
    correlation_matrix: np.ndarray | None = None
    vol_surfaces: dict[str, VolSurface] = {}
    yield_curves: dict[str, YieldCurveData] = {}

    for item in market_data:
        data_type = item.get("data_type")

        if data_type == "SPOT_PRICE":
            instrument_id = item.get("instrument_id", "")
            scalar = item.get("scalar")
            if instrument_id and scalar is not None:
                spot_prices[instrument_id] = scalar

        elif data_type == "HISTORICAL_PRICES":
            asset_class_name = item.get("asset_class", "")
            ac = _ASSET_CLASS_NAME_TO_DOMAIN.get(asset_class_name)
            if ac is None:
                continue
            ts = item.get("time_series", [])
            prices = [pt["value"] for pt in ts]
            vol = _annualized_vol_from_prices(prices)
            if vol is not None:
                vols_by_asset_class[ac].append(vol)
            # Collect log returns per asset class for historical VaR
            log_rets = _log_returns_from_prices(prices)
            if log_rets is not None:
                returns_by_asset_class[ac].append(log_rets)

        elif data_type == "CORRELATION_MATRIX":
            matrix_data = item.get("matrix")
            if matrix_data:
                rows = matrix_data["rows"]
                cols = matrix_data["cols"]
                values = matrix_data["values"]
                correlation_matrix = np.array(values).reshape(rows, cols)

        elif data_type == "VOLATILITY_SURFACE":
            instrument_id = item.get("instrument_id", "")
            raw_points = item.get("points", [])
            matrix_data = item.get("matrix")
            if instrument_id and raw_points:
                points = [
                    VolSurfacePoint(
                        strike=pt["strike"],
                        maturity_days=pt["maturity_days"],
                        implied_vol=pt["implied_vol"],
                    )
                    for pt in raw_points
                ]
                vol_surfaces[instrument_id] = VolSurface(points=points)
            elif instrument_id and matrix_data:
                points = _vol_surface_from_matrix(matrix_data)
                if points:
                    vol_surfaces[instrument_id] = VolSurface(points=points)

        elif data_type == "YIELD_CURVE":
            currency = item.get("currency", "")
            raw_tenors = item.get("tenors", [])
            if currency and raw_tenors:
                tenors = [(t["days"], t["rate"]) for t in raw_tenors]
                yield_curves[currency] = YieldCurveData(tenors=tenors)

    volatility_provider = None
    if vols_by_asset_class:
        avg_vols = {
            ac: sum(vols) / len(vols)
            for ac, vols in vols_by_asset_class.items()
        }
        volatility_provider = VolatilityProvider.from_dict(avg_vols)

    # Build historical return matrix: average returns per asset class, then stack
    historical_returns = _build_historical_return_matrix(returns_by_asset_class, vols_by_asset_class)

    return MarketDataBundle(
        volatility_provider=volatility_provider,
        correlation_matrix=correlation_matrix,
        spot_prices=spot_prices,
        vol_surfaces=vol_surfaces,
        yield_curves=yield_curves,
        historical_returns=historical_returns,
    )
