-- Replace two single-column indexes with one composite index that serves
-- the Trade Blotter query: WHERE portfolio_id = ? ORDER BY traded_at ASC.
-- The composite index satisfies portfolio_id-only predicates via the
-- left-prefix rule, making the two originals redundant.

DROP INDEX IF EXISTS idx_trade_events_portfolio;
DROP INDEX IF EXISTS idx_trade_events_traded_at;

CREATE INDEX idx_trade_events_portfolio_traded_at
    ON trade_events (portfolio_id, traded_at ASC);
