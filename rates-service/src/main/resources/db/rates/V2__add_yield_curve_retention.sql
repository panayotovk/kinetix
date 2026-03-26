-- Enforce a 7-year rolling retention window on yield curve data (regulatory requirement).
--
-- Strategy:
--   1. Drop FK constraints from yield_curve_tenors → yield_curves and
--      forward_curve_points → forward_curves before converting the parent
--      tables to hypertables. TimescaleDB does not permit FK references into
--      a hypertable.
--   2. Convert yield_curves and forward_curves to hypertables partitioned on
--      as_of_date. risk_free_rates is promoted to a hypertable for consistent
--      retention management.
--   3. Add 7-year (2555 days) retention policies on all three time-series
--      tables to bound storage growth.
--
-- yield_curve_tenors and forward_curve_points rows become unreachable once
-- their parent chunk is dropped by TimescaleDB.

CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Step 1: Drop FK constraints that block hypertable conversion.
ALTER TABLE yield_curve_tenors
    DROP CONSTRAINT IF EXISTS fk_yield_curve_tenors_curve;

ALTER TABLE forward_curve_points
    DROP CONSTRAINT IF EXISTS fk_forward_curve_points_curve;

-- Step 2a: Convert yield_curves to a hypertable.
SELECT create_hypertable(
    'yield_curves',
    'as_of_date',
    migrate_data => true
);

-- Step 2b: Convert forward_curves to a hypertable.
SELECT create_hypertable(
    'forward_curves',
    'as_of_date',
    migrate_data => true
);

-- Step 2c: Convert risk_free_rates to a hypertable.
SELECT create_hypertable(
    'risk_free_rates',
    'as_of_date',
    migrate_data => true
);

-- Step 3: Enforce 7-year retention (2555 days) on all three hypertables.
SELECT add_retention_policy('yield_curves', INTERVAL '2555 days');
SELECT add_retention_policy('forward_curves', INTERVAL '2555 days');
SELECT add_retention_policy('risk_free_rates', INTERVAL '2555 days');
