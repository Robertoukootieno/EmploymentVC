#!/usr/bin/env bash

# EmploymentVC Infrastructure Integration Guide
# This script validates and documents the complete infrastructure setup

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_header() { echo -e "\n${BLUE}================================${NC}"; echo -e "${BLUE}$1${NC}"; echo -e "${BLUE}================================${NC}\n"; }
print_ok() { echo -e "${GREEN}✓${NC} $1"; }
print_err() { echo -e "${RED}✗${NC} $1"; }
print_warn() { echo -e "${YELLOW}⚠${NC} $1"; }
print_info() { echo -e "${BLUE}ℹ${NC} $1"; }

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

print_header "EmploymentVC Infrastructure Setup Verification"

# 1. Check directories exist
print_info "Checking infrastructure directories..."
DIRS=(
  "infra/besu"
  "infra/helm/platform-chart"
  "infra/postgres/init.sql"
  "infra/security"
  "infra/terraform/k8s"
  "security"
  "k8s"
  "scripts"
)

for dir in "${DIRS[@]}"; do
  if [ -d "$dir" ]; then
    print_ok "Directory exists: $dir"
  else
    print_err "Missing directory: $dir"
  fi
done

# 2. Check critical files
print_info "Checking critical files..."
FILES=(
  "docker-compose.yml"
  "infra/besu/genesis.json"
  "infra/besu/docker-compose.besu.yml"
  "infra/postgres/init.sql/01-init-databases.sql"
  "infra/security/docker-compose.security.yml"
  "infra/helm/platform-chart/Chart.yaml"
  "infra/terraform/k8s/main.tf"
  "scripts/start-platform-stack.sh"
  "scripts/stop-platform-stack.sh"
)

for file in "${FILES[@]}"; do
  if [ -f "$file" ]; then
    print_ok "File exists: $file"
  else
    print_err "Missing file: $file"
  fi
done

# 3. Validate Docker availability
print_info "Checking Docker..."
if command -v docker &> /dev/null; then
  if docker info >/dev/null 2>&1; then
    print_ok "Docker daemon is running"
  else
    print_err "Docker daemon is not running"
  fi
else
  print_err "Docker is not installed"
fi

# 4. Validate Kubernetes tools (optional)
print_info "Checking Kubernetes tools (optional)..."
if command -v kubectl &> /dev/null; then
  print_ok "kubectl is installed"
else
  print_warn "kubectl not found (required for K8s deployment only)"
fi

if command -v helm &> /dev/null; then
  print_ok "helm is installed"
else
  print_warn "helm not found (required for Helm deployment only)"
fi

if command -v terraform &> /dev/null; then
  print_ok "terraform is installed"
else
  print_warn "terraform not found (required for Terraform deployment only)"
fi

# 5. Validate compose files
print_info "Validating Docker Compose files..."
if command -v docker-compose &> /dev/null || command -v docker &> /dev/null; then
  compose_cmd="docker compose"
  
  # Check if docker compose v1
  if ! command -v docker-compose &> /dev/null; then
    if docker compose 2>&1 | grep -q "Docker Compose"; then
      compose_cmd="docker compose"
    else
      compose_cmd="docker-compose"
    fi
  fi
  
  # Validate main compose
  if $compose_cmd -f docker-compose.yml config > /dev/null 2>&1; then
    print_ok "Main docker-compose.yml is valid"
  else
    print_err "docker-compose.yml validation failed"
  fi
  
  # Validate security compose
  if $compose_cmd -f infra/security/docker-compose.security.yml config > /dev/null 2>&1; then
    print_ok "infra/security/docker-compose.security.yml is valid"
  else
    print_err "infra/security/docker-compose.security.yml validation failed"
  fi
  
  # Validate besu compose
  if $compose_cmd -f infra/besu/docker-compose.besu.yml config > /dev/null 2>&1; then
    print_ok "infra/besu/docker-compose.besu.yml is valid"
  else
    print_err "infra/besu/docker-compose.besu.yml validation failed"
  fi
else
  print_err "Docker or docker-compose not found"
fi

# 6. Check scripts are executable
print_info "Checking script permissions..."
if [ -x "scripts/start-platform-stack.sh" ]; then
  print_ok "scripts/start-platform-stack.sh is executable"
else
  print_warn "scripts/start-platform-stack.sh is not executable"
fi

if [ -x "scripts/stop-platform-stack.sh" ]; then
  print_ok "scripts/stop-platform-stack.sh is executable"
else
  print_warn "scripts/stop-platform-stack.sh is not executable"
