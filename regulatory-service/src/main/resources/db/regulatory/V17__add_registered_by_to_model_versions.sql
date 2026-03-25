ALTER TABLE model_versions
    ADD COLUMN registered_by VARCHAR(255) NOT NULL DEFAULT '';
