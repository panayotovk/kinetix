---
name: incident
description: Structured incident triage — check service health, Kafka consumers, database connections, recent deployments, and error rates. Invoke with /incident optionally followed by a symptom description.
user-invocable: true
allowed-tools: Read, Glob, Grep, Bash, Task, WebFetch, WebSearch
---

# Incident Response

You are running a structured incident triage for Kinetix. Follow this diagnostic process systematically.

## Step 1 — Establish the symptom

If the user described a symptom, start there. Otherwise ask: "What is the observed behaviour and when did it start?"

Common symptom categories:
- **Stale data** — prices, risk numbers, or positions not updating
- **Calculation errors** — incorrect VaR, Greeks, P&L
- **Service unavailable** — UI errors, API timeouts, connection refused
- **Performance degradation** — slow responses, high latency
- **Data inconsistency** — different numbers in different views

## Step 2 — Check system vital signs

Run these diagnostics in parallel where possible:

### Service health
```bash
# Check if services are running (Docker/K8s context)
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || kubectl get pods -o wide 2>/dev/null
```

### Recent changes
```bash
# Recent deployments or code changes
git log --oneline -10 --since="24 hours ago"
```

### Application logs — check for errors
```bash
# Check recent logs for ERROR level (adjust paths for your setup)
docker logs --tail 50 --since 10m $(docker ps -q --filter name=risk) 2>/dev/null | grep -i error || echo "No container logs available — check Grafana/Loki"
```

### Kafka consumer health
```bash
# Check consumer lag if kafka tools available
docker exec -it $(docker ps -q --filter name=kafka) kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --all-groups 2>/dev/null || echo "Kafka CLI not available — check Grafana Kafka dashboard"
```

## Step 3 — Follow the data path

Based on the symptom, trace the data flow:

| Symptom | Investigation path |
|---------|-------------------|
| Stale prices | price-service → Kafka `price.updates` → consumers (risk-orchestrator, UI) |
| Wrong VaR | risk-orchestrator → positions fetch → price fetch → gRPC to risk-engine → `risk.results` |
| Missing trades | gateway → position-service → Kafka `trades.lifecycle` → consumers (audit, risk-orchestrator) |
| Wrong P&L | position-service (realized) + risk-engine (unrealized) → FX rates → UI aggregation |
| Audit gaps | trade events → Kafka → audit-service → hash chain verification |

For each hop, check:
1. Is data arriving? (logs, Kafka lag)
2. Is data correct? (compare input vs output)
3. Is data fresh? (check timestamps)

## Step 4 — Check what changed

```bash
# Recent commits
git log --oneline -20
# Recent config changes
git diff HEAD~5 -- '*.yaml' '*.yml' '*.properties' '*.conf'
```

## Step 5 — Formulate hypothesis and resolution

Based on findings:
1. **State the hypothesis** — "The risk-engine gRPC call is timing out because..."
2. **Propose immediate mitigation** — restart, config change, rollback
3. **Propose root cause fix** — code change, infrastructure fix
4. **Identify follow-up actions** — monitoring gaps, missing alerts, documentation

## Step 6 — Document

Summarise in incident format:
- **Impact**: who/what is affected
- **Timeline**: when it started, key events
- **Root cause**: what caused it (or hypothesis if uncertain)
- **Resolution**: what fixed it
- **Follow-ups**: what to do to prevent recurrence

## Reminders

- Check timestamps before assuming data is wrong — staleness is the #1 cause
- Check Kafka consumer lag — most "data not arriving" issues are consumer problems
- Check recent deployments — most "it suddenly stopped working" issues correlate with a change
- Do not restart services blindly — understand why before restarting
