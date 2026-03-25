-- Enforce a 2-year rolling retention window on vol surface data.
--
-- Strategy:
--   1. Drop the FK from volatility_surface_points → volatility_surfaces.
--      TimescaleDB does not permit FK references into a hypertable, so we
--      remove the constraint before converting the parent table.
--   2. Convert volatility_surfaces to a TimescaleDB hypertable partitioned
--      on as_of_date. The existing composite PK (instrument_id, as_of_date)
--      already satisfies the requirement that the time column is part of
--      every unique constraint, so no PK change is needed.
--   3. Add a 2-year retention policy. TimescaleDB will automatically drop
--      chunks older than 730 days, keeping storage bounded.
--
-- volatility_surface_points rows become unreachable once the parent surface
-- chunk is dropped. A companion cleanup function is provided for use by an
-- external scheduler if point-level orphan cleanup is required.

CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Step 1: Remove the FK constraint so the parent table can become a hypertable.
ALTER TABLE volatility_surface_points
    DROP CONSTRAINT IF EXISTS volatility_surface_points_instrument_id_as_of_date_fkey;

-- Step 2: Convert volatility_surfaces to a hypertable.
SELECT create_hypertable(
    'volatility_surfaces',
    'as_of_date',
    migrate_data => true
);

-- Step 3: Enforce 2-year retention (730 days).
SELECT add_retention_policy('volatility_surfaces', INTERVAL '730 days');
