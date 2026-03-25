-- Rebuild daily_pnl_summary continuous aggregate to include cross-Greek P&L columns.
--
-- TimescaleDB continuous aggregates cannot be altered with ADD COLUMN after creation;
-- they must be dropped and recreated. The refresh policy is also dropped automatically
-- when the view is dropped.
--
-- NOTE: This drops and recreates the materialized view. Historical aggregated data
-- will be lost and must be refreshed via:
--   CALL refresh_continuous_aggregate('daily_pnl_summary', NULL, NULL);

DROP MATERIALIZED VIEW IF EXISTS daily_pnl_summary;

CREATE MATERIALIZED VIEW daily_pnl_summary
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 day', attribution_date)  AS bucket,
    book_id,
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
GROUP BY bucket, book_id
WITH NO DATA;

-- Refresh policy: refresh daily, looking back 3 days for corrections.
SELECT add_continuous_aggregate_policy('daily_pnl_summary',
    start_offset      => INTERVAL '3 days',
    end_offset        => INTERVAL '1 day',
    schedule_interval => INTERVAL '1 day'
);
