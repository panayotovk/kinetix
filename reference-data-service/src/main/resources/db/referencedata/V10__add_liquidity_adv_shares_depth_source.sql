ALTER TABLE instrument_liquidity ADD COLUMN IF NOT EXISTS adv_shares NUMERIC(24,6) NULL;
ALTER TABLE instrument_liquidity ADD COLUMN IF NOT EXISTS market_depth_score NUMERIC(10,4) NULL;
ALTER TABLE instrument_liquidity ADD COLUMN IF NOT EXISTS source VARCHAR(50) NOT NULL DEFAULT 'unknown';
