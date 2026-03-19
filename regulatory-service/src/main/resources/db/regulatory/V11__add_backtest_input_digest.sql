-- Add input digest for backtest reproducibility.
-- backtest_results is a hypertable — decompress chunks before altering.
SELECT decompress_chunk(c) FROM show_chunks('backtest_results') c
WHERE is_compressed;

ALTER TABLE backtest_results
    ADD COLUMN IF NOT EXISTS input_digest  CHAR(64)     NULL,
    ADD COLUMN IF NOT EXISTS window_start  DATE         NULL,
    ADD COLUMN IF NOT EXISTS window_end    DATE         NULL,
    ADD COLUMN IF NOT EXISTS model_version VARCHAR(100) NULL;

CREATE INDEX IF NOT EXISTS idx_backtest_input_digest
    ON backtest_results (input_digest)
    WHERE input_digest IS NOT NULL;

SELECT compress_chunk(c) FROM show_chunks('backtest_results') c
WHERE NOT is_compressed
  AND range_end < NOW() - INTERVAL '90 days';
