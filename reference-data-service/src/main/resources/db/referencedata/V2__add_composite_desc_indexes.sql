-- Replace single-column instrument_id indexes with composite (instrument_id, as_of_date DESC).
-- This serves the findLatest pattern: WHERE instrument_id = ? ORDER BY as_of_date DESC LIMIT 1
-- Without DESC ordering the planner must scan all rows for the instrument then sort.
-- The left-prefix rule still covers instrument_id-only predicates.

DROP INDEX IF EXISTS idx_dividend_yields_instrument;
DROP INDEX IF EXISTS idx_credit_spreads_instrument;

CREATE INDEX idx_dividend_yields_instrument_date
    ON dividend_yields (instrument_id, as_of_date DESC);

CREATE INDEX idx_credit_spreads_instrument_date
    ON credit_spreads (instrument_id, as_of_date DESC);
