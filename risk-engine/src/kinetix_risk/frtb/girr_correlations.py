"""
FRTB GIRR intra-bucket correlations per BCBS 352 Table 4.

Formula: rho(T_k, T_l) = max(exp(-theta * |T_k - T_l| / min(T_k, T_l)), 0.40)
where theta = 0.03.
"""
import math

import numpy as np

# Standard GIRR tenor grid (years) from BCBS 352 Table 1
GIRR_TENOR_YEARS: list[float] = [0.25, 0.5, 1.0, 2.0, 3.0, 5.0, 10.0, 15.0, 20.0, 30.0]

_THETA = 0.03
_CORRELATION_FLOOR = 0.40


def intra_bucket_correlation(tenor_k_years: float, tenor_l_years: float) -> float:
    """
    Compute BCBS 352 Table 4 GIRR intra-bucket correlation between two tenors.

    rho = max(exp(-theta * |T_k - T_l| / min(T_k, T_l)), floor)
    """
    if tenor_k_years == tenor_l_years:
        return 1.0
    ratio = abs(tenor_k_years - tenor_l_years) / min(tenor_k_years, tenor_l_years)
    raw = math.exp(-_THETA * ratio)
    return max(raw, _CORRELATION_FLOOR)


def _build_correlation_matrix() -> np.ndarray:
    n = len(GIRR_TENOR_YEARS)
    matrix = np.zeros((n, n))
    for i, t_i in enumerate(GIRR_TENOR_YEARS):
        for j, t_j in enumerate(GIRR_TENOR_YEARS):
            matrix[i, j] = intra_bucket_correlation(t_i, t_j)
    return matrix


# Pre-computed 10×10 correlation matrix for the standard tenor grid.
# Row/column i corresponds to GIRR_TENOR_YEARS[i].
STANDARD_CORRELATION_MATRIX: np.ndarray = _build_correlation_matrix()
