-- Restore currency to the daily_pnl_summary GROUP BY.
--
-- V34 (portfolio_id -> book_id rename) dropped and recreated daily_pnl_summary
-- without the currency column that V32 had added. V54 (cross-Greek rebuild)
-- preserved the omission. Without currency in the GROUP BY, P&L values from
-- different currency denominations are summed into a single meaningless total.
--
-- NOTE: Dropping and recreating the view loses the cached materialized data.
-- After deploying, refresh with:
--   CALL refresh_continuous_aggregate('daily_pnl_summary', NULL, NULL);

DROP MATERIALIZED VIEW IF EXISTS daily_pnl_summary;

CREATE MATERIALIZED VIEW daily_pnl_summary
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 day', attribution_date)  AS bucket,
    book_id,
    currency,
    SUM(total_pnl)                          AS total_pnl,
    SUM(delta_pnl)                          AS total_delta_pnl,
    SUM(gamma_pnl)                          AS total_gamma_pnl,
    SUM(vega_pnl)                           AS total_vega_pnl,
    SUM(theta_pnl)                          AS total_theta_pnl,
    SUM(rho_pnl)                            AS total_rho_pnl,
    SUM(vanna_pnl)                          AS total_vanna_pnl,
    SUM(volga_pnl)                          AS total_volga_pnl,
    SUM(charm_pnl)                          AS total_charm_pnl,
    SUM(cross_gamma_pnl)                    AS total_cross_gamma_pnl,
    SUM(unexplained_pnl)                    AS total_unexplained_pnl,
    COUNT(*)                                AS attribution_count
FROM pnl_attributions
GROUP BY bucket, book_id, currency
WITH NO DATA;

-- Refresh policy: refresh daily, looking back 3 days for corrections.
SELECT add_continuous_aggregate_policy('daily_pnl_summary',
    start_offset      => INTERVAL '3 days',
    end_offset        => INTERVAL '1 day',
    schedule_interval => INTERVAL '1 day'
);
