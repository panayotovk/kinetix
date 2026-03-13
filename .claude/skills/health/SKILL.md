---
name: health
description: Quick system health check — verify all services are running, Kafka consumers are caught up, databases are connected, and market data is fresh. Invoke with /health.
user-invocable: true
allowed-tools: Read, Glob, Grep, Bash, Task, WebFetch, WebSearch
---

# System Health Check

You are running a quick health check across all Kinetix components. Run all checks and report a clear status summary.

## Checks to run (in parallel where possible)

### 1. Service availability

```bash
# Docker Compose
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null

# Or Kubernetes
kubectl get pods -o wide 2>/dev/null
```

Verify all expected services are running:
- gateway
- position-service
- price-service
- risk-engine
- risk-orchestrator
- audit-service
- notification-service
- regulatory-service
- ui
- kafka
- postgresql / timescaledb
- redis
- keycloak

### 2. Health endpoints

```bash
# Check health endpoints for each service (adjust ports as needed)
for port in 8080 8081 8082 8083 8084 8085 8086 8087; do
  curl -s -o /dev/null -w "%{http_code} localhost:${port}" "http://localhost:${port}/health" 2>/dev/null &
done
wait
```

### 3. Kafka health

```bash
# List topics and consumer groups
docker exec -it $(docker ps -q --filter name=kafka) kafka-topics.sh --bootstrap-server localhost:9092 --list 2>/dev/null
docker exec -it $(docker ps -q --filter name=kafka) kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --all-groups 2>/dev/null
```

Check for:
- All expected topics exist (trades.lifecycle, price.updates, risk.results, etc.)
- Consumer lag is near zero for all groups
- No messages accumulating in DLQ topics

### 4. Database connectivity

```bash
# Check PostgreSQL / TimescaleDB
docker exec -it $(docker ps -q --filter name=postgres) psql -U kinetix -c "SELECT 1" 2>/dev/null || echo "DB check not available via Docker"
```

### 5. Redis health

```bash
docker exec -it $(docker ps -q --filter name=redis) redis-cli ping 2>/dev/null || echo "Redis check not available"
```

### 6. Recent errors

```bash
# Check recent logs for errors across services
for svc in gateway position-service price-service risk-engine risk-orchestrator audit-service notification-service regulatory-service; do
  echo "=== ${svc} ==="
  docker logs --tail 20 --since 5m $(docker ps -q --filter name=${svc}) 2>/dev/null | grep -i "error\|exception\|fatal" || echo "No recent errors"
done
```

### 7. Data freshness

Check when key data was last updated:
- Latest price update timestamp
- Latest risk calculation timestamp
- Latest trade event timestamp

## Report format

Present results as a dashboard:

```
KINETIX HEALTH CHECK — [timestamp]
====================================

SERVICE STATUS
  gateway              [UP/DOWN]
  position-service     [UP/DOWN]
  price-service        [UP/DOWN]
  risk-engine          [UP/DOWN]
  risk-orchestrator    [UP/DOWN]
  audit-service        [UP/DOWN]
  notification-service [UP/DOWN]
  regulatory-service   [UP/DOWN]
  ui                   [UP/DOWN]

INFRASTRUCTURE
  PostgreSQL           [UP/DOWN]
  Kafka                [UP/DOWN]  (lag: X msgs)
  Redis                [UP/DOWN]
  Keycloak             [UP/DOWN]

DATA FRESHNESS
  Last price update:   [timestamp or "STALE"]
  Last risk calc:      [timestamp or "STALE"]
  Last trade:          [timestamp or "N/A"]

ERRORS (last 5 min)
  [service]: [count] errors
  ...

OVERALL: [HEALTHY / DEGRADED / UNHEALTHY]
```

## Status definitions

- **HEALTHY** — all services up, no consumer lag, no recent errors, fresh data
- **DEGRADED** — some non-critical issues (e.g. stale data, minor lag, non-critical service down)
- **UNHEALTHY** — critical service down, significant consumer lag, or persistent errors

## Reminders

- If Docker/K8s commands fail, note that the runtime environment is not available and suggest alternatives (check Grafana, check logs manually)
- Report what you CAN check, even if some checks fail
- Be specific about staleness — "last price update was 3 hours ago" not just "stale"
- If everything is healthy, say so concisely — do not invent problems
