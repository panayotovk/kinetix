# Post-Deployment Smoke Test Procedure

Run this checklist after every deployment to any environment. Each step must pass before moving on. If any step fails, halt the deployment and rollback.

All commands assume `KINETIX_HOST` is set to the gateway base URL (e.g., `https://kinetix.internal`) and `TOKEN` is a valid Keycloak bearer token for a trader-role account.

---

## 1. Health Endpoints

Hit the liveness and readiness probes on every service. A non-200 response means the service is not ready.

```bash
for svc in gateway position-service price-service rates-service \
            volatility-service correlation-service reference-data-service \
            risk-orchestrator regulatory-service notification-service audit-service; do
  echo -n "$svc /health: "
  curl -sf http://${svc}:8080/health && echo OK || echo FAIL

  echo -n "$svc /health/ready: "
  curl -sf http://${svc}:8080/health/ready && echo OK || echo FAIL
done
```

**Pass criterion:** All services return HTTP 200 for both `/health` and `/health/ready`.

---

## 2. Helm Test

Run the Helm test suite to verify the deployed chart hooks pass.

```bash
helm test kinetix --namespace kinetix --timeout 5m
```

**Pass criterion:** All test pods complete with `Succeeded` phase. Review `helm test` output for any `Failed` pods.

---

## 3. Trade Booking

Book a test trade via the gateway and verify the position is created.

```bash
# Book a test trade
TRADE_ID=$(curl -sf -X POST "$KINETIX_HOST/api/trades" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "instrumentId": "SMOKE-TEST-TRADE",
    "quantity": 100,
    "price": 50.00,
    "currency": "USD",
    "book": "SMOKE",
    "counterpartyId": "SMOKE-CP"
  }' | jq -r '.tradeId')

echo "Booked trade: $TRADE_ID"

# Verify position was created
sleep 2
curl -sf "$KINETIX_HOST/api/positions?book=SMOKE" \
  -H "Authorization: Bearer $TOKEN" | jq '.[] | select(.instrumentId == "SMOKE-TEST-TRADE")'
```

**Pass criterion:** The POST returns HTTP 201 with a `tradeId`. The subsequent position query returns at least one position with `instrumentId = "SMOKE-TEST-TRADE"` and `quantity = 100`.

---

## 4. Price Feed

Connect a WebSocket to the notification-service and verify that a price update arrives within 10 seconds.

```bash
# Requires websocat or wscat
wscat -c "wss://$KINETIX_HOST/ws/prices" \
  -H "Authorization: Bearer $TOKEN" \
  --wait 10 \
  --close 10 \
  | head -1
```

**Pass criterion:** At least one JSON message is received within 10 seconds. The message must contain a `price` field with a numeric value. If the price feed is not actively updating (e.g., outside market hours), manually publish a test price event to the `kinetix.prices` Kafka topic and verify it propagates.

---

## 5. VaR Calculation

Trigger a risk calculation for the smoke test book and verify a result arrives within 30 seconds.

```bash
# Trigger calculation
curl -sf -X POST "$KINETIX_HOST/api/risk/calculate" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"portfolioId": "SMOKE", "calculationType": "VAR"}'

# Poll for result (up to 30s)
for i in $(seq 1 30); do
  RESULT=$(curl -sf "$KINETIX_HOST/api/risk/latest?portfolioId=SMOKE" \
    -H "Authorization: Bearer $TOKEN" | jq '.varResult // empty')
  if [ -n "$RESULT" ]; then
    echo "VaR result: $RESULT"
    break
  fi
  sleep 1
done

if [ -z "$RESULT" ]; then
  echo "FAIL: No VaR result within 30 seconds"
  exit 1
fi
```

**Pass criterion:** A VaR result is returned within 30 seconds. The result must be a non-null, non-zero numeric value.

---

## 6. Audit Trail

Verify that the trade booked in step 3 appears in the audit log with a valid hash chain.

```bash
# Retrieve audit events for the trade
AUDIT_EVENTS=$(curl -sf "$KINETIX_HOST/api/audit/events?entityId=$TRADE_ID" \
  -H "Authorization: Bearer $TOKEN")

echo "Audit events found: $(echo $AUDIT_EVENTS | jq length)"

# Verify hash chain integrity
curl -sf "$KINETIX_HOST/api/audit/verify?entityId=$TRADE_ID" \
  -H "Authorization: Bearer $TOKEN" | jq '.valid'
```

**Pass criterion:** At least one audit event exists for the trade ID. The `/audit/verify` endpoint returns `{ "valid": true }`. A `false` response indicates hash chain corruption and must be investigated immediately.

---

## 7. Consumer Lag

Check that all Kafka consumer groups have lag below 100 messages. Elevated lag indicates a service is not keeping up.

```bash
# Requires kafka-consumer-groups.sh on the PATH, or use the broker pod
kubectl exec -n kinetix deploy/kafka -- kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --all-groups \
  | awk 'NR>1 && $6 > 100 { print "HIGH LAG:", $1, "topic:", $4, "partition:", $5, "lag:", $6 }'
```

**Pass criterion:** No consumer group reports lag above 100. If any group is above 100, wait 60 seconds and re-check before failing. A brief post-deployment lag spike is acceptable; sustained lag is not.

---

## 8. DLQ Check

Verify that no new messages arrived on any dead-letter queue topic during or after the deployment. DLQ messages indicate processing failures.

```bash
# List DLQ topics and check for recent messages
kubectl exec -n kinetix deploy/kafka -- kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --group kinetix-dlq-monitor 2>/dev/null || true

# Check offset on each DLQ topic
for dlq in kinetix.trades.dlq kinetix.prices.dlq kinetix.risk-results.dlq \
            kinetix.audit.dlq kinetix.notifications.dlq; do
  echo -n "$dlq end offset: "
  kubectl exec -n kinetix deploy/kafka -- kafka-run-class.sh kafka.tools.GetOffsetShell \
    --broker-list localhost:9092 \
    --topic $dlq \
    --time -1 2>/dev/null | awk -F: '{sum += $3} END {print sum+0}'
done
```

**Pass criterion:** All DLQ topic end offsets are unchanged from their pre-deployment baseline. Any new messages on a DLQ topic must be investigated before the deployment is declared successful. Retrieve the messages with `kafka-console-consumer.sh` to determine the cause.

---

## Smoke Test Summary Checklist

| # | Check | Pass |
|---|---|---|
| 1 | All service health + readiness endpoints return 200 | |
| 2 | `helm test kinetix` all pods succeed | |
| 3 | Test trade booked, position visible | |
| 4 | WebSocket delivers price update within 10s | |
| 5 | VaR result returned within 30s | |
| 6 | Audit event exists for trade, hash chain valid | |
| 7 | All consumer groups lag < 100 | |
| 8 | All DLQ topics have no new messages | |

All 8 checks must pass for the deployment to be declared successful.
