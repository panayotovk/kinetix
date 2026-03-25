-- Add cross-Greek P&L attribution columns to pnl_attributions.
-- These capture second-order mixed sensitivities: vanna (dS*dvol), volga (dvol^2),
-- charm (dS*dT), and cross_gamma (multi-asset, always zero for single-asset books).
-- data_quality_flag records whether the attribution used full pricing Greeks
-- (FULL_ATTRIBUTION), first-order only (PRICE_ONLY), or stale Greeks (STALE_GREEKS).

ALTER TABLE pnl_attributions
    ADD COLUMN IF NOT EXISTS vanna_pnl       DECIMAL(20, 8) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS volga_pnl       DECIMAL(20, 8) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS charm_pnl       DECIMAL(20, 8) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS cross_gamma_pnl DECIMAL(20, 8) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS data_quality_flag VARCHAR(32)  NOT NULL DEFAULT 'PRICE_ONLY';

-- Add cross-Greek P&L columns to intraday_pnl_snapshots so that intraday
-- attribution is consistent with the daily EOD attribution.

ALTER TABLE intraday_pnl_snapshots
    ADD COLUMN IF NOT EXISTS vanna_pnl       DECIMAL(20, 8) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS volga_pnl       DECIMAL(20, 8) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS charm_pnl       DECIMAL(20, 8) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS cross_gamma_pnl DECIMAL(20, 8) NOT NULL DEFAULT 0;
