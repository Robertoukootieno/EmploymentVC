# EmploymentVC Security Repository

Complete security infrastructure for the EmploymentVC platform, including certificate management, access control policies, secret management, security testing, and threat assessment.

## 📋 Quick Navigation

- **[SECURITY_TESTING_GUIDE.md](./SECURITY_TESTING_GUIDE.md)** - Step-by-step testing process (START HERE)
- **[IMPLEMENTATION.md](./IMPLEMENTATION.md)** - Complete setup and integration guide
- **[../security.md](../security.md)** - Responsible disclosure policy
- **[threat-models/threat-model.md](./threat-models/threat-model.md)** - Risk assessment & compliance

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────┐
│             API Gateway (TLS Termination)           │
│         ModSecurity WAF | CrowdSec DDoS Guard       │
└────────────────┬────────────────────────────────────┘
                 │
        ┌────────┴────────┐
        │                 │
    ┌───┴────────┐   ┌───┴────────┐
    │  Service A │   │  Service B │  (mTLS)
    │  (Auth)    │   │  (Wallet)  │
    └───┬────────┘   └───┬────────┘
        │                 │
        └────────┬────────┘
                 │
        ┌────────┴────────────────────┐
        │    OPA Authorization        │
        │  (access, issuance, verify) │
        └────────┬────────────────────┘
                 │
        ┌────────┴────────────────────┐
        │  Vault Secret Management    │
        │  (credentials, keys, certs) │
        └─────────────────────────────┘
```

## 🔒 Core Components

### 1. Certificate Management (`certificates/`)
TLS and mTLS certificate infrastructure supporting multiple deployment environments.

- **Script**: `generate-certs.sh` - Automated certificate generation
- **Directories**: CA, server, client, mTLS per-service certs
- **Features**: Multi-environment support, chain validation, expiration monitoring

```bash
# Development (90-day certs)
./certificates/generate-certs.sh ./certificates development

# Production (730-day certs)
./certificates/generate-certs.sh ./certificates production 730
```

### 2. Access Control - OPA Policies (`opa-policies/`)
Fine-grained access control using Open Policy Agent (OPA).

**Roles**:
- `super_admin` - Full system access
- `security_admin` - Security configuration
- `employment_issuer` - Issue employment credentials
- `employment_verifier` - Verify employment credentials
- `holder` - Manage credentials

**Policies**:
- `access.rego` - API endpoint authorization
- `issuance.rego` - Credential issuance rules
- `verification.rego` - Credential verification
- `wallet.rego` - Wallet operation control
- `data.rego` - Shared data and rules (NEW)

### 3. Secret Management - Vault (`vault-config/`)
HashiCorp Vault integration for secrets.

- **Policy**: `default-policy.hcl` - Access control
- **Setup**: `setup-vault.sh` - Automated initialization
- **Secrets**: Database creds, API keys, encryption keys, certificates

```bash
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=your-token
./vault-config/setup-vault.sh
```

### 4. Security Testing (`security-tests/`)
Automated security testing and vulnerability scanning.

**SAST** (Spotbugs, Checkstyle, PMD)
**DAST** (OWASP ZAP)
**Dependency Scanning** (Grype, Trivy, npm audit)

```bash
./security-tests/run-security-tests.sh
```

### 5. Threat Modeling (`threat-models/`)
Comprehensive threat assessment and compliance mapping.

**Includes**:
- Asset identification
- Threat analysis (STRIDE)
- Risk assessment matrix
- Compliance mapping (GDPR, SOC 2, ISO 27001)
- Security roadmap (Q1-Q4 2026)
- Incident response procedures

### 6. SBOM - Software Bill of Materials (`sbom/`)
Supply chain security tracking.

**Formats**: CycloneDX 1.4, SPDX 2.3
**Coverage**: Java, Python, JavaScript, System services
**Tracking**: CVE identification, license compliance

```bash
./sbom/generate-sbom.sh
```

## 🚀 Quick Start

### Setup All Components
```bash
# Development environment
bash setup-security.sh development

# Staging environment
bash setup-security.sh staging

# Production environment
bash setup-security.sh production --vault-addr https://vault.prod.example.com
```

### Verify Configuration
```bash
bash verify-security.sh                # Run all checks
bash verify-security.sh certificates   # Check certificates only
bash verify-security.sh dependencies   # Scan for vulnerabilities
```

### Individual Component Setup
```bash
# Generate certificates only
./certificates/generate-certs.sh ./certificates development

