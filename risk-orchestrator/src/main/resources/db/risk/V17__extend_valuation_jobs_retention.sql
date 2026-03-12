-- Regulatory requirement: risk calculation records must be available for 7 years.
-- Previous policy: 1 year (V8). Align with audit_events retention.
SELECT remove_retention_policy('valuation_jobs');
SELECT add_retention_policy('valuation_jobs', INTERVAL '7 years');

-- Enable compression settings before adding the compression policy.
-- TimescaleDB 3.x requires this step before add_compression_policy.
ALTER TABLE valuation_jobs SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'portfolio_id',
    timescaledb.compress_orderby = 'started_at DESC'
);

-- Add compression for cost management (chunks older than 90 days)
SELECT add_compression_policy('valuation_jobs', INTERVAL '90 days');
