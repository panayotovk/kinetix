-- V61: Persist factor returns — daily return observations per systematic
-- risk factor, previously passed as transient request data.
-- Composite PK (factor_name, as_of_date) enforces one return per factor per day.

CREATE TABLE factor_returns (
    factor_name  VARCHAR(64) NOT NULL,
    as_of_date   DATE        NOT NULL,
    return_value DOUBLE PRECISION NOT NULL,
    source       VARCHAR(64) NOT NULL,
    PRIMARY KEY (factor_name, as_of_date)
);

CREATE INDEX idx_factor_returns_factor_date
    ON factor_returns (factor_name, as_of_date);
