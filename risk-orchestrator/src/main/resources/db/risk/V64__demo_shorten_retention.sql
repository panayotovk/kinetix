-- Demo environment: shorten retention policies from production values to 90 days.
-- The 7-year valuation_jobs policy (V17) and 2-year daily_risk_snapshots policy
-- are appropriate for production but will exhaust disk on the Free Tier Oracle VM.

SELECT remove_retention_policy('valuation_jobs', if_exists => true);
SELECT add_retention_policy('valuation_jobs', INTERVAL '90 days');

-- Fix stale compress_segmentby from V17 (portfolio_id was renamed to book_id in V34).
ALTER TABLE valuation_jobs SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'book_id',
    timescaledb.compress_orderby = 'started_at DESC'
);

SELECT remove_retention_policy('daily_risk_snapshots', if_exists => true);
SELECT add_retention_policy('daily_risk_snapshots', INTERVAL '90 days');
