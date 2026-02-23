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

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SHARED_NETWORK="${EMPLOYMENTVC_SHARED_NETWORK:-employmentvc-network}"

WITH_VAULT=false
WITH_ELK=false
PRUNE=false

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
    --prune)
      PRUNE=true
      shift
      ;;
    *)
      echo "Unknown option: $1"
      echo "Usage: $0 [--with-vault] [--with-elk] [--prune]"
      exit 1
      ;;
  esac
done

cd "$PROJECT_ROOT"

print_step "Phase 1/4: stopping security stack"
docker compose -p employmentvc-security -f infra/security/docker-compose.security.yml down || true
print_ok "Security stack stopped"

if [[ "$WITH_ELK" == "true" ]]; then
  print_step "Phase 2a: stopping ELK"
  docker compose -p employmentvc-elk -f infra/elk/docker-compose.elk.yml down || true
  print_ok "ELK stopped"
fi

if [[ "$WITH_VAULT" == "true" ]]; then
  print_step "Phase 2b: stopping Vault"
  docker compose -p employmentvc-vault -f infra/vault/docker-compose.vault.yml down || true
  print_ok "Vault stopped"
fi

print_step "Phase 3/4: stopping observability (Prometheus, Grafana, Loki, Tempo, Alertmanager)"
docker compose -p employmentvc-observability \
  -f observability/docker-compose.observability.yml \
  -f observability/docker-compose.alerts.yml down || true
print_ok "Observability stack stopped"

print_step "Phase 4/4: stopping core infra (Postgres, Redis, Besu, Keycloak)"
docker compose -p employmentvc -f docker-compose.yml down || true
print_ok "Core infra stopped"

if [[ "$PRUNE" == "true" ]]; then
  print_step "Cleaning up volumes and dangling resources"
  docker system prune -af --volumes || true
  print_ok "Cleanup complete"
fi

print_ok "Platform stack stopped"
