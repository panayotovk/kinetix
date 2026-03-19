-- Standardize all identifier columns to VARCHAR(255) to prevent
-- constraint violations when an ID exceeds the narrower width.

-- daily_risk_snapshots is a TimescaleDB hypertable; decompress any
-- compressed chunks before altering the column type, then recompress.
-- Use DO block to handle cases where chunks may have been dropped by
-- retention policies.
DO $$
DECLARE
    chunk_rec RECORD;
BEGIN
    FOR chunk_rec IN
        SELECT c.chunk_name FROM timescaledb_information.chunks c
        WHERE c.hypertable_name = 'daily_risk_snapshots' AND c.is_compressed = true
    LOOP
        BEGIN
            PERFORM decompress_chunk(chunk_rec.chunk_name::regclass);
        EXCEPTION WHEN undefined_table THEN
            RAISE NOTICE 'Chunk % not found, skipping decompress', chunk_rec.chunk_name;
        END;
    END LOOP;
END $$;

ALTER TABLE daily_risk_snapshots
    ALTER COLUMN instrument_id TYPE VARCHAR(255);

-- Recompress any chunks that were decompressed above.
DO $$
DECLARE
    chunk_rec RECORD;
BEGIN
    FOR chunk_rec IN
        SELECT c.chunk_name FROM timescaledb_information.chunks c
        WHERE c.hypertable_name = 'daily_risk_snapshots' AND c.is_compressed = false
    LOOP
        BEGIN
            PERFORM compress_chunk(chunk_rec.chunk_name::regclass);
        EXCEPTION WHEN undefined_table THEN
            RAISE NOTICE 'Chunk % not found, skipping compress', chunk_rec.chunk_name;
        END;
    END LOOP;
END $$;

-- sod_baselines: portfolio_id VARCHAR(64) → VARCHAR(255)
ALTER TABLE sod_baselines
    ALTER COLUMN portfolio_id TYPE VARCHAR(255);
