-- V21: Run manifest tables for calculation reproducibility
-- Supports regulatory requirement to reconstruct any risk calculation

-- Step 1: Add manifest_id column to valuation_jobs
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

ALTER TABLE valuation_jobs ADD COLUMN IF NOT EXISTS manifest_id UUID NULL;

SELECT add_compression_policy('valuation_jobs', INTERVAL '90 days', if_not_exists => true);

-- Step 2: Create run_manifests table (standard PG table, not hypertable)
-- This is the "tag" that links a valuation job to all its inputs.
CREATE TABLE IF NOT EXISTS run_manifests (
    manifest_id       UUID         PRIMARY KEY,
    job_id            UUID         NOT NULL,
    portfolio_id      VARCHAR(255) NOT NULL,
    valuation_date    DATE         NOT NULL,
    captured_at       TIMESTAMPTZ  NOT NULL,
    model_version     VARCHAR(100) NOT NULL,
    calculation_type  VARCHAR(50)  NOT NULL,
    confidence_level  VARCHAR(10)  NOT NULL,
    time_horizon_days INTEGER      NOT NULL,
    num_simulations   INTEGER      NOT NULL,
    monte_carlo_seed  BIGINT       NOT NULL DEFAULT 0,
    position_count    INTEGER      NOT NULL,
    position_digest   CHAR(64)     NOT NULL,
    market_data_digest CHAR(64)    NOT NULL,
    input_digest      CHAR(64)     NOT NULL,
    status            VARCHAR(20)  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_run_manifests_job_id
    ON run_manifests (job_id);
CREATE INDEX IF NOT EXISTS idx_run_manifests_portfolio_date
    ON run_manifests (portfolio_id, valuation_date);

-- Step 3: Create run_position_snapshots table
-- Stores the full position state at calculation time.
CREATE TABLE IF NOT EXISTS run_position_snapshots (
    manifest_id          UUID           NOT NULL REFERENCES run_manifests(manifest_id),
    instrument_id        VARCHAR(255)   NOT NULL,
    asset_class          VARCHAR(32)    NOT NULL,
    quantity             NUMERIC(28,12) NOT NULL,
    avg_cost_amount      NUMERIC(28,12) NOT NULL,
    market_price_amount  NUMERIC(28,12) NOT NULL,
    currency             VARCHAR(3)     NOT NULL,
    market_value_amount  NUMERIC(28,12) NOT NULL,
    unrealized_pnl_amount NUMERIC(28,12) NOT NULL,
    CONSTRAINT pk_run_position_snapshots PRIMARY KEY (manifest_id, instrument_id)
);

-- Step 4: Create run_market_data_blobs table (content-addressable)
-- Market data is stored once per unique content; multiple manifests
-- reference the same blob via content_hash.
CREATE TABLE IF NOT EXISTS run_market_data_blobs (
    content_hash    CHAR(64)       NOT NULL,
    data_type       VARCHAR(50)    NOT NULL,
    instrument_id   VARCHAR(255)   NOT NULL,
    asset_class     VARCHAR(32)    NOT NULL,
    payload         JSONB          NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_run_market_data_blobs PRIMARY KEY (content_hash)
);

-- Step 5: Create run_manifest_market_data table (join table)
-- Links a manifest to each market data blob it used.
CREATE TABLE IF NOT EXISTS run_manifest_market_data (
    manifest_id     UUID           NOT NULL REFERENCES run_manifests(manifest_id),
    content_hash    CHAR(64)       NOT NULL,
    data_type       VARCHAR(50)    NOT NULL,
    instrument_id   VARCHAR(255)   NOT NULL,
    asset_class     VARCHAR(32)    NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    source_service  VARCHAR(50)    NOT NULL,
    sourced_at      TIMESTAMPTZ    NOT NULL,
    CONSTRAINT pk_run_manifest_market_data PRIMARY KEY (manifest_id, data_type, instrument_id)
);

-- Step 6: Rollback SQL is in V20-rollback.sql
