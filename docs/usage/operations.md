# Kinetix Operations / Middle Office Guide

A practical guide to the Kinetix platform from the operations and middle office perspective. This covers what you need to run a clean book every day: system health, data quality, reconciliation, EOD sign-off, audit verification, and incident response.

---

## At a Glance

| Surface | What operations uses it for |
|---------|----------------------------|
| **System tab** | Per-service health, Grafana links, connection status |
| **EOD History tab** | Promoting official EOD runs, timeline review, job drill-down |
| **Trades tab -- Reconciliation** | Prime broker break detection and resolution |
| **Alerts tab** | Operational alert triage and escalation |
| **Data Quality indicator (header)** | Market data freshness, staleness detection |
| **Audit trail (API)** | Hash chain integrity verification |
| **Grafana** | Deep-dive metrics, logs, traces, Kafka lag, database health |

The **Data Quality indicator** in the header is a traffic light (green/amber/red) that tells you at a glance whether the platform is pricing from fresh data. If it is amber or red, traders' numbers are not trustworthy. That is your first check every morning.

---

## 1. System Health Dashboard

The **System tab** is the single-pane-of-glass view of whether all services are alive.

### Service health grid

Each service has a card with a status dot (green = UP, red = DOWN), a Grafana link, and status text:

| Service | Port | What breaks if it goes DOWN |
|---------|------|----------------------------|
| Gateway | 8080 | All UI and API traffic stops |
| Position Service | 8081 | Trade booking and position queries fail |
| Price Service | 8082 | Price feed goes stale; VaR uses last known prices |
| Risk Orchestrator | 8083 | VaR, Greeks, stress tests stop calculating |
| Notification Service | 8086 | Alert rules stop firing; DLQ may accumulate |
| Rates Service | 8088 | Yield curve inputs go stale; bond/swap pricing degrades |
| Reference Data Service | 8089 | Instrument lookups fail; new trades may be rejected |
| Volatility Service | 8090 | Vol surface inputs go stale; options pricing degrades |
| Correlation Service | 8091 | Correlation matrix goes stale; portfolio VaR diversification benefit may be incorrect |

The overall banner reads **"All Systems Operational"** (green) or **"Degraded"** (yellow) when any service is down. A red dot on the System tab itself is visible from any screen.

The UI polls `GET /api/v1/system/health` every 30 seconds. Use the **Refresh** button to force an immediate check.

### Grafana dashboards

The observability links panel provides quick access to cross-cutting dashboards:

| Dashboard | What to look at |
|-----------|----------------|
| **System Health** | Request rate, error rate, P95 latency, JVM memory, GC pauses, Kafka lag |
| **Service Overview** | Per-service request rate, error counts, latency percentiles |
| **Risk Overview** | VaR gauge, ES, component breakdown trends |
| **Trade Flow** | Trade lifecycle, booking rate, amends and cancels |
| **Database Health** | Connection pools, query latency, table sizes |
| **Kafka Health** | Consumer lag per topic, partition health, throughput |
| **Service Logs** | Log volume, errors, warnings, full log lines via Loki |

The observability stack is Prometheus (metrics) + Loki (logs) + Tempo (distributed traces), all surfaced through Grafana.

### Prometheus alert rules

| Group | Rule | Threshold |
|-------|------|-----------|
| `kinetix-risk` | VaR breach | VaR > $1M |
| `kinetix-risk` | Calculation latency | P95 > 30s |
| `kinetix-price` | Price staleness | No update in 60s |
| `kinetix-kafka` | Consumer lag | Lag > 10,000 messages |

Wire these to PagerDuty or Slack. If Kafka lag is building, check the Kafka Health dashboard before it cascades into calculation delays.

---

## 2. Data Quality Monitoring

The **Data Quality indicator** sits in the header next to the market regime badge. Always visible regardless of tab. Click to expand the per-check detail panel.

### Traffic light states

| Colour | Meaning | Action |
|--------|---------|--------|
| Green | All checks passing | None |
| Amber | At least one WARNING check | Investigate and remediate before EOD |
| Red | At least one CRITICAL check | Immediate action; notify traders that numbers may be unreliable |

Refreshes every 30 seconds via `GET /api/v1/data-quality/status`.

### Per-check detail

Each check shows its own status (OK / WARNING / CRITICAL), a message, and a `lastChecked` timestamp:

- **Price freshness** -- last tick per instrument. If any instrument exceeds the staleness window, the check degrades.
- **Rates freshness** -- yield curve snapshots. Critical for bond and swap pricing.
- **Volatility freshness** -- vol surface snapshots. Critical for options Greeks.
- **Correlation freshness** -- correlation matrix snapshots. Stale correlation distorts portfolio VaR.

