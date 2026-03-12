CREATE EXTENSION IF NOT EXISTS timescaledb;

-- frtb_calculations: convert to hypertable for chunk-based retention.
-- TimescaleDB requires the time column in every unique constraint.
ALTER TABLE frtb_calculations DROP CONSTRAINT pk_frtb_calculations;
ALTER TABLE frtb_calculations
    ADD CONSTRAINT pk_frtb_calculations PRIMARY KEY (id, calculated_at);

SELECT create_hypertable(
    'frtb_calculations',
    'calculated_at',
    migrate_data => true
);

-- Enable compression before adding policy (required by TimescaleDB 3.x).
ALTER TABLE frtb_calculations SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'portfolio_id',
    timescaledb.compress_orderby = 'calculated_at DESC'
);

SELECT add_compression_policy('frtb_calculations', INTERVAL '90 days');

-- FRTB calculations are regulatory artifacts — 7 year retention to match
-- audit trail and Basel III lookback requirements.
SELECT add_retention_policy('frtb_calculations', INTERVAL '7 years');

-- backtest_results: same treatment.
ALTER TABLE backtest_results DROP CONSTRAINT pk_backtest_results;
ALTER TABLE backtest_results
    ADD CONSTRAINT pk_backtest_results PRIMARY KEY (id, calculated_at);

SELECT create_hypertable(
    'backtest_results',
    'calculated_at',
    migrate_data => true
);

ALTER TABLE backtest_results SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'portfolio_id',
    timescaledb.compress_orderby = 'calculated_at DESC'
);

SELECT add_compression_policy('backtest_results', INTERVAL '90 days');

-- Backtest results: 3 year retention covers the Basel 250-day lookback window
-- with margin for multi-year comparison.
SELECT add_retention_policy('backtest_results', INTERVAL '3 years');
