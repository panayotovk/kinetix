-- V35: Create intraday_pnl_snapshots hypertable for real-time P&L tracking.
-- 30-day retention, 1-hour chunk interval, compress after 1 day.

CREATE TABLE intraday_pnl_snapshots (
    id             BIGSERIAL,
    book_id        VARCHAR(64)     NOT NULL,
    snapshot_at    TIMESTAMPTZ     NOT NULL,
    base_currency  VARCHAR(3)      NOT NULL DEFAULT 'USD',
    trigger        VARCHAR(32)     NOT NULL,
    total_pnl      NUMERIC(20, 8)  NOT NULL,
    realised_pnl   NUMERIC(20, 8)  NOT NULL,
    unrealised_pnl NUMERIC(20, 8)  NOT NULL,
    delta_pnl      NUMERIC(20, 8)  NOT NULL DEFAULT 0,
    gamma_pnl      NUMERIC(20, 8)  NOT NULL DEFAULT 0,
    vega_pnl       NUMERIC(20, 8)  NOT NULL DEFAULT 0,
    theta_pnl      NUMERIC(20, 8)  NOT NULL DEFAULT 0,
    rho_pnl        NUMERIC(20, 8)  NOT NULL DEFAULT 0,
    unexplained_pnl NUMERIC(20, 8) NOT NULL DEFAULT 0,
    high_water_mark NUMERIC(20, 8) NOT NULL,
    correlation_id VARCHAR(128),
    PRIMARY KEY (id, snapshot_at)
);

-- Convert to TimescaleDB hypertable, 1-hour chunks for fine-grained retention.
SELECT create_hypertable(
    'intraday_pnl_snapshots',
    'snapshot_at',
    chunk_time_interval => INTERVAL '1 hour',
    migrate_data        => true
);

-- Composite index for the primary query pattern: book + time window.
CREATE INDEX idx_intraday_pnl_book_time
    ON intraday_pnl_snapshots (book_id, snapshot_at DESC);

-- Retention policy: 30 days.
SELECT add_retention_policy('intraday_pnl_snapshots', INTERVAL '30 days');

-- Compression policy: compress chunks older than 1 day.
ALTER TABLE intraday_pnl_snapshots
    SET (timescaledb.compress,
         timescaledb.compress_segmentby = 'book_id',
         timescaledb.compress_orderby   = 'snapshot_at DESC');

SELECT add_compression_policy('intraday_pnl_snapshots', INTERVAL '1 day');

-- 1-minute continuous aggregate for the intraday chart.
CREATE MATERIALIZED VIEW minute_pnl_summary
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 minute', snapshot_at) AS bucket,
    book_id,
    base_currency,
    AVG(total_pnl)      AS avg_total_pnl,
    MIN(total_pnl)      AS min_total_pnl,
    MAX(total_pnl)      AS max_total_pnl,
    MAX(high_water_mark) AS high_water_mark,
    AVG(realised_pnl)   AS avg_realised_pnl,
    AVG(unrealised_pnl) AS avg_unrealised_pnl,
    COUNT(*)            AS snapshot_count
FROM intraday_pnl_snapshots
GROUP BY bucket, book_id, base_currency
WITH NO DATA;

SELECT add_continuous_aggregate_policy('minute_pnl_summary',
    start_offset      => INTERVAL '2 hours',
    end_offset        => INTERVAL '30 seconds',
    schedule_interval => INTERVAL '30 seconds'
);
