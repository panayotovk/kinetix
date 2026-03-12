CREATE EXTENSION IF NOT EXISTS timescaledb;

-- alert_events: convert to hypertable for time-based retention.
-- TimescaleDB requires the time column in every unique constraint.
ALTER TABLE alert_events DROP CONSTRAINT pk_alert_events;
ALTER TABLE alert_events
    ADD CONSTRAINT pk_alert_events PRIMARY KEY (id, triggered_at);

SELECT create_hypertable(
    'alert_events',
    'triggered_at',
    migrate_data => true
);

-- Alert events are operational data. 1 year of history is sufficient for
-- pattern analysis; older alerts are covered by the audit trail.
SELECT add_retention_policy('alert_events', INTERVAL '1 year');
