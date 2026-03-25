CREATE MATERIALIZED VIEW IF NOT EXISTS daily_eod_completeness AS
SELECT
    j.valuation_date,
    COUNT(DISTINCT j.book_id)                                  AS books_with_jobs,
    COUNT(DISTINCT e.book_id)                                  AS books_promoted,
    COUNT(DISTINCT j.book_id) - COUNT(DISTINCT e.book_id)     AS books_pending
FROM valuation_jobs j
LEFT JOIN official_eod_designations e
    ON j.book_id = e.book_id AND j.valuation_date = e.valuation_date
WHERE j.status = 'COMPLETED'
GROUP BY j.valuation_date;

CREATE UNIQUE INDEX IF NOT EXISTS idx_daily_eod_completeness_date
    ON daily_eod_completeness (valuation_date);
