-- Fix labels_hash generated column to produce compact JSON text (no spaces
-- after commas) so the MD5 matches the application-side hash computation.
-- PostgreSQL's JSONB::text adds spaces after commas and colons, which differs
-- from the compact JSON that kotlinx.serialization produces.
DROP INDEX IF EXISTS idx_correlation_hash_window;

ALTER TABLE correlation_matrices
    DROP COLUMN labels_hash;

ALTER TABLE correlation_matrices
    ADD COLUMN labels_hash VARCHAR(32) GENERATED ALWAYS AS (md5(replace(labels::text, ', ', ','))) STORED;

CREATE INDEX idx_correlation_hash_window ON correlation_matrices (labels_hash, window_days);
