# Security Components Implementation Guide

Complete guide to EmploymentVC security components and their integration with the platform.

## Directory Structure

```
security/
├── README.md                           # Security overview
├── certificates/                       # TLS/mTLS certificate management
│   └── generate-certs.sh              # Certificate generation script
├── opa-policies/                       # Access control policies
│   ├── access.rego                    # RBAC and API access control
│   ├── issuance.rego                  # Credential issuance rules
│   ├── verification.rego              # Credential verification rules
│   ├── wallet.rego                    # Wallet operation rules
│   └── data.rego                      # Policy data and definitions
├── sbom/                               # Software Bill of Materials
│   ├── generate-sbom.sh               # SBOM generation script
│   └── SBOM-REPORT-*.md               # Generated SBOM reports
├── security-tests/                     # Security testing
│   ├── run-security-tests.sh          # Test execution script
│   ├── sast/                          # Static analysis results
│   ├── dast/                          # Dynamic analysis config
│   ├── dependency/                    # Dependency scanning
│   └── reports/                       # Test reports
├── threat-models/                      # Threat assessments
│   └── threat-model.md                # Complete threat model
└── vault-config/                       # Vault configuration
    ├── default-policy.hcl             # Vault policies
    └── setup-vault.sh                 # Vault setup script
```

## Quick Start

### 1. Generate Certificates

For development:
```bash
bash security/certificates/generate-certs.sh /tmp/certs development
```

For production:
```bash
bash security/certificates/generate-certs.sh /etc/employmentvc/certs production 730
```

This creates:
- Root CA (ca.crt, ca.key)
- Server certificate (server.crt, server.key)
- Client certificate (client.crt, client.key)
- Service mTLS certificates (per service)

### 2. Configure Vault

```bash
# Start Vault (if using Docker)
docker run -d --cap-add IPC_LOCK \
  -e VAULT_DEV_ROOT_TOKEN_ID=mytoken \
  -p 8200:8200 \
  vault:latest

# Configure Vault
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=mytoken
bash security/vault-config/setup-vault.sh
```

Vault stores:
- Database credentials
- API keys (Keycloak, Blockchain)
- Encryption keys
- Signing keys
- Service secrets

### 3. Run Security Tests

```bash
# Full security scan
bash security/security-tests/run-security-tests.sh

# SAST only
./gradlew spotbugsMain checkstyleMain

# Dependency scan
grype ./

# Generate SBOM
bash security/sbom/generate-sbom.sh
```

### 4. Deploy OPA Policies

```bash
# Start OPA server
docker run -d -p 8181:8181 openpolicyagent/opa:latest run --server

# Load policies
for policy in security/opa-policies/*.rego; do
  curl -X PUT http://localhost:8181/v1/policies/$(basename $policy .rego) \
    --data-binary @$policy \
    -H "Content-Type: application/x-rego"
done

# Test policy evaluation
curl -X POST http://localhost:8181/v1/data/access \
  -d '{
    "user": {"authenticated": true, "role": "issuer"},
    "request": {"path": "/api/credentials/issue", "method": "POST"}
  }'
```

## Component Details

### Certificates

**Files Generated**:
- `ca/{ca.crt, ca.key}` - Root Certificate Authority
- `server/{server.crt, server.key, chain.pem}` - API Gateway certificates
- `client/{client.crt, client.key}` - Client application certificate
- `mtls/{service}.crt {service}.key` - Service-to-service mTLS certificates

**Usage**:
- nginx: Use `server/{cert.pem, key.pem}`
- Kubernetes: `kubectl create secret tls provenly-tls --cert=cert.pem --key=key.pem`
- Services: Mount individual service certificates

**Rotation**:
- Development: Yearly
- Staging: 6 months
- Production: Every 90 days

### OPA Policies

**Policy Packages**:

1. **access** - API-level access control
   - User authentication verification
   - Role-based access control (RBAC)
   - Permission checking
   - Rate limiting

2. **issuance** - Credential issuance rules
   - Issuer authorization
   - Schema validation
   - Required claims checking
   - Revocation list checking

3. **verification** - Credential verification
   - Verifier authorization
   - Credential expiration checking
   - Revocation verification
   - Issuer trust verification
   - Signature validation
   - Challenge verification

4. **wallet** - Wallet operation control
   - Wallet ownership verification
   - Operation type authorization
   - Key security thresholds
   - Credential storage rules

5. **data** - Shared data and rules
   - Trusted issuers list
   - Revoked credentials
   - Rate limit definitions
   - Role permissions mapping
   - Compliance requirements

**Integration in Services**:

```java
// Example: Spring Boot integration
@Service
public class OpaAuthorizationService {
    private final OpaEvaluator opaEvaluator;
    
    public boolean canIssueCredential(IssuanceRequest request) {
        DecisionResult result = opaEvaluator.evaluate(
            "data.issuance.allow",
            request
        );
        return result.allowed;
    }
}
```

### SBOM (Software Bill of Materials)

**Purpose**: Track all dependencies for supply chain security

**Contents**:
- Java/Kotlin libraries (via Gradle)
- Node.js packages (via npm)
- System services (PostgreSQL, Redis, Besu, etc.)
- Licenses and compliance info
- Known vulnerabilities

