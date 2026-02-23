# EmploymentVC Infrastructure & Security Integration Guide

## Overview

This guide provides a complete picture of how the EmploymentVC platform's infrastructure, security, and application layers integrate together for a production-ready deployment.

### Architecture Layers

```
┌─────────────────────────────────────────────────────┐
│          API Layer (Application Services)             │
│  Auth | Wallet | Issuer | Verifier | Gateway         │
└─────────────────────────────────┬─────────────────────┘
                                  │
┌─────────────────────────────────┴─────────────────────┐
│       Security & Access Control Layer                  │
│  ┌─────────────────────────────────────────────────┐  │
│  │ WAF (ModSecurity) | CrowdSec | OPA Policies    │  │
│  │ mTLS Certificates | Network Policies           │  │
│  └─────────────────────────────────────────────────┘  │
└─────────────────────────────────┬─────────────────────┘
                                  │
┌─────────────────────────────────┴─────────────────────┐
│     Core Infrastructure Layer                         │
│  ┌───────────────────┬──────────────────┬───────────┐ │
│  │  Database Layer   │   Caching Layer  │ Blockchain│ │
│  │  (PostgreSQL)     │  (Redis)         │  (Besu)   │ │
│  └───────────────────┴──────────────────┴───────────┘ │
└─────────────────────────────────┬─────────────────────┘
                                  │
┌─────────────────────────────────┴─────────────────────┐
│     Observability & Monitoring Layer                  │
│  Prometheus | Grafana | Loki | Tempo | Alertmanager  │
└──────────────────────────────────────────────────────┘
```

## Part 1: Security Architecture

### Two-Directory Security Model

#### `/security/` - Policy & Artifact Layer
Centralized security governance, independent of deployment method.

```
security/
├── opa-policies/              # Access control policies
│   ├── access-control.rego    # RBAC/ABAC rules
│   ├── issuance-rules.rego    # Credential issuance policies
│   ├── wallet-rules.rego      # Wallet operation policies
│   └── verification-rules.rego # Credential verification
├── certificates/              # PKI Management
│   ├── ca.crt                 # Root CA certificate
│   ├── ca.key                 # Root CA private key (KEEP SECURE)
│   ├── service.crt            # Service certificate
│   └── client.crt             # Client certificate (mTLS)
├── threat-models/             # Security assessments
│   ├── threat-model.md
│   └── risk-register.csv
├── sbom/                      # Supply chain security
│   └── provenly-sbom-*.json   # OWASP CycloneDX format
├── vault-config/              # Secret management
│   ├── policies/
│   ├── auth-methods/
│   └── secret-engines/
└── security-tests/            # Automated security scanning
    ├── sast/                  # Static code analysis
    ├── dast/                  # Dynamic penetration testing
    └── dependency/            # Vulnerability scanning
```

**Usage**: Version-controlled, audit-able, shareable across environments.

#### `infra/security/` - Runtime Protection Deployment
Operational security layers deployed with infrastructure.

```
infra/security/
├── docker-compose.security.yml  # WAF + DDoS protection stack
├── modsecurity/                 # ModSecurity configuration
│   ├── modsecurity.conf         # Core rule set config
│   ├── custom-rules.conf        # Custom rules
│   └── nginx.conf               # Reverse proxy config
├── crowdsec/                    # DDoS/brute-force detection
│   ├── config.yaml
│   ├── scenarios/
│   └── bouncers/
├── certs/                       # Runtime certificate storage
│   ├── cert.pem
│   ├── key.pem
│   └── chain.pem
└── .env.example                 # Runtime credentials
```

**Usage**: Deployed with each environment (dev/staging/prod).

## Part 2: Infrastructure Components

### Database Tier

**PostgreSQL** - Multi-database setup with role-based access:

