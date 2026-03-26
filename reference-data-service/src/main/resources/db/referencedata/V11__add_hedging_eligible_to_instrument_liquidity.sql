-- Marks whether an instrument is approved for use as a hedge leg.
-- Instruments with hedging_eligible = false are excluded from hedge candidate lists.
-- NULL means the flag has not been set (treated as eligible for backward compatibility).
ALTER TABLE instrument_liquidity
    ADD COLUMN IF NOT EXISTS hedging_eligible BOOLEAN DEFAULT NULL;
