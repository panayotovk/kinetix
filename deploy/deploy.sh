#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

# ── Create shared Docker network ────────────────────────────────────────────
docker network inspect kinetix >/dev/null 2>&1 || {
  echo "==> Creating 'kinetix' Docker network..."
  docker network create kinetix
}

# ── Override observability configs for containerised targets ─────────────────
export PROMETHEUS_CONFIG="${ROOT_DIR}/deploy/observability/prometheus.yml"
export ALERTMANAGER_CONFIG="${ROOT_DIR}/deploy/observability/alertmanager.yml"

# ── Ensure databases exist ──────────────────────────────────────────────────
echo "==> Starting infrastructure..."
docker compose \
  -f "$ROOT_DIR/infra/docker-compose.infra.yml" \
  up -d --wait

echo "==> Ensuring databases exist..."
docker exec kinetix-postgres psql -U kinetix -f /docker-entrypoint-initdb.d/01-create-databases.sql 2>/dev/null || true

# ── Start the full stack ────────────────────────────────────────────────────
echo "==> Starting all services (this may take a few minutes on first build)..."
docker compose \
  -f "$ROOT_DIR/infra/docker-compose.infra.yml" \
  -f "$ROOT_DIR/infra/docker-compose.auth.yml" \
  -f "$ROOT_DIR/infra/docker-compose.observability.yml" \
  -f "$ROOT_DIR/docker-compose.services.yml" \
  up -d --build --wait

# ── Summary ─────────────────────────────────────────────────────────────────
echo ""
echo "=============================================="
echo "  Kinetix is running"
echo "=============================================="
echo ""
printf "  %-22s %s\n" "Service" "URL"
printf "  %-22s %s\n" "──────────────────────" "──────────────────────────"
printf "  %-22s %s\n" "UI"                   "http://localhost:80"
printf "  %-22s %s\n" "Gateway API"          "http://localhost:8080"
printf "  %-22s %s\n" "Grafana"              "http://localhost:3000"
printf "  %-22s %s\n" "Prometheus"           "http://localhost:9090"
printf "  %-22s %s\n" "Keycloak"             "http://localhost:8180"
echo ""
echo "  Stop: ./deploy/deploy-down.sh"
echo ""
