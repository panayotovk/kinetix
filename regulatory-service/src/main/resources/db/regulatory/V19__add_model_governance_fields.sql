ALTER TABLE model_versions
    ADD COLUMN model_tier            VARCHAR(50),
    ADD COLUMN validation_report_url VARCHAR(2048),
    ADD COLUMN known_limitations     TEXT,
    ADD COLUMN approved_use_cases    TEXT,
    ADD COLUMN next_validation_date  DATE;
