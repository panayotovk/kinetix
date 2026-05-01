-- Renames TIER_1/TIER_2/TIER_3 labels in instrument_liquidity to canonical
-- HIGH_LIQUID/LIQUID/SEMI_LIQUID as defined in specs/core.allium:143 and
-- specs/liquidity.allium:210-214.
--
-- Semantic equivalence:
--   TIER_1   → HIGH_LIQUID  (adv >= 50M AND spread <= 5bps)
--   TIER_2   → LIQUID       (adv >= 10M AND spread <= 20bps)
--   TIER_3   → SEMI_LIQUID  (adv >= 1M)
--   ILLIQUID → ILLIQUID     (unchanged)
UPDATE instrument_liquidity SET liquidity_tier = 'HIGH_LIQUID' WHERE liquidity_tier = 'TIER_1';
UPDATE instrument_liquidity SET liquidity_tier = 'LIQUID'      WHERE liquidity_tier = 'TIER_2';
UPDATE instrument_liquidity SET liquidity_tier = 'SEMI_LIQUID' WHERE liquidity_tier = 'TIER_3';
ALTER TABLE instrument_liquidity ALTER COLUMN liquidity_tier SET DEFAULT 'ILLIQUID';
