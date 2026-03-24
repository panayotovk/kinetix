package com.kinetix.position.model

enum class LimitType {
    POSITION,
    NOTIONAL,
    VAR,
    CONCENTRATION,
    ADV_CONCENTRATION,
    /**
     * Soft risk budget for VaR. Unlike VAR, exceeding VAR_BUDGET fires an alert
     * and triggers a risk committee review — it does NOT block trades.
     */
    VAR_BUDGET,
}