When a check is WARNING or CRITICAL, the message tells you which instrument or data type is affected. Chase the upstream feed or trigger a manual ingest via `POST /api/v1/prices/ingest`.

---

## 3. Prime Broker Reconciliation

The **Reconciliation sub-tab** lives inside the **Trades** tab. Select a book first.

### What it shows

Each reconciliation run appears as a dated card:

| Field | Meaning |
|-------|---------|
| Date | Reconciliation date |
| Status | **CLEAN** (green) or **BREAKS** (amber) |
| Matched count | Positions agreeing with PB statement |
| Break count | Positions with quantity mismatches |

### Break details

When breaks exist, the card expands to a table:

| Column | Meaning |
|--------|---------|
| Instrument | The instrument with a discrepancy |
| Internal Qty | Kinetix position quantity |
| PB Qty | Prime broker statement quantity |
| Break Qty | Difference (Internal minus PB) |
| Break Notional | Dollar value of the discrepancy |

Break rows are highlighted amber. Positive break = you are longer than the PB shows (settlement lag or booking error). Negative = PB has you longer. Both must be chased before cut-off.

### Workflow

1. Open Trades tab, select the book.
2. Navigate to Reconciliation sub-tab.
3. CLEAN = done. BREAKS = investigate each discrepancy against the Blotter sub-tab.
4. Book correcting entries or chase the PB.
5. Re-run reconciliation and confirm CLEAN.

---

## 4. EOD Sign-Off Workflow

The **EOD History** tab manages the daily close. Only completed calculations can be promoted, and the person who triggers the run cannot promote it (four-eyes). Requires `PROMOTE_EOD_RUN` permission (RISK_MANAGER and ADMIN roles).

### Run labels

| Label | Meaning |
|-------|---------|
| `ADHOC` | Ad-hoc, outside the official cycle |
| `SOD` | Start-of-day snapshot (06:00 UTC) |
| `INTRADAY` | Intraday recalculation |
| `PRE_CLOSE` | Pre-close check |
| `OFFICIAL_EOD` | Promoted, authoritative result |
| `SUPERSEDED_EOD` | Previously promoted, now replaced |

### Daily EOD process

1. **Pre-close** -- confirm the PRE_CLOSE run completed and VaR is within limits. Check Data Quality is green. If market data is stale, the risk team decides whether to reprice or accept last known values.

2. **Post-close** -- confirm the official EOD calculation has run or trigger one manually. Watch for COMPLETED status.

3. **Promote** -- locate today's completed run, click **Promote to Official EOD**. If you triggered the run, a different team member must promote. Promoting a second run for the same date supersedes the prior one.

4. **Verify** -- the promoted run shows `OFFICIAL_EOD` label with promoter and timestamp. An `OfficialEodPromotedEvent` is published to Kafka and an audit record is written.

5. **Demotion** -- if an input error is discovered, demote the current EOD, correct the input, rerun, and re-promote. Demotion is audited.

### EOD drill-down

Click any row in the EOD History grid to see:

- VaR, ES, and PV for that date
- Day-over-day VaR change (red = higher, green = lower)
- Calculation type and confidence level
- Greeks at that snapshot
- Full position risk table
- Side-by-side comparison with another date (optional)

The EOD trend chart above the grid plots VaR over the last 30 business days. Sudden spikes are worth investigating before promotion.

---

## 5. WebSocket Connectivity

Real-time price updates and intraday P&L flow via WebSocket (`/ws/prices`), maintained with 30-second ping/pong heartbeats. When the connection drops, the UI enters exponential backoff reconnect:

| Attempt | Delay |
|---------|-------|
| 1 | 1 second |
| 2 | 2 seconds |
| 3 | 4 seconds |
| ... | doubles each time (cap: 30s) |
| Max | 20 attempts |

A **"Reconnecting..."** banner appears. After 20 failures, a manual reconnect button appears.

### When the banner appears

1. Confirm the Gateway is UP on the System tab.
2. Check Grafana Service Logs for WebSocket errors from the gateway.
3. Check whether corporate proxies or load balancer idle timeouts are interrupting the connection.
4. If gateway is UP but WebSocket fails, check JVM memory on the System Health dashboard -- heap pressure can drop sessions.

Traders need to know they are without live marks. The `disconnectedSince` timestamp tells you how long.

---

## 6. Market Data Freshness

Stale market data is the most operationally dangerous condition. Stale prices mean stale VaR.

