ALTER TABLE stress_scenarios
    ALTER COLUMN shocks TYPE JSONB USING shocks::JSONB;
