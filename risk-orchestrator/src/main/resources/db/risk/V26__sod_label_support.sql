-- Add partial index for SOD-labelled jobs (mirrors idx_valuation_jobs_official_eod from V19)
CREATE INDEX IF NOT EXISTS idx_valuation_jobs_sod
    ON valuation_jobs (portfolio_id, valuation_date)
    WHERE run_label = 'SOD';

-- Add VaR/ES columns to sod_baselines for fast lookups without job table joins
ALTER TABLE sod_baselines ADD COLUMN IF NOT EXISTS var_value DOUBLE PRECISION;
ALTER TABLE sod_baselines ADD COLUMN IF NOT EXISTS expected_shortfall DOUBLE PRECISION;
