-- V62: Persist instrument factor loadings with staleness tracking.
-- One row per (instrument_id, factor_name) — upsert replaces on re-estimation.
-- estimation_date drives staleness checks; estimation_window records the
-- look-back period used (in trading days).

CREATE TABLE instrument_factor_loadings (
    instrument_id     VARCHAR(64)       NOT NULL,
    factor_name       VARCHAR(64)       NOT NULL,
    loading           DOUBLE PRECISION  NOT NULL,
    r_squared         DOUBLE PRECISION,
    method            VARCHAR(32)       NOT NULL,
    estimation_date   DATE              NOT NULL,
    estimation_window INTEGER           NOT NULL,
    PRIMARY KEY (instrument_id, factor_name)
);

CREATE INDEX idx_instrument_factor_loadings_estimation_date
    ON instrument_factor_loadings (estimation_date);
