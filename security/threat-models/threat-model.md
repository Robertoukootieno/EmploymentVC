# EmploymentVC Security Threat Model

## Asset Identification

### Critical Assets
1. **User Credentials** - Authentication tokens, passwords
2. **Verifiable Credentials** - Employment verification data
3. **Private Keys** - DID signing keys, wallet keys
4. **Database** - User data, credential records, audit logs
5. **Blockchain** - DID registry, credential proofs

### Asset Classification
- **High**: Credentials, private keys, PII
- **Medium**: Transaction data, audit logs
- **Low**: Public metadata, non-sensitive configuration

---

## Threat Identification

### Authentication & Access Control
**Threat**: Unauthorized access to credentials and verification data
- Attackers obtain valid tokens through phishing
- Weak password policies bypass authentication
- Session hijacking via HTTPS downgrade

**Controls**:
- Require MFA for sensitive operations
- TLS 1.3+ enforced
- Short session timeouts (15 min)
- Rate limiting on auth endpoints
- OPA policy enforcement

---

### Data Integrity
**Threat**: Modification of credential data
- Man-in-the-middle attacks
- SQL injection attacks
- Supply chain attacks (malicious dependencies)

**Controls**:
- Database audit logging
- Digital signatures on credentials
- mTLS between services
- Input validation & parameterized queries
- SBOM tracking & vulnerability scanning

---

### Data Confidentiality
**Threat**: Disclosure of sensitive employment data
- Unencrypted data at rest
- Unencrypted data in transit
- Improper access control
- Log files containing PII

**Controls**:
- AES-256 encryption at rest (PostgreSQL)
- TLS encryption in transit
- Field-level encryption for PII
- Log anonymization
- Selective disclosure in credentials

---

### Availability
**Threat**: Service disruption attacks
- DDoS attacks on API endpoints
- Resource exhaustion on blockchain
- Database connection pool exhaustion

**Controls**:
- CrowdSec DDoS detection
- Rate limiting (both application & WAF)
- Connection pooling limits
- Auto-scaling in Kubernetes
- Alertmanager notifications

---

### Blockchain-Specific Threats
**Threat**: Malicious smart contract interactions
- Unauthorized DID operations
- Credential forgery
- Private key exposure

**Controls**:
- Private blockchain (Besu with authorization)
- Smart contract formal verification
- Cryptographic proof verification
- Key rotation procedures
- Audit logging of all transactions

---

## Compliance Requirements

### GDPR
- [ ] Data minimization: Only collect necessary employment data
- [ ] Consent: Document user consent for credential storage
- [ ] Right to be forgotten: Implement credential revocation
- [ ] Data processing agreement (DPA): Establish with third parties
- [ ] Privacy impact assessment (DPIA): Complete before launch

### SOC 2 Type II
- [ ] Access controls: RBAC + OPA policies
- [ ] Audit logging: Enable Postgres audit
- [ ] Encryption: All PII encrypted
- [ ] Change management: Version control + approval workflows
- [ ] Incident response: Runbooks for security events

### ISO/IEC 27001
- [ ] Risk assessment: Document all identified threats
- [ ] Asset inventory: Maintain list of systems & data
- [ ] Security policies: Define and enforce
- [ ] Incident management: Establish procedures
- [ ] Continuous improvement: Regular security reviews

---

## Risk Assessment Matrix

| Threat | Likelihood | Impact | RiskLevel | Mitigation |
|--------|-----------|--------|-----------|-----------|
| Phishing attacks | High | High | Critical | MFA, security training |
| SQL Injection | Medium | Critical | Critical | Input validation, parameterized queries |
| DDoS attacks | Medium | High | High | CrowdSec, rate limiting |
| Insider threats | Low | Critical | High | Audit logging, access controls |
| Credential forgery | Low | Critical | High | Digital signatures, trusted issuers |
| Key exposure | Low | Critical | High | Vault secret management |
| Network data interception | Low | High | Medium | TLS 1.3, mTLS |
| Unauthorized access | Medium | High | High | OPA policies, RBAC |

---

## Security Testing Plan

### Static Analysis (SAST)
- Weekly Spotbugs + Checkstyle analysis
- Java security profile validation
- Dependency vulnerability scanning (Grype/Trivy)

### Dynamic Testing (DAST)
- Monthly OWASP ZAP scans against staging
- Authentication bypass testing
- Authorization testing
- Input validation testing

### Penetration Testing
- Quarterly third-party pentests
- Focus on API endpoints
- Blockchain interaction testing
- Social engineering assessment

### Compliance Audits
- Annual GDPR compliance review
- SOC 2 audit readiness
- ISO 27001 certification path
- Vulnerability disclosure policy review

---

## Incident Response Plan

### Discovery Phase
1. Alert system detection (Prometheus/Alertmanager)
2. Manual report via security contact
3. Log analysis via Loki

### Assessment Phase
- [ ] Confirm incident severity (Critical/High/Medium/Low)
- [ ] Identify affected systems
- [ ] Determine data exposure scope
- [ ] Activate incident response team

### Response Phase
- [ ] Isolate affected systems
- [ ] Preserve evidence for forensics
- [ ] Notify stakeholders
- [ ] Implement fixes/patches
- [ ] Document timeline

### Recovery Phase
- [ ] Restore from backups if needed
- [ ] Verify system integrity
- [ ] Re-enable monitoring
- [ ] Resume normal operations

### Post-Incident
- [ ] Conduct root cause analysis
- [ ] Update security controls
- [ ] Document lessons learned
- [ ] Update playbooks

---

## Security Roadmap

### Q1 2026
- [ ] Complete threat modeling (THIS DOCUMENT)
- [ ] Deploy ModSecurity WAF
- [ ] Enable Postgres audit logging
- [ ] Establish certificate rotation

### Q2 2026
- [ ] First round of pentesting
- [ ] OPA policy enforcement
- [ ] Kubernetes network policies
- [ ] SBOM integration in CI/CD

### Q3 2026
- [ ] SOC 2 audit readiness
- [ ] Disaster recovery testing
- [ ] Security awareness training
- [ ] Vendor security review

### Q4 2026
- [ ] GDPR compliance certification
- [ ] Bug bounty program launch
- [ ] Security certification (SOC 2)
- [ ] Annual security review

---

## Contact & Escalation

**Security Lead**: security@provenly.io
**Incident Hotline**: +1-XXX-SECURITY
**Bug Bounty**: security.md
**Public Disclosure**: Coordinated vulnerability disclosure

---

**Document Version**: 1.0  
**Last Updated**: 2026-02-21  
**Next Review**: 2026-05-21
