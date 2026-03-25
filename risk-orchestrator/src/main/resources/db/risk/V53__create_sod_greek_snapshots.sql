-- Stores immutable per-instrument PRICING Greeks locked before market open.
--
-- These are analytical Black-Scholes sensitivities computed at SOD, not the
-- VaR sensitivities stored in daily_risk_snapshots. The distinction matters:
-- VaR Greeks are bump-and-reprice for risk aggregation; pricing Greeks are
-- the closed-form BS partials used for P&L attribution.
--
-- Once locked (is_locked = true), the snapshot is immutable for the trading day.
-- The lock is set immediately after the SOD snapshot job completes.

CREATE TABLE sod_greek_snapshots (
    id                BIGSERIAL PRIMARY KEY,
    book_id           VARCHAR(64)         NOT NULL,
    snapshot_date     DATE                NOT NULL,
    instrument_id     VARCHAR(64)         NOT NULL,
    -- SOD market state used to compute the Greeks
    sod_price         DECIMAL(20, 8)      NOT NULL,
    sod_vol           DOUBLE PRECISION,
    sod_rate          DOUBLE PRECISION,
    -- First-order pricing Greeks (analytical BS for options, DV01 for fixed income)
    delta             DOUBLE PRECISION,
    gamma             DOUBLE PRECISION,
    vega              DOUBLE PRECISION,
    theta             DOUBLE PRECISION,
    rho               DOUBLE PRECISION,
    -- Cross-Greeks (second-order mixed sensitivities, analytical BS)
    vanna             DOUBLE PRECISION,
    volga             DOUBLE PRECISION,
    charm             DOUBLE PRECISION,
    -- Fixed-income sensitivities (null for non-fixed-income instruments)
    bond_dv01         DOUBLE PRECISION,
    swap_dv01         DOUBLE PRECISION,
    -- Immutability lock: set to true immediately after SOD job completes.
    -- Rows with is_locked = true must never be updated.
    is_locked         BOOLEAN             NOT NULL DEFAULT FALSE,
    locked_at         TIMESTAMPTZ,
    locked_by         VARCHAR(128),
    created_at        TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    UNIQUE (book_id, snapshot_date, instrument_id)
);

CREATE INDEX idx_sod_greek_snapshots_book_date
    ON sod_greek_snapshots (book_id, snapshot_date);

-- Link from sod_baselines so the attribution service can reach the Greek snapshot
-- without a full table scan on sod_greek_snapshots.
ALTER TABLE sod_baselines
    ADD COLUMN IF NOT EXISTS greek_snapshot_id BIGINT REFERENCES sod_greek_snapshots(id);