**Files**:
- `bom-java-*.json` - CycloneDX format for Java deps
- `nodejs-packages-*.txt` - Node.js package list
- `npm-audit-*.json` - JavaScript vulnerabilities
- `SBOM-REPORT-*.md` - Human-readable summary

**Integration with CI/CD**:
```yaml
# GitHub Actions example
- name: Generate SBOM
  run: bash security/sbom/generate-sbom.sh

- name: Upload SBOM
  uses: actions/upload-artifact@v3
  with:
    name: sbom-reports
    path: security/sbom/
```

### Security Tests

**SAST (Static Analysis)**:
- Spotbugs: Java bytecode analysis
- Checkstyle: Code style and quality
- PMD: Code smell detection
- Results: `build/reports/spotbugs/`, `build/reports/checkstyle/`

**DAST (Dynamic Analysis)**:
- OWASP ZAP: Web application scanning
- Configuration: `security/security-tests/dast/zap-config.xml`
- Target: Staging environment only

**Dependency Scanning**:
- Grype: CVE database scanning
- Trivy: Container and filesystem scanning
- Results: JSON reports with vulnerability details

### Threat Model

**Key Threats Addressed**:
1. Authentication bypass - MFA, OPA enforcement
2. Data breach - Encryption, access control
3. DDoS - CrowdSec, rate limiting
4. Credential forgery - Digital signatures
5. Key exposure - Vault secret management

**Controls Documented**:
- Technical controls (TLS, encryption, WAF)
- Administrative controls (policies, procedures)
- Operational controls (monitoring, incident response)
- Physical controls (data center security)

### Vault Configuration

**Secrets Stored**:
- Database: PostgreSQL User/PW
- Cache: Redis password
- Blockchain: RPC endpoint URLs
- Identity: Keycloak client credentials
- Crypto: Master keys for encryption

**Auth Methods**:
- Kubernetes: Pod identity authentication
- JWT: Token-based authentication
- AppRole: Service-to-service auth

**Secret Engines**:
- KV v2: General secrets storage
- PKI: Certificate generation
- Database: Dynamic DB credentials
- SSH: SSH key management

## Integration with Infrastructure

### Docker Compose Deployment

```yaml
# Use generated certificates
volumes:
  - ./security/certificates/server/cert.pem:/etc/nginx/ssl/cert.pem:ro
  - ./security/certificates/server/key.pem:/etc/nginx/ssl/key.pem:ro

# Run security tests
services:
  security-check:
    image: ubuntu:latest
    volumes:
      - .:/project
    command: bash /project/security/security-tests/run-security-tests.sh
```

### Kubernetes Deployment

```yaml
# Create TLS secret
kubectl create secret tls provenly-tls \
  --cert=security/certificates/server/server.crt \
  --key=security/certificates/server/server.key \
  -n provenly

# Create OPA ConfigMap
kubectl create configmap opa-policies \
  --from-file=security/opa-policies/ \
  -n provenly

# Mount in deployment
volumeMounts:
  - name: opa-policies
    mountPath: /etc/opa/policies
```

### GitHub Actions CI/CD Integration

```yaml
name: Security Checks

on: [push, pull_request]

jobs:
  security:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Generate SBOM
        run: bash security/sbom/generate-sbom.sh
      
      - name: Run Security Tests
        run: bash security/security-tests/run-security-tests.sh
      
      - name: Upload Reports
        uses: actions/upload-artifact@v3
        with:
          name: security-reports
          path: security/
```

## Security Checklist

### Pre-Deployment
- [ ] Generate production certificates (90+ day validity)
- [ ] Configure Vault with all secrets
- [ ] Deploy OPA policies
- [ ] Run full security test suite
- [ ] Complete threat model review
- [ ] Verify SBOM accuracy
- [ ] Enable audit logging in Postgres
- [ ] Configure WAF (ModSecurity) rules

### Post-Deployment
- [ ] Set up Prometheus security alerts
- [ ] Configure log rotation in Loki
- [ ] Test certificate renewal procedures
- [ ] Verify TLS in browser (HSTS headers)
- [ ] Confirm mTLS between services
- [ ] Run OPA policy tests
- [ ] Set up incident response contacts
- [ ] Schedule security review (quarterly)

## Monitoring & Maintenance

### Weekly
- [ ] Review Prometheus security alerts
- [ ] Check Loki logs for anomalies
- [ ] Monitor ModSecurity/CrowdSec blocks

### Monthly
- [ ] Update vulnerability definitions
- [ ] Review access control logs
- [ ] Test certificate renewal
- [ ] Update SBOM reports

### Quarterly
- [ ] Full security audit
- [ ] Penetration testing (third-party)
- [ ] Policy review & updates
- [ ] Incident response drill

### Annually
- [ ] Third-party security assessment
- [ ] Compliance audit (GDPR, SOC 2)
- [ ] Update threat model
- [ ] Review and update this guide

## Resources

- [OPA Documentation](https://www.openpolicyagent.org/docs/)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)
- [CIS Controls](https://www.cisecurity.org/cis-controls/)
- [GDPR Compliance](https://gdpr-info.eu/)

## Support & Questions

For security-related questions:
- GitHub Issues: Use `[SECURITY]` prefix
- Email: security@provenly.io
- Bug Bounty: See [security.md](../security.md)

---

**Version**: 1.0  
**Last Updated**: 2026-02-21  
**Maintainer**: Security Team