# Setup Vault only
export VAULT_ADDR=http://localhost:8200
./vault-config/setup-vault.sh

# Run security tests only
./security-tests/run-security-tests.sh

# Generate SBOM only
./sbom/generate-sbom.sh
```

## 📊 Directory Structure

```
security/
├── README.md                           # This file
├── IMPLEMENTATION.md                   # Complete setup guide
├── setup-security.sh                   # Master orchestration script
├── verify-security.sh                  # Verification script
├── certificates/
│   ├── generate-certs.sh
│   ├── ca/                            # Root CA
│   ├── server/                        # API server certs
│   ├── client/                        # Client certs
│   └── mtls/                          # Service-to-service certs
├── opa-policies/
│   ├── access.rego                    # RBAC/ABAC
│   ├── issuance.rego                  # Credential rules
│   ├── verification.rego              # Verification rules
│   ├── wallet.rego                    # Wallet control
│   └── data.rego                      # Policy data
├── vault-config/
│   ├── default-policy.hcl
│   └── setup-vault.sh
├── security-tests/
│   ├── run-security-tests.sh
│   ├── dast/
│   ├── sast/
│   ├── dependency/
│   └── reports/
├── sbom/
│   ├── generate-sbom.sh
│   └── *.json, *.md (generated)
└── threat-models/
    └── threat-model.md
```

## 🔐 Key Features

✅ **Multi-environment support** (development, staging, production)
✅ **Automated certificate generation** with chain validation
✅ **OPA policy framework** with RBAC and ABAC
✅ **Vault integration** for secret management
✅ **Automated security testing** (SAST, DAST, dependency scanning)
✅ **Comprehensive threat modeling** with compliance mapping
✅ **SBOM generation** for supply chain security
✅ **Verification scripts** for ongoing validation
✅ **Docker and Kubernetes integration**
✅ **GitHub Actions CI/CD support**

## 🔧 Orchestration Scripts

### setup-security.sh
Master setup orchestrating all security components:
```bash
./setup-security.sh [environment] [options]

Options:
  --skip-certs          - Skip certificate generation
  --skip-vault          - Skip Vault setup
  --skip-tests          - Skip security tests
  --skip-sbom           - Skip SBOM generation
  --vault-addr URL      - Vault server address
  --vault-token TOKEN   - Vault authentication token
  --dry-run             - Show actions without executing
```

### verify-security.sh
Verification and compliance checking:
```bash
./verify-security.sh [checks]

Checks:
  all, certificates, policies, vault, dependencies, 
  credentials, headers, logs
```

## 📈 Integration Points

### Docker Compose
```yaml
volumes:
  - ./security/certificates/server/:/etc/nginx/ssl/:ro
  - ./security/opa-policies/:/etc/opa/policies/:ro
environment:
  - VAULT_ADDR=http://vault:8200
```

### Kubernetes
```bash
kubectl create secret tls provenly-tls \
  --cert=security/certificates/server/server.crt \
  --key=security/certificates/server/server.key

kubectl create configmap opa-policies \
  --from-file=security/opa-policies/
```

### GitHub Actions
```yaml
- name: Run Security Suite
  run: bash security/setup-security.sh staging --skip-vault

- name: Upload SBOM
  uses: actions/upload-artifact@v3
  with:
    name: sbom-reports
    path: security/sbom/
```

## 📚 Documentation

| Document | Purpose |
|----------|---------|
| [SECURITY_TESTING_GUIDE.md](./SECURITY_TESTING_GUIDE.md) | Step-by-step testing procedures (tested & validated) |
| [IMPLEMENTATION.md](./IMPLEMENTATION.md) | Complete setup and integration guide |
| [threat-models/threat-model.md](./threat-models/threat-model.md) | Risk assessment and compliance |
| [../security.md](../security.md) | Responsible disclosure policy |

## 🔔 Maintenance Schedule

- **Daily**: Monitor alerts and audit logs
- **Weekly**: Certificate expiration check, dependency updates
- **Monthly**: Full security audit, SBOM regeneration
- **Quarterly**: Threat model review, penetration testing
- **Annually**: Third-party security assessment

## 👥 Team Contacts

- **Security Issues**: security@provenly.io
- **GitHub Issues**: Use `[SECURITY]` prefix
- **Emergency**: security-team-oncall (PagerDuty)

---

**Status**: ✅ Production Ready  
**Version**: 1.0  
**Last Updated**: February 21, 2026  
**Next Review**: May 21, 2026
