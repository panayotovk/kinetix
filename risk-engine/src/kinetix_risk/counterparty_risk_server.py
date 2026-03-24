"""gRPC servicer for Counterparty Risk calculations (PFE and CVA).

Translates proto messages to domain calls in credit_exposure.py
and maps results back.  The servicer is stateless — all state lives
in the request or is derived from it.
"""
from __future__ import annotations

import logging
from datetime import datetime, timezone

import grpc
import numpy as np
from google.protobuf import timestamp_pb2

from kinetix.risk import counterparty_risk_pb2, counterparty_risk_pb2_grpc
from kinetix_risk.credit_exposure import (
    ExposureProfile,
    PositionExposure,
    calculate_cva,
    calculate_pfe,
)

logger = logging.getLogger(__name__)

_DEFAULT_NUM_SIMULATIONS = 10_000
_DEFAULT_RISK_FREE_RATE = 0.05


def _now_timestamp() -> timestamp_pb2.Timestamp:
    now = datetime.now(timezone.utc)
    ts = timestamp_pb2.Timestamp()
    ts.FromDatetime(now)
    return ts


def _proto_exposure_profile_to_domain(
    proto_profiles,
) -> list[ExposureProfile]:
    return [
        ExposureProfile(
            tenor=ep.tenor,
            tenor_years=ep.tenor_years,
            expected_exposure=ep.expected_exposure,
            pfe_95=ep.pfe_95,
            pfe_99=ep.pfe_99,
        )
        for ep in proto_profiles
    ]


class CounterpartyRiskServicer(
    counterparty_risk_pb2_grpc.CounterpartyRiskServiceServicer
):
    """gRPC servicer that computes PFE and CVA for counterparty credit risk."""

    def CalculatePFE(self, request, context):
        try:
            if not request.counterparty_id:
                raise ValueError("counterparty_id is required")
            if not request.netting_set_id:
                raise ValueError("netting_set_id is required")

            positions: list[PositionExposure] = [
                PositionExposure(
                    instrument_id=p.instrument_id,
                    market_value=p.market_value,
                    asset_class=p.asset_class or "EQUITY",
                    volatility=p.volatility if p.volatility > 0.0 else 0.20,
                    sector=p.sector or "OTHER",
                )
                for p in request.positions
            ]

            num_simulations = (
                request.num_simulations
                if request.num_simulations > 0
                else _DEFAULT_NUM_SIMULATIONS
            )
            seed = int(request.seed) if request.seed > 0 else 42

            # Rebuild correlation matrix from row-major flat list if supplied
            corr_flat = list(request.correlation_matrix)
            n = len(positions)
            corr: np.ndarray | None = None
            if corr_flat:
                expected_len = n * n
                if len(corr_flat) != expected_len:
                    raise ValueError(
                        f"correlation_matrix length must be {expected_len} for {n} positions"
                    )
                corr = np.array(corr_flat).reshape(n, n)

            result = calculate_pfe(
                counterparty_id=request.counterparty_id,
                netting_set_id=request.netting_set_id,
                agreement_type=request.agreement_type or "UNKNOWN",
                positions=positions,
                num_simulations=num_simulations,
                seed=seed,
                correlation_matrix=corr,
            )

            proto_profiles = [
                counterparty_risk_pb2.ExposureProfile(
                    tenor=ep.tenor,
                    tenor_years=ep.tenor_years,
                    expected_exposure=ep.expected_exposure,
                    pfe_95=ep.pfe_95,
                    pfe_99=ep.pfe_99,
                )
                for ep in result.exposure_profile
            ]

            return counterparty_risk_pb2.CalculatePFEResponse(
                counterparty_id=result.counterparty_id,
                netting_set_id=result.netting_set_id,
                gross_exposure=result.gross_exposure,
                net_exposure=result.net_exposure,
                exposure_profile=proto_profiles,
                calculated_at=_now_timestamp(),
            )

        except ValueError as e:
            context.abort(grpc.StatusCode.INVALID_ARGUMENT, str(e))
        except Exception as e:
            logger.exception("CalculatePFE failed")
            context.abort(grpc.StatusCode.INTERNAL, str(e))

    def CalculateCVA(self, request, context):
        try:
            if not request.counterparty_id:
                raise ValueError("counterparty_id is required")
            if request.lgd <= 0.0 or request.lgd > 1.0:
                raise ValueError(f"lgd must be in (0, 1], got {request.lgd}")

            exposure_profile = _proto_exposure_profile_to_domain(
                request.exposure_profile
            )

            # Resolve optional credit data fields — proto uses 0.0 as "not provided"
            pd_1y = request.pd_1y if request.pd_1y > 0.0 else None
            cds_spread_bps = (
                request.cds_spread_bps if request.cds_spread_bps > 0.0 else None
            )
            rating = request.rating if request.rating else None
            sector = request.sector if request.sector else None
            risk_free_rate = (
                request.risk_free_rate
                if request.risk_free_rate > 0.0
                else _DEFAULT_RISK_FREE_RATE
            )

            result = calculate_cva(
                counterparty_id=request.counterparty_id,
                exposure_profile=exposure_profile,
                lgd=request.lgd,
                pd_1y=pd_1y,
                cds_spread_bps=cds_spread_bps,
                rating=rating,
                sector=sector,
                risk_free_rate=risk_free_rate,
            )

            return counterparty_risk_pb2.CalculateCVAResponse(
                counterparty_id=result.counterparty_id,
                cva=result.cva,
                is_estimated=result.is_estimated,
                hazard_rate=result.hazard_rate,
                pd_1y=result.pd_1y,
                calculated_at=_now_timestamp(),
            )

        except ValueError as e:
            context.abort(grpc.StatusCode.INVALID_ARGUMENT, str(e))
        except Exception as e:
            logger.exception("CalculateCVA failed")
            context.abort(grpc.StatusCode.INTERNAL, str(e))
