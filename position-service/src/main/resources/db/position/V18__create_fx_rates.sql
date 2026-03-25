CREATE TABLE fx_rates (
    from_currency   CHAR(3)        NOT NULL,
    to_currency     CHAR(3)        NOT NULL,
    rate            NUMERIC(18, 8) NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_fx_rates PRIMARY KEY (from_currency, to_currency)
);