| Data type | Source service | Kafka topic | Alert threshold |
|-----------|---------------|-------------|-----------------|
| Prices | Price Service | `price.updates` | 60s without update |
| Yield curves | Rates Service | `rates.yield-curves` | Configured per check |
| Vol surfaces | Volatility Service | `volatility.surfaces` | Configured per check |
| Correlations | Correlation Service | `correlation.matrices` | Configured per check |
| Credit spreads | Reference Data Service | `reference-data.credit-spreads` | Configured per check |
| Dividend yields | Reference Data Service | `reference-data.dividends` | Configured per check |

### Checking the latest price

`GET /api/v1/prices/{instrumentId}/latest` -- returns the most recent price and its timestamp. If the gap exceeds expected feed frequency, the feed is stale.

### Forcing a manual ingest

If a feed is late and you have a reliable price:

```
POST /api/v1/prices/ingest
Body: { "instrumentId": "...", "priceAmount": "...", "priceCurrency": "..." }
```

Publishes a PriceEvent to `price.updates` which propagates to position-service and triggers risk recalculation.

### Consumer lag vs. actual staleness

Consumer lag on `price.updates` means prices are queued but not yet consumed. This looks like stale data even when the feed is live. Check the Kafka Health dashboard first -- if lag is building, the position-service is the bottleneck, not the feed itself.

---

## 7. Alert Management

The **Alerts tab** handles both trading threshold alerts and operational notifications.

### Alert types

| Type | Monitors |
|------|----------|
| `VAR_BREACH` | Portfolio VaR exceeds a limit |
| `PNL_THRESHOLD` | P&L crosses a threshold |
| `RISK_LIMIT` | General risk limit breach |

Severity: CRITICAL (red), WARNING (amber), INFO (blue). CRITICAL alerts also surface as inline banners at the top of every tab.

### Alert lifecycle

`TRIGGERED` -> `ACKNOWLEDGED` -> `ESCALATED` -> `RESOLVED`

Acknowledge promptly to stop escalation. Do not leave CRITICAL alerts in TRIGGERED state -- unacknowledged critical alerts are a compliance finding.

### Delivery channels

Rules deliver via `IN_APP`, `EMAIL`, or `WEBHOOK`. WEBHOOK integrates with Slack or PagerDuty. Operational data quality alerts should use WEBHOOK so they are visible even when nobody has the platform open.

### Exporting

Export alert history to CSV from the Alerts tab for regulatory record-keeping. This is the audit evidence that breaches were detected and managed within required timeframes.

---

## 8. Audit Trail Verification

The audit service maintains a **hash-chained trail** of all trade and governance events. Each record contains a SHA-256 hash of its content concatenated with the previous record's hash.

### What is audited

- Trade bookings, amendments, and cancellations (with userId, userRole, eventType)
- EOD promotion and demotion events
- Scenario governance actions
- Risk run events

### Verifying chain integrity

```
GET /api/v1/audit/verify

Response: { "valid": true, "eventCount": 47823 }
```

Processes the full chain in batches of 10,000 records. `valid: false` is a P1 incident -- escalate to information security immediately. Do not promote any EOD or regulatory submission until resolved.

### When to verify

| Trigger | Who |
|---------|-----|
| Post-deployment | Operations or engineering |
| Weekly scheduled | Operations |
| Pre-regulatory submission | Compliance |
| Suspicious activity | Information security |
| Pre-audit inspection | Compliance |

### Browsing events

```
GET /api/v1/audit/events?bookId={bookId}&limit=1000
GET /api/v1/audit/events?afterId={lastId}&limit=1000
```

Fields include tradeId, portfolioId, instrumentId, side, quantity, price, tradedAt, userId, userRole, eventType, receivedAt, recordHash, and previousHash.

---

## 9. Correlation ID Tracing

Every event carries a `correlationId` (UUID). When a trade is booked, the UUID propagates through the entire chain:

```
Trade booking (position-service) -> TradeEvent on trades.lifecycle
  -> Risk Orchestrator -> RiskResultEvent
  -> Notification Service -> alert evaluation
  -> Audit Service -> audit record
```

The same ID travels on HTTP requests as `X-Correlation-ID` and is logged via MDC in every service. To trace across all services in Grafana/Loki:

```
{service=~".+"} |= "correlationId=<UUID>"
```

### When correlation IDs matter

- **Alert did not fire** -- trace the risk result event to confirm it reached the notification service.
- **Audit record missing** -- trace the trade event to confirm the audit consumer received it. Consumer lag can delay records.
- **Trader questions a risk number** -- use the correlationId to find the RunManifest for that calculation.

### Run manifests

