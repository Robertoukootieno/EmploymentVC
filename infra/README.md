# EmploymentVC Infrastructure Guide

Enterprise-grade infrastructure for the Employment Verification Credential Platform including Kubernetes, Besu blockchain, PostgreSQL, security hardening, and observability.

## Directory Structure

```
infra/
├── besu/                          # Hyperledger Besu blockchain node
│   ├── genesis.json              # Network genesis configuration
│   ├── docker-compose.besu.yml   # Besu deployment
│   └── start-besu.sh             # Node startup script
├── helm/                          # Helm charts for Kubernetes
│   ├── platform-chart/           # Main platform Helm chart
│   ├── issuer-chart/             # Issuer service Helm chart
│   ├── verifier-chart/           # Verifier service Helm chart
│   └── wallet-chart/             # Wallet service Helm chart
├── java/                          # Java build artifacts & configuration
├── k8s/                           # Native Kubernetes manifests
├── kubernetes/                    # Extended K8s configurations
├── keycloak/                      # Keycloak identity management
├── postgres/                      # PostgreSQL database
│   └── init.sql/                 # SQL initialization scripts
├── security/                      # Security implementations
│   ├── certs/                    # SSL/TLS certificates
│   ├── crowdsec/                 # CrowdSec DDoS/brute-force protection
│   ├── docker-compose.security.yml  # WAF (ModSecurity + nginx)
│   ├── modsecurity/              # ModSecurity rules
│   └── .env.example              # Security service environment
├── terraform/                     # Infrastructure as Code
│   ├── k8s/                      # Terraform for Kubernetes
│   ├── networking/               # Network infrastructure
│   ├── secrets/                  # Secret management configs
│   └── storage/                  # Storage configurations
├── mtls/                          # mTLS certificate management
├── vault/                         # HashiCorp Vault secret storage
├── elk/                           # ELK stack
└── prometheus/                    # Prometheus monitoring
```

## Quick Start: Docker Compose

Start the integrated platform with all services:

```bash
cd /home/robert/EmploymentVC

# Start all infrastructure components
./scripts/start-platform-stack.sh

# Optional: Include Vault and ELK
./scripts/start-platform-stack.sh --with-vault --with-elk

# Check service status
docker ps

# Stop everything (reverse order)
./scripts/stop-platform-stack.sh --with-vault --with-elk --prune
```

**Services Started:**
- PostgreSQL (port 5432)
- Redis (port 6379)  
- Besu blockchain (port 8545)
- Keycloak (port 8092)
- ModSecurity WAF (port 80/443)
- Prometheus (port 9090)
- Grafana (port 3000)

## PostgreSQL Setup

SQL initialization scripts in `postgres/init.sql/`:
1. `01-init-databases.sql` - Creates databases, users, and extensions
2. `02-core-schema.sql` - Core tenant/org management
3. `03-auth-schema.sql` - Authentication and security tables
4. `04-credential-schema.sql` - Credential registry

**Databases Created**: employmentvc_core, auth, wallet, did, credential, issuer, verifier, keycloak

## Besu Blockchain

### Quick Start

```bash
docker compose -f infra/besu/docker-compose.besu.yml up -d
curl http://localhost:8545 -X POST -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"eth_chainId","params":[],"id":1}'
```

### Configuration
- Chain ID: 1337 (development)
- Consensus: Clique (PoA)
- Gas Limit: 128M

## Security: Integrated Approach

### `/security/` Directory
- **opa-policies/**: Access control & credential policies
- **certificates/**: TLS/mTLS certificates
- **threat-models/**: Security assessments
- **sbom/**: Software Bill of Materials
- **vault-config/**: Vault policies
- **security-tests/**: SAST, DAST, dependency scanning

### `infra/security/` Directory
- **ModSecurity WAF**: OWASP CRS protection
- **CrowdSec**: DDoS & brute-force detection
- **nginx**: Reverse proxy, TLS termination

## Kubernetes Deployment

### Helm
```bash
helm install employmentvc infra/helm/platform-chart/ \
  --namespace provenly --create-namespace
```

### Terraform
```bash
cd infra/terraform/k8s
terraform init
terraform apply -var postgres_password=xxx -var redis_password=yyy
```

### Native Manifests
```bash
kubectl apply -f /k8s/
```

## Troubleshooting

```bash
# PostgreSQL
docker logs postgres

# Besu
docker logs besu-node

# Security
docker logs nginx-modsecurity
docker logs crowdsec
```

## Production Checklist
- [ ] Move secrets to HashiCorp Vault
- [ ] Use production TLS certificates
- [ ] Enable database backups
- [ ] Deploy 3+ Postgres replicas
- [ ] Configure Prometheus + Grafana
- [ ] Apply network policies in K8s
- [ ] Review OPA policies

## Links
- [Besu Docs](https://besu.hyperledger.org/)
- [PostgreSQL HA](https://www.postgresql.org/docs/current/warm-standby.html)
- [ModSecurity CRS](https://coreruleset.org/)
- [OPA Policy Language](https://www.openpolicyagent.org/docs/latest/policy-language/)

---
**Version**: 2026-02-21 | **Maintainer**: EmploymentVC Team
