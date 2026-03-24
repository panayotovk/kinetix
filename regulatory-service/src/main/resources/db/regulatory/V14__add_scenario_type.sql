ALTER TABLE stress_scenarios
    ADD COLUMN scenario_type VARCHAR(30) NOT NULL DEFAULT 'PARAMETRIC';

CREATE INDEX idx_stress_scenarios_type ON stress_scenarios (scenario_type);
