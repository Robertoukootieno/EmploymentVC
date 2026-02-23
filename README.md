# Provenly Employment VC Platform

A comprehensive Verifiable Credentials platform for employment verification built with **Java/Spring Boot backend** and **Next.js frontend**, featuring Hyperledger Besu, EBSI APIs for DIDs, walt.id VCs, selective disclosure, and JSON-LD.

## 🏗️ Architecture

### Backend (Java/Spring Boot)
- **API Gateway** - Central routing, authentication, and rate limiting
- **Authentication Service** - Multi-method auth (Traditional, Web3, DID)
- **DID Registry** - EBSI integration for DID management
- **Credential Schema Registry** - JSON-LD schema management
- **Application Service** - Core VC operations (Issuer, Verifier, Wallets)

### Frontend (Next.js/React)
- **Web Application** - User interface for credential management
- **Web3 Integration** - Wallet connectivity and blockchain interactions
- **Responsive Design** - Mobile-first approach with Tailwind CSS

## 🏗️ Complete Infrastructure Stack

The platform includes enterprise-grade infrastructure for development, staging, and production:

### Core Services
- **PostgreSQL** - Multi-database setup with audit logging & role-based access
- **Redis** - Session caching and credential schema cache
- **Hyperledger Besu** - Private blockchain (Chain ID 1337, Clique consensus)
- **Keycloak** - Identity & Access Management (OIDC/OAuth2)

### Security Layer
- **ModSecurity WAF** - OWASP Top 10 protection via nginx
- **CrowdSec** - DDoS & brute-force detection with behavioral threat analysis
- **OPA Policies** - Fine-grained access control (RBAC/ABAC)
- **mTLS Certificates** - Service-to-service encryption

### Observability & Monitoring
- **Prometheus** - Metrics collection and alerting
- **Grafana** - Metrics visualization & dashboards
- **Loki** - Log aggregation & querying
- **Tempo** - Distributed tracing
- **Alertmanager** - Alert routing & notification

### Deployment Options
- **Docker Compose** - Development & testing
- **Kubernetes** - Production with Helm or native manifests
- **Terraform** - Infrastructure as Code for cloud providers

### Database Schema

| Database | Purpose | Tables |
|----------|---------|--------|
| `employmentvc_core` | Core platform | organizations, audit_log |
| `employmentvc_auth` | Auth service | account_lockouts, login_audit, rate_limit_sessions |
| `employmentvc_credential` | Credentials | credential_schemas, credentials, credential_revocation |
| `employmentvc_wallet` | Wallet data | wallets, wallet_keys, transactions |
| `employmentvc_did` | DID registry | dids, did_documents, did_operations |
| `employmentvc_issuer` | Issuer data | issuer_profiles, issued_credentials, signing_keys |
| `employmentvc_verifier` | Verifier data | verifier_profiles, verification_requests, verification_results |
| `keycloak` | Identity Mgmt | realms, users, roles, clients, federated_users |

---

## 🔧 Infrastructure Architecture

```
┌─────────────────────────────────────────────┐
│    API Layer (Spring Boot Services)         │
│  Auth │ Wallet │ Issuer │ Verifier │ Gateway│
└────────────────┬────────────────────────────┘
                 │
┌────────────────┴────────────────────────────┐
│  Security Layer (WAF + Access Control)      │
│  ModSecurity │ CrowdSec │ OPA │ mTLS        │
└────────────────┬────────────────────────────┘
                 │
┌────────────────┴────────────────────────────┐
│  Storage & State (Database + Blockchain)    │
│  PostgreSQL 15 │ Redis 7 │ Besu Node        │
└────────────────┬────────────────────────────┘
                 │
┌────────────────┴────────────────────────────┐
│  Observability (Prometheus + Loki)          │
│  Metrics │ Logs │ Traces │ Alerts           │
└─────────────────────────────────────────────┘
```

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- Docker & Docker Compose
- Gradle 8.5+

### Development Setup

1. **Clone and setup**
   ```bash
   git clone <repository-url>
   cd EmploymentVC
   ```