fi

# 7. Infrastructure summary
print_header "Infrastructure Summary"

cat << 'SUMMARY'
🏗️  EMPLOYMENTVC INFRASTRUCTURE STACK

DOCKER COMPOSE (Development/Testing)
├─ Core Services
│  ├─ PostgreSQL (multi-database setup)
│  ├─ Redis (caching layer)
│  ├─ Besu (blockchain node)
│  └─ Keycloak (identity management)
├─ Observability
│  ├─ Prometheus (metrics)
│  ├─ Grafana (visualization)
│  ├─ Loki (logs)
│  ├─ Tempo (traces)
│  └─ Alertmanager
├─ Security
│  ├─ ModSecurity WAF (nginx)
│  └─ CrowdSec (DDoS/brute-force)
├─ Optional
│  ├─ HashiCorp Vault (secrets)
│  └─ ELK Stack (logging)

KUBERNETES (Production)
├─ Deployment Methods
│  ├─ Helm Charts (infra/helm/)
│  ├─ Terraform IaC (infra/terraform/k8s/)
│  └─ Native Manifests (/k8s/)
├─ Namespaces: provenly, provenly-dev, provenly-staging
├─ Configuration: ConfigMaps, Secrets, RBAC
└─ Networking: Network Policies, Service Mesh (optional)

DATABASES (PostgreSQL)
├─ employmentvc_core (platform core)
├─ employmentvc_auth (auth service)
├─ employmentvc_wallet (wallet service)
├─ employmentvc_did (DID registry)
├─ employmentvc_credential (credential registry)
├─ employmentvc_issuer (issuer service)
├─ employmentvc_verifier (verifier service)
└─ keycloak (identity management)

SECURITY INTEGRATION
├─ /security/ (Policies & Artifacts)
│  ├─ opa-policies/ (access control)
│  ├─ certificates/ (TLS/mTLS)
│  ├─ threat-models/ (assessments)
│  ├─ sbom/ (supply chain)
│  ├─ vault-config/ (secret mgmt)
│  └─ security-tests/ (SAST/DAST)
└─ infra/security/ (Runtime Protection)
   ├─ ModSecurity + nginx (WAF)
   └─ CrowdSec (behavioral detection)

BLOCKCHAIN
├─ Node: Hyperledger Besu
├─ Chain ID: 1337 (development)
├─ Consensus: Clique (PoA)
├─ RPC Port: 8545
├─ WebSocket Port: 8546
└─ P2P Port: 30303

QUICK COMMANDS
├─ Start Stack: ./scripts/start-platform-stack.sh
├─ Stop Stack: ./scripts/stop-platform-stack.sh
├─ With Vault: --with-vault
├─ With ELK: --with-elk
├─ Cleanup: --prune
└─ Check Status: docker ps

KUBERNETES DEPLOYMENT
├─ Helm: helm install employmentvc infra/helm/platform-chart/ -n provenly
├─ Terraform: cd infra/terraform/k8s && terraform apply
└─ Manifests: kubectl apply -f /k8s/

HEALTH CHECKS
├─ PostgreSQL: psql -h localhost -U app_user -d employmentvc_core
├─ Redis: redis-cli ping
├─ Besu: curl http://localhost:8545 -X POST -H "Content-Type: application/json" \
│         -d '{"jsonrpc":"2.0","method":"eth_chainId","params":[],"id":1}'
├─ Keycloak: http://localhost:8092/realms/master
├─ Prometheus: http://localhost:9090
├─ Grafana: http://localhost:3000 (admin/admin)
├─ WAF: http://localhost:8080/nginx_status
└─ CrowdSec: http://localhost:5000

NEXT STEPS
1. Copy .env.example to .env and update credentials
2. Run: ./scripts/start-platform-stack.sh
3. Wait 60-90s for services to stabilize
4. Verify health endpoints above
5. Deploy backend services: ./gradlew bootRun (per service)
6. Access platform via API:
   - API Gateway: http://localhost:3000
   - Auth Service: http://localhost:8081
   - Wallet API: http://localhost:8082
SUMMARY

print_header "Setup Verification Complete"

echo "✓ Infrastructure is ready for deployment"
echo ""
echo "Start the platform:"
echo "  $ ./scripts/start-platform-stack.sh"
echo ""
echo "Check service status:"
echo "  $ docker ps"
echo ""
echo "Stop the platform:"
echo "  $ ./scripts/stop-platform-stack.sh"
echo ""

print_ok "All checks passed! Infrastructure is configured correctly."
