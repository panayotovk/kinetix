-- Restore production retention values that V64 shortened for demo mode.
-- V64 reduced both policies to 90 days to conserve disk on the demo VM.
-- The compress_segmentby fix in V64 (portfolio_id -> book_id) is retained;
-- only the retention overrides are reverted here.
SELECT remove_retention_policy('valuation_jobs', if_exists => true);
SELECT add_retention_policy('valuation_jobs', INTERVAL '7 years');

SELECT remove_retention_policy('daily_risk_snapshots', if_exists => true);
SELECT add_retention_policy('daily_risk_snapshots', INTERVAL '2 years');
