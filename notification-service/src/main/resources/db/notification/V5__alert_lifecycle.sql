-- Alert lifecycle: status tracking, resolution, and contributors for drill-down.
ALTER TABLE alert_events ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'TRIGGERED';
ALTER TABLE alert_events ADD COLUMN resolved_at TIMESTAMPTZ;
ALTER TABLE alert_events ADD COLUMN resolved_reason TEXT;
ALTER TABLE alert_events ADD COLUMN contributors JSONB;
ALTER TABLE alert_events ADD COLUMN correlation_id VARCHAR(255);

-- Fix financial precision (audit correctness).
ALTER TABLE alert_events ALTER COLUMN current_value TYPE NUMERIC USING current_value::NUMERIC;
ALTER TABLE alert_events ALTER COLUMN threshold TYPE NUMERIC USING threshold::NUMERIC;
ALTER TABLE alert_rules ALTER COLUMN threshold TYPE NUMERIC USING threshold::NUMERIC;

-- Active alerts (the hot query).
CREATE INDEX idx_alert_events_active ON alert_events (triggered_at DESC) WHERE status = 'TRIGGERED';

-- Critical unresolved (escalation view).
CREATE INDEX idx_alert_events_critical_active ON alert_events (severity, triggered_at DESC) WHERE status = 'TRIGGERED';

-- Compression policy for older alert events.
ALTER TABLE alert_events SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'book_id',
    timescaledb.compress_orderby = 'triggered_at DESC'
);
SELECT add_compression_policy('alert_events', INTERVAL '30 days');
