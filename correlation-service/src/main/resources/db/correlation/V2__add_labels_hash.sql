-- Add a generated MD5 hash column for efficient index-based label lookups.
-- Queries previously matched the full JSON text of labels; the hash reduces
-- index width and comparison cost.
ALTER TABLE correlation_matrices
    ADD COLUMN labels_hash VARCHAR(32) GENERATED ALWAYS AS (md5(labels)) STORED;

-- Replace the full-text index with a compact hash-based index.
DROP INDEX IF EXISTS idx_correlation_labels_window;
CREATE INDEX idx_correlation_hash_window ON correlation_matrices (labels_hash, window_days);
