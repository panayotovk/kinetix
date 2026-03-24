-- Stores PFE and CVA calculation results per counterparty.
-- counterparty_exposure_history is a TimescaleDB hypertable partitioned by calculated_at.
-- This enables efficient historical queries and retention policies.
--
-- pfe_profile_json: JSON array of {tenor, exposure} for the PFE curve
-- netting_set_exposures_json: JSON array of per-netting-set PFE breakdowns
-- wrong_way_risk_flags_json: JSON array of wrong-way risk flags detected
-- cva: Credit Valuation Adjustment (in USD)
-- cva_estimated: true if CVA used sector-average CDS spread fallback

CREATE TABLE counterparty_exposure_history (
    id                          BIGSERIAL       NOT NULL,
    counterparty_id             VARCHAR(255)    NOT NULL,
    calculated_at               TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    pfe_profile_json            JSONB           NOT NULL DEFAULT '[]',
    netting_set_exposures_json  JSONB           NOT NULL DEFAULT '[]',
    wrong_way_risk_flags_json   JSONB           NOT NULL DEFAULT '[]',
    current_net_exposure        NUMERIC(24,6)   NOT NULL DEFAULT 0,
    peak_pfe                    NUMERIC(24,6)   NOT NULL DEFAULT 0,
    cva                         NUMERIC(24,6),
    cva_estimated               BOOLEAN         NOT NULL DEFAULT FALSE,
    currency                    VARCHAR(3)      NOT NULL DEFAULT 'USD',
    CONSTRAINT pk_counterparty_exposure_history PRIMARY KEY (id, calculated_at)
);

SELECT create_hypertable(
    'counterparty_exposure_history',
    'calculated_at',
    chunk_time_interval => INTERVAL '7 days',
    if_not_exists => TRUE
);

CREATE INDEX idx_ceh_counterparty_time
    ON counterparty_exposure_history (counterparty_id, calculated_at DESC);