2. **Start infrastructure**
   ```bash
   # Recommended: Start integrated stack with all components
   ./scripts/start-platform-stack.sh
   
   # Optional: Include Vault and ELK stack
   ./scripts/start-platform-stack.sh --with-vault --with-elk
   
   # Verify infrastructure setup
   ./scripts/verify-infrastructure.sh
   ```

   Or use Docker Compose directly:
   ```bash
   COMPOSE_PROJECT_NAME=employmentvc docker compose up -d postgres redis besu-node keycloak
   ```

3. **Build and run backend services**
   ```bash
   ./gradlew build
   ./gradlew bootRun --parallel
   ```

4. **Start frontend**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

5. **Access the platform**
   - Frontend: http://localhost:3000
   - API Gateway: http://localhost:8080
   - API Documentation: http://localhost:8080/swagger-ui.html

## 📊 Service Ports

| Service | Port | Description |
|---------|------|-------------|
| Frontend | 3000 | Next.js web application |
| API Gateway | 8080 | Main API entry point |
| Auth Service | 8081 | Authentication service |
| DID Registry | 8082 | DID management |
| Schema Registry | 8083 | Credential schemas |
| **Application Service** | **8084** | **🌟 Core VC Operations** |
| | | **• Issuer Component** |
| | | **• Verifier Component** |
| | | **• Custodial Wallet** |
| | | **• Non-Custodial Wallet** |

## 🔧 Development

### Backend Development
```bash
# Run specific service
./gradlew :application-service:bootRun

# Run tests
./gradlew test

# Code quality checks
./gradlew checkstyleMain spotbugsMain
```

### Frontend Development
```bash
cd frontend

# Development server
npm run dev

# Build for production
npm run build

# Run tests
npm test
```

## � Security Setup

The platform includes comprehensive security infrastructure. For complete security setup:

```bash
# One-command security setup
bash security/setup-security.sh development

# Or for production with Vault
bash security/setup-security.sh production --vault-addr https://vault.prod.example.com
```

**Key Security Components**:
- **TLS/mTLS Certificates** - Automated generation and management
- **OPA Policies** - Fine-grained access control (RBAC/ABAC)
- **Vault Integration** - Centralized secret management
- **Security Testing** - SAST, DAST, dependency scanning
- **Threat Modeling** - GDPR, SOC 2, ISO 27001 compliance
- **SBOM Generation** - Supply chain security tracking

See [**Security Documentation**](security/README.md) for details.

## 📚 Documentation

### Security & Compliance
- [**Security README**](security/README.md) - Security infrastructure overview
- [**Security Implementation Guide**](security/IMPLEMENTATION.md) - Complete setup & integration guide
- [**Responsible Disclosure**](security.md) - Vulnerability reporting policy
- [**Threat Model**](security/threat-models/threat-model.md) - Risk assessment & compliance roadmap
- [**Deployment Checklist**](security/DEPLOYMENT_CHECKLIST.md) - Pre-deployment verification steps

### Infrastructure & Deployment
- [**Infrastructure Integration Guide**](INFRASTRUCTURE_INTEGRATION.md) - Complete guide to infrastructure & security layers
- [**Infrastructure README**](infra/README.md) - Detailed infra component documentation

### Application & Architecture
- [API Documentation](docs/API.md)
- [Architecture Guide](docs/ARCHITECTURE.md)
- [Deployment Guide](docs/DEPLOYMENT.md)
- [Web3 Integration](docs/WEB3_INTEGRATION.md)

## 🔐 Features

- **Multi-Method Authentication** - Traditional, Web3 wallets, DID-based
- **EBSI Integration** - European Blockchain Services Infrastructure
- **Selective Disclosure** - Privacy-preserving credential sharing
- **Custodial & Non-Custodial Wallets** - Flexible credential storage
- **JSON-LD Support** - Semantic interoperability
- **Enterprise Security** - Production-ready security features

## 🚀 Deployment

### Docker Compose (Development)
```bash
COMPOSE_PROJECT_NAME=employmentvc docker compose up -d
COMPOSE_PROJECT_NAME=employmentvc docker compose ps
COMPOSE_PROJECT_NAME=employmentvc docker compose down
```

### Kubernetes (Production)
```bash
kubectl apply -f k8s/
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