```sql
-- Databases
employmentvc_core           # Core platform tables
employmentvc_auth          # Auth service schema
employmentvc_wallet        # Wallet service data
employmentvc_did           # DID registry
employmentvc_credential    # Credentials & schemas
employmentvc_issuer        # Issuer service data
employmentvc_verifier      # Verifier service data
keycloak                   # Keycloak realm data

-- Roles (Users)
app_user                   # Application operations
monitor_user               # Read-only monitoring
replication_user           # Database replication (HA)
```

**Initialization** (`infra/postgres/init.sql/`):
1. Creates databases and roles
2. Installs PostgreSQL extensions (uuid, crypto, JSON, etc.)
3. Sets up core schema with audit logging
4. Configures auth-related tables (lockouts, login audit)
5. Creates credential registry schema

### Caching Tier

**Redis** - Session/credential caching:
- Session storage
- Token blacklist
- Rate limiting counters
- Cached credential schemas

Password-protected, network-isolated.

### Blockchain Tier

**Hyperledger Besu** - Private blockchain for DID operations:
- **Chain ID**: 1337
- **Consensus**: Clique (PoA) with 15-second blocks
- **RPC Endpoints**: HTTP (8545) + WebSocket (8546)
- **Storage**: Persistent volume for blockchain state

**Genesis Configuration** (`infra/besu/genesis.json`):
- Pre-allocated development accounts
- Gas limit: 128M (~100k transactions/block)
- Istanbul fork enabled for latest Ethereum opcodes

### Identity Management

**Keycloak** - User authentication & authorization:
- Realm configuration stored in PostgreSQL
- OIDC/OAuth2 provider for all services
- User federation (LDAP/AD support)
- Two-factor authentication (TOTP)
- Identity brokering for social login

## Part 3: Security Enforcement

### Web Application Firewall (ModSecurity)

nginx + ModSecurity + OWASP CRS protects against:
- SQL injection
- Cross-site scripting (XSS)
- Cross-site request forgery (CSRF)
- Remote code execution
- Path traversal
- XXE attacks

**Location**: `infra/security/docker-compose.security.yml`

**Configuration**:
```nginx
# High-risk rule actions
SecDefaultAction "phase:1,log,deny,status:403"
SecDefaultAction "phase:2,log,deny,status:403"

# Custom rules in modsecurity/custom-rules.conf
# Examples: API key validation, JWT verification
```

### DDoS & Brute-Force Protection (CrowdSec)

CrowdSec provides:
- Behavioral threat detection
- Distributed IP reputation
- Automatic blocking via nginx/iptables bouncer
- Customizable scenarios (3+ failed auth = 24h block)
- Dashboard at `http://localhost:5000`

**Scenarios Enabled**:
- `crowdsecurity/http-pathtraversal-detection`
- `crowdsecurity/http-xss-detection`
- `crowdsecurity/ssh-bf`
- `crowdsecurity/http-cve-2021-41773`

### Access Control Policies (OPA)

OPA Rego policies in `/security/opa-policies/`:

**Example: Credential Issuance Policy**
```rego
package employmentvc.issuance

# Only employment verifiers can issue employment credentials
allow {
    input.role == "employment-verifier"
    input.credential_type == "employment-verification"
    input.scope == "employment-verification:issue"
}

# Only issuer organization can issue to its employees
allow {
    input.issuer_organization == input.subject_organization
}
```

**Integration in Services**:
```java
// In auth-service
OpaEvaluator evaluator = new OpaEvaluator("http://localhost:8181");
DecisionResult result = evaluator.evaluate(
    "data.employmentvc.issuance.allow",
    new IssuanceContext(...)
);
if (!result.allowed) {
    throw new AccessDeniedException("Policy denies issuance");
}
```

### Certificate Management (mTLS)

Service-to-service communication via mTLS:

```
service-a (client cert)
         ↓ (mutual TLS)
    nginx/reverse-proxy
         ↓
service-b (server cert)
```

**Certificate Directory**: `/security/certificates/`

