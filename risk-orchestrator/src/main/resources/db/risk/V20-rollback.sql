-- V20 rollback: Remove run manifest tables
DROP TABLE IF EXISTS run_manifest_market_data;
DROP TABLE IF EXISTS run_market_data_blobs;
DROP TABLE IF EXISTS run_position_snapshots;
DROP TABLE IF EXISTS run_manifests;

-- Remove manifest_id column from valuation_jobs
SELECT remove_compression_policy('valuation_jobs', if_exists => true);

DO $$
DECLARE
    chunk RECORD;
BEGIN
    FOR chunk IN
        SELECT show_chunks.chunk_schema || '.' || show_chunks.chunk_name AS chunk_full_name
        FROM show_chunks('valuation_jobs')
        WHERE is_compressed
    LOOP
        EXECUTE format('SELECT decompress_chunk(%L)', chunk.chunk_full_name);
    END LOOP;
END $$;

ALTER TABLE valuation_jobs DROP COLUMN IF EXISTS manifest_id;

SELECT add_compression_policy('valuation_jobs', INTERVAL '90 days', if_not_exists => true);
