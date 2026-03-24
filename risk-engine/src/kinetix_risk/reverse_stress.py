"""
Reverse stress testing: find the minimum-norm shock vector that causes
portfolio losses >= a given target.

Uses scipy.optimize.minimize with SLSQP (Sequential Least Squares Programming).
Non-convergence is handled gracefully: returns converged=False rather than raising.

Mathematical formulation:
    min  || shocks ||^2
    s.t. -sum(shocks_i * market_value_i) >= target_loss
         max_shock <= shocks_i <= 0     for all i

The constraint is linear: loss = -dot(shocks, market_values).
Shocks are negative (price declines), so the portfolio loses money.
"""
from dataclasses import dataclass, field

import numpy as np
from scipy.optimize import minimize, OptimizeResult

from kinetix_risk.models import PositionRisk

_DEFAULT_MAX_SHOCK = -1.0   # shocks bounded in [-1, 0] by default
_SOLVER_MAX_ITER = 500


@dataclass
class ReverseStressRequest:
    """Input to a reverse stress calculation.

    target_loss: positive dollar amount the portfolio should lose.
    max_shock:   most negative shock allowed per instrument (default -100%).
    """
    positions: list[PositionRisk]
    target_loss: float
    max_shock: float = _DEFAULT_MAX_SHOCK


@dataclass
class ReverseStressResult:
    """Result of a reverse stress calculation.

    shocks:       shock per position (negative = price decline).
    instrument_ids: instrument_id for each shock entry, matching index order.
    achieved_loss:  actual loss produced by the shock vector.
    target_loss:    the requested target.
    converged:      False if optimizer did not find a feasible solution.
    """
    shocks: list[float]
    instrument_ids: list[str]
    achieved_loss: float
    target_loss: float
    converged: bool


def run_reverse_stress(request: ReverseStressRequest) -> ReverseStressResult:
    """Find the minimum-norm shock vector reaching the target loss.

    The loss function is linear in shocks:  L(s) = -sum(s_i * mv_i)
    We minimise ||s||^2 subject to L(s) >= target and max_shock <= s_i <= 0.
    """
    if not request.positions:
        raise ValueError("Cannot run reverse stress on empty positions list")
    if request.target_loss <= 0.0:
        raise ValueError("target_loss must be positive")

    market_values = np.array([p.market_value for p in request.positions])
    n = len(market_values)
    instrument_ids = [p.instrument_id for p in request.positions]

    # Check feasibility upper bound: maximum achievable loss with max_shock on all positions
    max_achievable = -request.max_shock * float(np.sum(market_values))
    if request.target_loss > max_achievable:
        return ReverseStressResult(
            shocks=[request.max_shock] * n,
            instrument_ids=instrument_ids,
            achieved_loss=max_achievable,
            target_loss=request.target_loss,
            converged=False,
        )

    # Initial guess: uniform shock that achieves exactly the target
    # -uniform * sum(mv) = target  =>  uniform = -target / sum(mv)
    uniform_shock = -request.target_loss / float(np.sum(market_values))
    x0 = np.full(n, uniform_shock)

    # Objective: minimise sum of squared shocks
    def objective(x: np.ndarray) -> float:
        return float(np.dot(x, x))

    def grad_objective(x: np.ndarray) -> np.ndarray:
        return 2.0 * x

    # Constraint: -dot(shocks, market_values) >= target_loss
    # => dot(shocks, market_values) <= -target_loss
    # In scipy form: ineq means fun(x) >= 0
    #   -dot(s, mv) - target >= 0
    constraints = [{
        "type": "ineq",
        "fun": lambda x: -float(np.dot(x, market_values)) - request.target_loss,
        "jac": lambda x: -market_values,
    }]

    # Bounds: max_shock <= s_i <= 0
    bounds = [(request.max_shock, 0.0)] * n

    try:
        opt_result: OptimizeResult = minimize(
            objective,
            x0,
            jac=grad_objective,
            method="SLSQP",
            bounds=bounds,
            constraints=constraints,
            options={"maxiter": _SOLVER_MAX_ITER, "ftol": 1e-9},
        )
    except Exception:
        return ReverseStressResult(
            shocks=[0.0] * n,
            instrument_ids=instrument_ids,
            achieved_loss=0.0,
            target_loss=request.target_loss,
            converged=False,
        )

    shocks = opt_result.x.tolist()
    achieved_loss = -float(np.dot(opt_result.x, market_values))

    return ReverseStressResult(
        shocks=shocks,
        instrument_ids=instrument_ids,
        achieved_loss=achieved_loss,
        target_loss=request.target_loss,
        converged=bool(opt_result.success),
    )