**Generation** (for development):
```bash
# Using OpenSSL
openssl req -x509 -newkey rsa:2048 -keyout ca.key -out ca.crt
openssl req -new -keyout service.key -out service.csr
openssl x509 -req -in service.csr -CA ca.crt -CAkey ca.key -out service.crt
```

## Part 4: Deployment Methods

### Docker Compose (Development)

**Start Everything:**
```bash
./scripts/start-platform-stack.sh [--with-vault] [--with-elk]
```

**Order of Startup:**
1. Core infra (Postgres, Redis, Besu, Keycloak)
2. Observability (Prometheus, Grafana, Loki, Alertmanager)
3. Security (WAF, CrowdSec)
4. Optional (Vault, ELK)

**Network**: All services on `employmentvc-network`

**Verification:**
```bash
# Check services running
docker ps --filter "label=service"

# Health endpoints
curl http://localhost:8545 -X POST -d '{"jsonrpc":"2.0","method":"eth_chainId","params":[],"id":1}'
curl http://localhost:8092/realms/master
curl http://localhost:9090/-/ready
```

### Kubernetes (Production)

#### Option 1: Helm Charts

```bash
cd infra/helm/platform-chart

# Create namespace
kubectl create namespace provenly

# Install chart
helm install employmentvc . \
  --namespace provenly \
  --values values-prod.yaml \
  --set postgresql.auth.password=\$ecure_pwd
```

**Chart Structure:**
```
platform-chart/
├── Chart.yaml           # Chart metadata
├── values.yaml          # Default values
├── templates/
│   ├── namespace.yaml   # RBAC + Network Policies
│   ├── deployment.yaml  # Service deployments
│   ├── statefulset.yaml # Stateful services
│   ├── ingress.yaml     # Ingress rules
│   └── hpa.yaml         # Horizontal Pod Autoscaling
```

#### Option 2: Terraform (Infrastructure as Code)

```bash
cd infra/terraform/k8s

terraform init
terraform plan \
  -var postgres_password=\$ecure \
  -var redis_password=\$ecure

terraform apply \
  -var postgres_password=\$ecure \
  -var redis_password=\$ecure
```

**What Terraform Creates:**
- Namespaces (provenly, provenly-dev, provenly-staging)
- Service accounts + RBAC roles
- ConfigMaps & Secrets
- Network policies
- StatefulSets for databases
- Deployments for services

#### Option 3: Native Manifests

```bash
kubectl apply -f /k8s/namespace.yaml
kubectl apply -f /k8s/configmap.yaml
kubectl apply -f /k8s/secrets.yaml
kubectl apply -f /k8s/postgres.yaml
# ... continue with other manifests
```

## Part 5: Observability Integration

### Monitoring Stack

**Prometheus** (port 9090):
- Scrapes metrics from all services
- Retention: 15 days by default
- PromQL queries for alerting

**Grafana** (port 3000):
- Dashboards visualize metrics
- Pre-built dashboards for:
  - PostgreSQL performance
  - Redis memory usage
  - API request rates
  - Error rates & latency

**Loki** (port 3100):
- Log aggregation from all services
- Labels: `service`, `environment`, `pod`
- Queryable via LogQL in Grafana

**Alertmanager** (port 9093):
- Routes alerts based on rules
- Integrates with: Slack, PagerDuty, email, webhooks

### Example: Credential Issuance Alerts

```yaml
groups:
- name: credential-issuance
  rules:
  - alert: HighFailureRate
    expr: rate(issuance_failures_total[5m]) > 0.05
    for: 5m
    annotations:
      summary: "High credential issuance failure rate"
      
  - alert: SlowIssuanceTime
    expr: histogram_quantile(0.95, issuance_duration_seconds) > 2
    for: 10m
    annotations:
      summary: "P95 issuance time exceeds 2 seconds"
```

## Part 6: Security Integration Checklist

### Pre-Deployment

- [ ] Review OPA policies for your use case
- [ ] Generate TLS certificates (development: self-signed, production: CA-signed)
- [ ] Configure Keycloak realm (LDAP/federation if needed)
- [ ] Set up secret management (Vault or K8s Secrets)
- [ ] Enable Postgres audit logging
- [ ] Configure backup strategy

