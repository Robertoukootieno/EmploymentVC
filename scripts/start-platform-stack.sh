#!/usr/bin/env bash

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_step() { echo -e "${BLUE}==>${NC} $1"; }
print_ok() { echo -e "${GREEN}✅${NC} $1"; }
print_warn() { echo -e "${YELLOW}⚠️${NC} $1"; }
print_err() { echo -e "${RED}❌${NC} $1"; }

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SHARED_NETWORK="${EMPLOYMENTVC_SHARED_NETWORK:-employmentvc-network}"

WITH_VAULT=false
WITH_ELK=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --with-vault)
      WITH_VAULT=true
      shift
      ;;
    --with-elk)
      WITH_ELK=true
      shift
      ;;
    *)
      print_err "Unknown option: $1"
      echo "Usage: $0 [--with-vault] [--with-elk]"
      exit 1
      ;;
  esac
done

cd "$PROJECT_ROOT"

print_step "Checking prerequisites"
command -v docker >/dev/null 2>&1 || { print_err "Docker is not installed"; exit 1; }
docker info >/dev/null 2>&1 || { print_err "Docker daemon is not running"; exit 1; }
print_ok "Docker is available"

print_step "Ensuring shared network exists: $SHARED_NETWORK"
if ! docker network inspect "$SHARED_NETWORK" >/dev/null 2>&1; then
  docker network create "$SHARED_NETWORK" >/dev/null
  print_ok "Created network $SHARED_NETWORK"
else
  print_ok "Network $SHARED_NETWORK already exists"
fi

print_step "Phase 1/4: starting core infra (Postgres, Redis, Besu, Keycloak)"
EMPLOYMENTVC_SHARED_NETWORK="$SHARED_NETWORK" docker compose -p employmentvc -f docker-compose.yml up -d postgres redis besu-node keycloak
print_ok "Core infra started"

print_step "Phase 2/4: starting observability (Prometheus, Grafana, Loki, Tempo, Alertmanager)"
EMPLOYMENTVC_SHARED_NETWORK="$SHARED_NETWORK" docker compose -p employmentvc-observability \
  -f observability/docker-compose.observability.yml \
  -f observability/docker-compose.alerts.yml up -d
print_ok "Observability stack started"

print_step "Phase 3/4: starting security stack (WAF + CrowdSec)"
EMPLOYMENTVC_SHARED_NETWORK="$SHARED_NETWORK" docker compose -p employmentvc-security -f infra/security/docker-compose.security.yml up -d
print_ok "Security stack started"

if [[ "$WITH_VAULT" == "true" ]]; then
  print_step "Phase 4a: starting Vault"
  EMPLOYMENTVC_SHARED_NETWORK="$SHARED_NETWORK" docker compose -p employmentvc-vault -f infra/vault/docker-compose.vault.yml up -d
  print_ok "Vault started"
fi

if [[ "$WITH_ELK" == "true" ]]; then
  print_step "Phase 4b: starting ELK"
  EMPLOYMENTVC_SHARED_NETWORK="$SHARED_NETWORK" docker compose -p employmentvc-elk -f infra/elk/docker-compose.elk.yml up -d
  print_ok "ELK started"
fi

print_step "Quick status"
docker compose -p employmentvc -f docker-compose.yml ps | cat

docker compose -p employmentvc-observability -f observability/docker-compose.observability.yml -f observability/docker-compose.alerts.yml ps | cat

docker compose -p employmentvc-security -f infra/security/docker-compose.security.yml ps | cat

if [[ "$WITH_VAULT" == "true" ]]; then
  docker compose -p employmentvc-vault -f infra/vault/docker-compose.vault.yml ps | cat
fi

if [[ "$WITH_ELK" == "true" ]]; then
  docker compose -p employmentvc-elk -f infra/elk/docker-compose.elk.yml ps | cat
fi

print_step "Health endpoints"
for endpoint in \
  "http://localhost:8092/realms/master" \
  "http://localhost:9090/-/ready" \
  "http://localhost:3000/api/health" \
  "http://localhost:3100/ready" \
  "http://localhost:3200/ready" \
  "http://localhost:9093/-/ready"; do
  if curl -fsS "$endpoint" >/dev/null 2>&1; then
    print_ok "$endpoint"
  else
    print_warn "$endpoint not ready yet"
  fi
done

echo
echo "Run backend services after infra is up:"
echo "  ./gradlew :auth-service:bootRun :wallet-api:bootRun :api-gateway:bootRun"
echo
print_ok "Platform orchestration complete"
