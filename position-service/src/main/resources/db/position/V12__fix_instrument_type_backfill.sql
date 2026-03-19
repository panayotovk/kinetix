-- Fix incorrect DERIVATIVE → CASH_EQUITY backfill from V9.
UPDATE trade_events
SET instrument_type = 'UNKNOWN'
WHERE asset_class = 'DERIVATIVE'
  AND instrument_type = 'CASH_EQUITY';

UPDATE positions
SET instrument_type = 'UNKNOWN'
WHERE asset_class = 'DERIVATIVE'
  AND instrument_type = 'CASH_EQUITY';

-- Make column NOT NULL with DEFAULT for safety.
ALTER TABLE trade_events
    ALTER COLUMN instrument_type SET NOT NULL;
ALTER TABLE trade_events
    ALTER COLUMN instrument_type SET DEFAULT 'UNKNOWN';

ALTER TABLE positions
    ALTER COLUMN instrument_type SET NOT NULL;
ALTER TABLE positions
    ALTER COLUMN instrument_type SET DEFAULT 'UNKNOWN';
