-- V59: Make triggered_by NOT NULL in valuation_jobs.
-- The domain model already treats this field as non-null; the DB column was
-- nullable by omission. Backfill any existing NULLs with 'system' before
-- applying the NOT NULL constraint.

UPDATE valuation_jobs
SET triggered_by = 'system'
WHERE triggered_by IS NULL;

ALTER TABLE valuation_jobs
    ALTER COLUMN triggered_by SET NOT NULL,
    ALTER COLUMN triggered_by SET DEFAULT 'system';
