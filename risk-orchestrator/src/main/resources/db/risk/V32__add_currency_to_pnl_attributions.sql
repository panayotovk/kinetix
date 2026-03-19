-- Add currency denomination to pnl_attributions so multi-currency P&L
-- values are not conflated in the daily aggregate.

-- Step 1: add the column to the base table.
ALTER TABLE pnl_attributions
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NULL;

UPDATE pnl_attributions SET currency = 'USD' WHERE currency IS NULL;

ALTER TABLE pnl_attributions
    ALTER COLUMN currency SET NOT NULL;
ALTER TABLE pnl_attributions
    ALTER COLUMN currency SET DEFAULT 'USD';

-- Step 2: recreate daily_pnl_summary with currency in GROUP BY.
-- Without currency in the GROUP BY, the SUM merges values from different
-- currency denominations into a single meaningless total.
SELECT remove_continuous_aggregate_policy('daily_pnl_summary');

DROP MATERIALIZED VIEW IF EXISTS daily_pnl_summary;

CREATE MATERIALIZED VIEW daily_pnl_summary
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 day', attribution_date) AS bucket,
    portfolio_id,
    currency,
    SUM(total_pnl)        AS total_pnl,
    SUM(delta_pnl)        AS total_delta_pnl,
    SUM(gamma_pnl)        AS total_gamma_pnl,
    SUM(vega_pnl)         AS total_vega_pnl,
    SUM(theta_pnl)        AS total_theta_pnl,
    SUM(rho_pnl)          AS total_rho_pnl,
    SUM(unexplained_pnl)  AS total_unexplained_pnl,
    COUNT(*)              AS attribution_count
FROM pnl_attributions
GROUP BY bucket, portfolio_id, currency
WITH NO DATA;

SELECT add_continuous_aggregate_policy('daily_pnl_summary',
    start_offset  => INTERVAL '3 days',
    end_offset    => INTERVAL '1 day',
    schedule_interval => INTERVAL '1 day'
);