Every risk calculation captures a RunManifest recording exact inputs and outputs:

| Field | Purpose |
|-------|---------|
| `positionDigest` | SHA-256 of positions at calculation time |
| `marketDataDigest` | SHA-256 of all market data inputs |
| `inputDigest` | Combined hash of positions + market data + parameters |
| `modelVersion` | Risk engine version string |
| `monteCarloSeed` | Deterministic seed for MC runs |
| `outputDigest` | SHA-256 of the result |

Same inputs + same model + same seed = same output. Verifiable independently.

---

## 10. Kafka Consumer Health

Six consumers across four services. Healthy consumers run at near-zero lag.

### Consumer groups

| Consumer | Service | Topic | Impact if lagging |
|----------|---------|-------|------------------|
| position-service | Position Service | `trades.lifecycle` | Positions not updated |
| price-service | Price Service | `price.updates` | Price history not persisted |
| risk-orchestrator | Risk Orchestrator | `risk.results`, `risk.anomalies` | Risk processing delayed |
| audit-service | Audit Service | `risk.audit` | Audit records delayed |
| notification-service | Notification Service | `risk.results` | Alerts firing late |

### Dead-letter queues

All consumers use RetryableConsumer (3 retries, exponential backoff). After exhaustion, messages move to a DLQ:

| Source topic | DLQ |
|-------------|-----|
| `trades.lifecycle` | `trades.lifecycle.dlq` |
| `price.updates` | `price.updates.dlq` |
| `risk.results` | `risk.results.dlq` |
| `risk.anomalies` | `risk.anomalies.dlq` |

DLQ accumulation means messages are failing after all retries. Do not let a DLQ grow silently -- events are falling out of the processing chain. Investigate the root cause before replaying.

---

## 11. Infrastructure Reference

**Databases** -- each service has its own PostgreSQL/TimescaleDB instance. Retention: prices 2 years, valuation jobs 1 year, audit 7 years. Continuous aggregates pre-compute hourly VaR and daily P&L summaries.

**Caching** -- VaR results cached in Redis (300s TTL). If Redis is down, fallback to in-memory cache (not shared across replicas -- may see inconsistent results). Redis health visible in the Database Health dashboard.

**Scaling** -- Kubernetes HPAs use CPU, memory, and Kafka consumer lag. Consumer services scale out when lag exceeds 10,000 messages per replica.

**Deployment blackout** -- never deploy between 05:55 and 06:05 UTC. The SOD job triggers at 06:00. Deploying interrupts in-flight gRPC calls and leaves gaps in the continuous aggregate. Full sequence in `docs/runbooks/zero-downtime-deployment.md`.

---

## Daily Workflow

**Morning pre-open**

1. Check the **Data Quality indicator**. Amber or red = resolve before market open.
2. Open the **System tab**. All services must be UP.
3. Check Grafana **Kafka Health** for consumer lag. Zero is the target.
4. Confirm the SOD job ran (look for `SOD` label in EOD History). If missing, trigger manually.

**During the trading day**

5. Monitor **Alerts** for CRITICAL and WARNING alerts. Acknowledge promptly.
6. Watch for the WebSocket reconnect banner. Traders without live prices is a risk issue.
7. Check DLQ message counts on Kafka Health periodically.
8. Respond to trader queries using correlationId tracing in Grafana.

**End of day**

9. Confirm EOD calculation completed. Check Data Quality is green.
10. Promote Official EOD from **EOD History** tab (four-eyes).
11. Verify the audit chain: `GET /api/v1/audit/verify` -- confirm `"valid": true`.
12. **Trades > Reconciliation** must show CLEAN for all books.
13. Export alert history to CSV.

**Post-close**

14. Check Grafana **System Health** for error rate spikes or memory pressure.
15. Confirm the `hourly_var_summary` continuous aggregate has entries for the close hour.

---

## Quick Reference

| Task | Where |
|------|-------|
| Check service health | System tab |
| Check data quality | Header indicator (click to expand) |
| Reconciliation breaks | Trades tab > Reconciliation |
| Promote Official EOD | EOD History tab > Promote |
| Demote EOD | EOD History tab > Demote (RISK_MANAGER role) |
| Job drill-down | EOD History tab > click any row |
| Verify audit chain | `GET /api/v1/audit/verify` |
| Browse audit events | `GET /api/v1/audit/events?bookId=...` |
| Check consumer lag | Grafana > Kafka Health |
| Force price ingest | `POST /api/v1/prices/ingest` |
| Trace by correlationId | Grafana > Service Logs, filter by correlationId |
| Emergency rollback | `helm rollback kinetix --namespace kinetix` |
