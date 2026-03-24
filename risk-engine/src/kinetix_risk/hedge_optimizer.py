"""Hedge optimiser gRPC servicer.

This module exposes the SuggestHedge RPC on RiskCalculationService. The current
implementation returns UNIMPLEMENTED — the active hedge suggestion logic lives in
the Kotlin AnalyticalHedgeCalculator. This stub exists to define the gRPC
contract and as the integration point for a future Python-side optimiser (e.g.
convex optimisation or ML-driven ranking).
"""
import logging

import grpc

from kinetix.risk import risk_calculation_pb2, risk_calculation_pb2_grpc

logger = logging.getLogger(__name__)


class HedgeOptimizerServicer(risk_calculation_pb2_grpc.RiskCalculationServiceServicer):
    """Stub servicer for the SuggestHedge RPC.

    When a Python-side optimiser is implemented, this class should override
    SuggestHedge with the actual computation. All other RPCs on
    RiskCalculationService are handled by RiskCalculationServicer in server.py.
    """

    def SuggestHedge(self, request: risk_calculation_pb2.SuggestHedgeRequest, context: grpc.ServicerContext) -> risk_calculation_pb2.SuggestHedgeResponse:
        """Placeholder: suggest hedge instruments for a given book and target.

        Currently returns UNIMPLEMENTED. The Kotlin AnalyticalHedgeCalculator
        in risk-orchestrator is the active implementation.

        Future implementation should:
          1. Parse candidates from request.candidates
          2. Solve the neutralisation problem for request.target_metric
          3. Apply request.max_notional and request.allowed_sides constraints
          4. Return ranked HedgeSuggestionProto entries
        """
        logger.warning(
            "SuggestHedge called for book %s target %s — stub not yet implemented, "
            "use Kotlin AnalyticalHedgeCalculator via risk-orchestrator",
            request.book_id,
            request.target_metric,
        )
        context.abort(
            grpc.StatusCode.UNIMPLEMENTED,
            "SuggestHedge is not yet implemented in the Python risk engine. "
            "Use the risk-orchestrator AnalyticalHedgeCalculator instead.",
        )
