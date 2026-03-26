ALTER TABLE daily_risk_snapshots
    ADD COLUMN IF NOT EXISTS sod_vol  DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS sod_rate DOUBLE PRECISION;

COMMENT ON COLUMN daily_risk_snapshots.sod_vol  IS 'ATM implied volatility (1-month tenor) captured at start-of-day. Null when no vol surface exists for the instrument.';
COMMENT ON COLUMN daily_risk_snapshots.sod_rate IS 'Risk-free rate (1Y tenor) captured at start-of-day. Null when no rate data exists for the instrument currency.';
