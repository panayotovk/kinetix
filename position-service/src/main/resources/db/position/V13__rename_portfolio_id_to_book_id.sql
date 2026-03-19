-- Rename portfolio_id to book_id in trade_events and positions tables.
-- Also replaces the composite index which referenced the old column name.

ALTER TABLE trade_events RENAME COLUMN portfolio_id TO book_id;

DROP INDEX IF EXISTS idx_trade_events_portfolio;
DROP INDEX IF EXISTS idx_trade_events_portfolio_traded_at;

CREATE INDEX idx_trade_events_book ON trade_events (book_id);
CREATE INDEX idx_trade_events_book_traded_at ON trade_events (book_id, traded_at ASC);

ALTER TABLE positions RENAME COLUMN portfolio_id TO book_id;
