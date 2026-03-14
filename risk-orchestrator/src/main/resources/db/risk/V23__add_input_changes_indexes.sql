-- Index for cross-manifest market data hash comparison
CREATE INDEX IF NOT EXISTS idx_rmmd_data_type_instrument_hash
    ON run_manifest_market_data (data_type, instrument_id, content_hash);

-- Index for blob store lookup by data type and instrument
CREATE INDEX IF NOT EXISTS idx_rmdb_data_type_instrument
    ON run_market_data_blobs (data_type, instrument_id);

-- Index for reverse lookup from manifest to job
CREATE INDEX IF NOT EXISTS idx_valuation_jobs_manifest_id
    ON valuation_jobs (manifest_id)
    WHERE manifest_id IS NOT NULL;
