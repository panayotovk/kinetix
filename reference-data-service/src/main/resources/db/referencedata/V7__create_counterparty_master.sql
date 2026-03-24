-- Counterparty master: legal entities we trade with.
-- Stores credit data (PD, LGD, ratings, CDS spread) for CVA and PFE calculations.
--
-- pd_1y:          Annual probability of default (null → use rating table).
-- lgd:            Loss Given Default (default 0.40).
-- cds_spread_bps: Most recent CDS spread in basis points (null → no CDS market).
-- rating_sp:      S&P rating string (e.g. 'BBB+').  null → unrated.
-- is_financial:   True for banks, broker-dealers, insurance companies.

CREATE TABLE counterparty_master (
    counterparty_id     VARCHAR(255)   NOT NULL,
    legal_name          VARCHAR(500)   NOT NULL,
    short_name          VARCHAR(100)   NOT NULL,
    lei                 VARCHAR(20),
    rating_sp           VARCHAR(10),
    rating_moodys       VARCHAR(10),
    rating_fitch        VARCHAR(10),
    sector              VARCHAR(100)   NOT NULL DEFAULT 'OTHER',
    country             VARCHAR(3),
    is_financial        BOOLEAN        NOT NULL DEFAULT FALSE,
    pd_1y               NUMERIC(12,8),
    lgd                 NUMERIC(8,6)   NOT NULL DEFAULT 0.400000,
    cds_spread_bps      NUMERIC(12,4),
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_counterparty_master PRIMARY KEY (counterparty_id)
);

CREATE INDEX idx_counterparty_master_sector  ON counterparty_master (sector);
CREATE INDEX idx_counterparty_master_rating  ON counterparty_master (rating_sp);
CREATE INDEX idx_counterparty_master_updated ON counterparty_master (updated_at DESC);

-- Netting agreements (ISDA, GMRA etc.) linking counterparties to close-out netting sets.
-- Trades belonging to a netting set can be netted on counterparty default.

CREATE TABLE netting_agreements (
    netting_set_id      VARCHAR(255)   NOT NULL,
    counterparty_id     VARCHAR(255)   NOT NULL REFERENCES counterparty_master(counterparty_id),
    agreement_type      VARCHAR(20)    NOT NULL DEFAULT 'ISDA_2002',
    close_out_netting   BOOLEAN        NOT NULL DEFAULT TRUE,
    csa_threshold       NUMERIC(24,6),
    currency            VARCHAR(3),
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_netting_agreements PRIMARY KEY (netting_set_id)
);

CREATE INDEX idx_netting_agreements_counterparty ON netting_agreements (counterparty_id);
