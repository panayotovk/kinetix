-- Convert daily_risk_snapshots to a TimescaleDB hypertable for chunk exclusion
-- and efficient time-windowed queries.

-- Step 1: TimescaleDB requires the partition column in every unique constraint.
-- The UNIQUE(portfolio_id, snapshot_date, instrument_id) already includes snapshot_date.
-- But the standalone PRIMARY KEY(id) does not — reconstruct it.
ALTER TABLE daily_risk_snapshots DROP CONSTRAINT daily_risk_snapshots_pkey;
ALTER TABLE daily_risk_snapshots
    ADD CONSTRAINT pk_daily_risk_snapshots PRIMARY KEY (id, snapshot_date);

-- Step 2: Change snapshot_date from DATE to TIMESTAMPTZ for proper TimescaleDB
-- partitioning. DATE dimensions have limited chunk interval and retention support.
ALTER TABLE daily_risk_snapshots
    ALTER COLUMN snapshot_date TYPE TIMESTAMPTZ
    USING snapshot_date::TIMESTAMPTZ;

-- Step 3: Convert to hypertable. chunk_time_interval = 90 days (quarterly).
-- A 2-year retention window yields ~8 active chunks.
SELECT create_hypertable(
    'daily_risk_snapshots',
    'snapshot_date',
    chunk_time_interval => INTERVAL '90 days',
    migrate_data        => true
);

-- Step 3: Drop the old single-column index; replaced by the composite below.
DROP INDEX IF EXISTS idx_daily_risk_snapshots_date;

-- Step 4: Composite index for findByPortfolioId and findByPortfolioIdAndDate.
CREATE INDEX idx_daily_risk_snapshots_portfolio_date
    ON daily_risk_snapshots (portfolio_id, snapshot_date DESC);

-- Step 5: Retention policy — 2 years of daily snapshots.
SELECT add_retention_policy('daily_risk_snapshots', INTERVAL '2 years');