### Runtime

- [ ] Enable ModSecurity + CRS
- [ ] Deploy CrowdSec with custom scenarios
- [ ] Verify mTLS between services
- [ ] Monitor WAF/CrowdSec alerts
- [ ] Review logs daily (Loki)
- [ ] Check metrics (Prometheus/Grafana)

### Production

- [ ] Use enterprise TLS certificates (Let's Encrypt or corporate CA)
- [ ] Move all secrets to HashiCorp Vault
- [ ] Enable Postgres replication (3+ replicas)
- [ ] Set up automated backups
- [ ] Deploy network segmentation (VPC/subnets)
- [ ] Implement disaster recovery procedures
- [ ] Regular security audits (quarterly)
- [ ] Compliance scanning (SBOM, SAST, DAST)

## Part 7: Environment-Specific Configurations

### Development

```bash
# Maximum observability, relaxed security
NODE_ENV=development
SPRING_PROFILES_ACTIVE=dev
LOG_LEVEL=debug
POSTGRES_SSLMODE=disable
CERTIFICATE_VALIDATION=false
```

### Staging

```bash
# Production-like setup, full security, test data
NODE_ENV=staging
SPRING_PROFILES_ACTIVE=staging
LOG_LEVEL=info
POSTGRES_SSLMODE=require
CERTIFICATE_VALIDATION=true
WAF_ACTION=log  # Log violations but don't block
```

### Production

```bash
# Maximum security, audit logging, HA
NODE_ENV=production
SPRING_PROFILES_ACTIVE=prod
LOG_LEVEL=warn
POSTGRES_SSLMODE=require
CERTIFICATE_VALIDATION=true
WAF_ACTION=deny  # Block violations
REPLICATION_ENABLED=true
BACKUP_RETENTION=30  # days
```

## Part 8: Troubleshooting

### Service Won't Start

```bash
# Check logs
docker logs <service-name>

# Verify dependencies (e.g., Postgres)
curl -i http://postgres:5432
curl -i http://redis:6379

# Check DNS resolution
docker exec <service> nslookup <dependency-host>
```

### WAF Blocking Legitimate Traffic

```bash
# Check ModSecurity logs
docker logs nginx-modsecurity 2>&1 | grep "403\|deny"

# Disable specific rule (temporary)
# Edit infra/security/modsecurity/custom-rules.conf

# Reload WAF
docker restart nginx-modsecurity
```

### High Database Load

```bash
# Check slow queries (Postgres)
docker exec postgres psql -U app_user employmentvc_core \
  -c "SELECT query, calls, mean_time FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;"

# Check Redis memory
docker exec redis redis-cli INFO memory
```

## Part 9: Integration Commands

```bash
# Start full stack
./scripts/start-platform-stack.sh --with-vault --with-elk

# Verify infrastructure
./scripts/verify-infrastructure.sh

# Run backend services (after infra is up)
./gradlew :auth-service:bootRun &
./gradlew :wallet-api:bootRun &
./gradlew :api-gateway:bootRun &

# Deploy to Kubernetes
./scripts/deploy-to-kubernetes.sh  # (optional script)

# Stop everything
./scripts/stop-platform-stack.sh --with-vault --with-elk --prune
```

## Links & References

- **Infrastructure**: `infra/README.md`
- **Security Policies**: `/security/README.md`
- **Kubernetes**: `https://kubernetes.io/docs/concepts/security/`
- **OPA**: `https://www.openpolicyagent.org/docs/`
- **ModSecurity CRS**: `https://coreruleset.org/`
- **Besu Blockchain**: `https://besu.hyperledger.org/`
- **PostgreSQL HA**: `https://www.postgresql.org/docs/current/warm-standby.html`

---

**Version**: 2026-02-21  
**Status**: Integration Complete  
**Maintainer**: EmploymentVC Team
