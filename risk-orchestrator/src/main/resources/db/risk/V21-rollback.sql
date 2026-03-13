-- V21 rollback: Remove run manifest tables
DROP TABLE IF EXISTS run_manifest_market_data;
DROP TABLE IF EXISTS run_market_data_blobs;
DROP TABLE IF EXISTS run_position_snapshots;
DROP TABLE IF EXISTS run_manifests;

-- Remove manifest_id column from valuation_jobs
SELECT remove_compression_policy('valuation_jobs', if_exists => true);

DO $$
DECLARE chunk REGCLASS;
BEGIN
    FOR chunk IN
        SELECT format('%I.%I', c.chunk_schema, c.chunk_name)::regclass
        FROM timescaledb_information.chunks c
        WHERE c.hypertable_name = 'valuation_jobs'
          AND c.is_compressed = true
    LOOP
        PERFORM decompress_chunk(chunk);
    END LOOP;
END $$;

ALTER TABLE valuation_jobs DROP COLUMN IF EXISTS manifest_id;

SELECT add_compression_policy('valuation_jobs', INTERVAL '90 days', if_not_exists => true);
