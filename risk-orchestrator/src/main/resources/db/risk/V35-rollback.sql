-- V35 Rollback: Remove intraday_pnl_snapshots hypertable and minute_pnl_summary aggregate
-- WARNING: This is NOT Flyway-managed. Execute manually in a maintenance window.
-- Data loss: All intraday_pnl_snapshots rows are permanently deleted.
-- Dependencies: V47, V52 add columns to this table — their rollbacks must run first.

-- Step 1: Drop continuous aggregate policy and view
SELECT remove_continuous_aggregate_policy('minute_pnl_summary', if_exists => true);
DROP MATERIALIZED VIEW IF EXISTS minute_pnl_summary CASCADE;

-- Step 2: Suspend compression before dropping hypertable
SELECT remove_compression_policy('intraday_pnl_snapshots', if_exists => true);

-- Step 3: Decompress all chunks (required before DROP on some TimescaleDB versions)
DO $$
DECLARE
    chunk RECORD;
BEGIN
    FOR chunk IN
        SELECT show_chunks.chunk_schema || '.' || show_chunks.chunk_name AS chunk_full_name
        FROM show_chunks('intraday_pnl_snapshots')
        WHERE is_compressed
    LOOP
        EXECUTE format('SELECT decompress_chunk(%L)', chunk.chunk_full_name);
    END LOOP;
END $$;

-- Step 4: Remove retention policy
SELECT remove_retention_policy('intraday_pnl_snapshots', if_exists => true);

-- Step 5: Drop the hypertable (removes all data and indexes)
DROP TABLE IF EXISTS intraday_pnl_snapshots CASCADE;

-- Step 6: Remove Flyway migration records (V35, V47, V52 all depend on this table)
DELETE FROM flyway_schema_history WHERE version IN ('35', '47', '52');
