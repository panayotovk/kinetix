ALTER TABLE model_versions
    ALTER COLUMN parameters TYPE JSONB USING parameters::JSONB;
