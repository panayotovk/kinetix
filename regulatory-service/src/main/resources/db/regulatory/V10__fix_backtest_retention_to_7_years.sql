-- Fix: backtest results require 7-year retention for regulatory audit period (spec: regulatory.allium).
-- V6 incorrectly set this to 3 years based on the Basel 250-day backtest lookback window;
-- the correct requirement is the full 7-year regulatory audit retention period.
SELECT remove_retention_policy('backtest_results', if_exists => true);
SELECT add_retention_policy('backtest_results', INTERVAL '7 years');
