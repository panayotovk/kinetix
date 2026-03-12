-- Replace single-column instrument_id index with composite (instrument_id, as_of_date DESC).
-- This serves the findLatest pattern: WHERE instrument_id = ? ORDER BY as_of_date DESC LIMIT 1
-- The left-prefix rule still covers instrument_id-only predicates.

DROP INDEX IF EXISTS idx_vol_surfaces_instrument;

CREATE INDEX idx_vol_surfaces_instrument_date
    ON volatility_surfaces (instrument_id, as_of_date DESC);
