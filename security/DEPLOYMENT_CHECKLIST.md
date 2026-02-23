# Security Deployment Checklist

Complete checklist for deploying EmploymentVC security infrastructure across development, staging, and production environments.

## Pre-Deployment Preparation

### Prerequisites
- [ ] All team members reviewed security policies
- [ ] Security team approved threat model
- [ ] Infrastructure team reviewed certificate requirements
- [ ] Compliance team reviewed GDPR/SOC 2 mappings
- [ ] Backup procedures verified

### Environment Preparation
- [ ] Development environment prepared
- [ ] Staging environment ready for testing
- [ ] Production environment isolated and secured
- [ ] Network connectivity verified
- [ ] Database backups created

### Tools & Access
- [ ] OpenSSL installed (version 1.1.1 or later)
- [ ] curl available for API testing
- [ ] Git configured with SSH keys
- [ ] Vault CLI installed (if using Vault)
- [ ] Docker CLI available (if using Docker)
- [ ] kubectl installed (if using Kubernetes)

## Development Environment Deployment

### Certificates
- [ ] Run: `bash security/certificates/generate-certs.sh security/certificates development`
- [ ] Verify output: `ls -la security/certificates/{ca,server,client,mtls}`
- [ ] Check certificate validity: `openssl x509 -in security/certificates/ca/ca.crt -noout -dates`
- [ ] Document certificate paths in deployment notes

### OPA Policies
- [ ] Verify all policy files exist: `ls security/opa-policies/*.rego`
- [ ] Review policy syntax: `opa fmt -d security/opa-policies/`
- [ ] Load policies to OPA server (if running):
  ```bash
  curl -X PUT http://localhost:8181/v1/policies/data \
    --data-binary @security/opa-policies/data.rego
  ```
- [ ] Test basic policy:
  ```bash
  curl -X POST http://localhost:8181/v1/data/access \
    -d '{"user": {"authenticated": true}}'
  ```

### Security Testing Setup
- [ ] Install security tools: `./gradlew spotbugsMain checkstyleMain`
- [ ] Configure OWASP ZAP (if using DAST)
- [ ] Set up Grype for dependency scanning
- [ ] Run initial test: `bash security/security-tests/run-security-tests.sh`
- [ ] Review test results in `security/security-tests/reports/`

### SBOM Generation
- [ ] Generate initial SBOM: `bash security/sbom/generate-sbom.sh`
- [ ] Verify output files: `ls security/sbom/`
- [ ] Review SBOM report: `cat security/sbom/SBOM-REPORT-*.md`
- [ ] Store SBOM baseline for comparison

### Verification
- [ ] Run complete verification: `bash security/verify-security.sh all`
- [ ] Fix any warnings or failures
- [ ] Document results: `cp security/verify-*.log deployment-logs/`

## Staging Environment Deployment

### Pre-Deployment
- [ ] Staging environment mirrors production configuration
- [ ] Network policies match production
- [ ] Database contains realistic test data (GDPR compliant)
- [ ] Monitoring/logging configured

### Certificates
- [ ] Generate 180-day staging certificates:
  ```bash
  bash security/certificates/generate-certs.sh \
    security/certificates staging
  ```
- [ ] Verify certificate chain: `openssl verify -CAfile ca.crt server.crt`
- [ ] Upload to staging infrastructure
- [ ] Test HTTPS endpoint for SSL/TLS errors

### Vault Setup (if applicable)
- [ ] Set Vault address: `export VAULT_ADDR=https://vault.staging.example.com`
- [ ] Obtain Vault token from ops team
- [ ] Run setup: `bash security/vault-config/setup-vault.sh`
- [ ] Verify policies: `vault policy list`
- [ ] Confirm secret engines: `vault secrets list`

### OPA Deployment
- [ ] Deploy OPA to staging cluster/server
- [ ] Load staging policies
- [ ] Configure OPA logging at INFO level
- [ ] Test policy decisions with staging data
- [ ] Monitor OPA latency metrics

### Security Testing in Staging
- [ ] Run full security test suite
- [ ] Execute DAST against staging endpoint
- [ ] Perform dependency scanning including transitive deps
- [ ] Run penetration testing with staging credentials
- [ ] Document any findings and remediation plans

### Load Testing (Security-Focused)
- [ ] Test authentication under load
- [ ] Verify rate limiting enforcement
- [ ] Confirm certificate validation during scale
- [ ] Monitor Vault for throttling issues

### Monitoring & Alerting
- [ ] Configure Prometheus security metrics
- [ ] Set up alerts for cert expiration
- [ ] Enable audit logging in Vault
- [ ] Verify log aggregation in Loki
- [ ] Test alert notifications

### Staging Verification
- [ ] Complete staging verification checklist: `bash security/verify-security.sh all`
- [ ] Obtain sign-off from security team
- [ ] Document staging deployment results

## Production Environment Deployment

### **⚠️ CRITICAL: All following steps require security team approval**

### Pre-Production Review
- [ ] Security team final review complete
- [ ] Compliance team confirmed GDPR/SOC 2 controls
- [ ] Legal team approved security disclosures
- [ ] CTO/CISO sign-off obtained
- [ ] Rollback plan documented and tested

### Certificates (Production)
- [ ] Generate 730-day production certificates:
  ```bash
  bash security/certificates/generate-certs.sh \
    security/certificates production 730
  ```
- [ ] Validate certificate chain with root CAs
- [ ] Install to production web servers/proxies
- [ ] Configure automated renewal (before 60-day expiration)
- [ ] Document certificate locations for all services

### Vault Production Setup
- [ ] Production Vault instance running and sealed/unsealed properly
- [ ] All team members with Vault tokens trained
- [ ] Unseal key procedures documented and backed up (offline)
- [ ] Audit logging enabled: `vault audit enable file file_path=/vault/logs/audit.log`
- [ ] Run setup: `bash security/vault-config/setup-vault.sh`
- [ ] Verify all database credentials stored
- [ ] Confirm API keys synchronized with services

### OPA Production Deployment
- [ ] Deploy OPA cluster (at least 2 replicas)
- [ ] Load all production policies
- [ ] Configure high availability setup
- [ ] Set up OPA backup and restore procedures
- [ ] Enable detailed logging for debugging

### TLS/mTLS Production Configuration
- [ ] Generate per-service certificates: `ls security/certificates/mtls/`
- [ ] Configure service-to-service TLS enforcement
- [ ] Verify mutual TLS works between services
- [ ] Test certificate validation failures

### Security Monitoring Production
- [ ] Enable all Prometheus security metrics
- [ ] Configure critical alerts (cert expiration, Vault sealed, etc.)
- [ ] Set up 24/7 security monitoring
- [ ] Test alert escalation procedures
- [ ] Verify SecurityCenter dashboard is operational

### Production Verification
- [ ] Full production environment verification: `bash security/verify-security.sh all`
- [ ] Automated daily verification scheduled
- [ ] All checks passing with zero failures
- [ ] Document production security baseline

### Operational Procedures
- [ ] Certificate rotation procedures documented
- [ ] Vault backup procedures tested
- [ ] Secret rotation procedures automated
- [ ] Incident response runbooks available
- [ ] On-call rotation established

### Post-Deployment (Production)
- [ ] 24-hour monitoring for anomalies
- [ ] Daily log review for security events
- [ ] Weekly security metrics review
- [ ] Document production deployment results
- [ ] Schedule post-incident review if needed

## Ongoing Maintenance

### Daily
- [ ] Monitor security alerts and logs
- [ ] Review authentication failures
- [ ] Check Vault audit logs for anomalies
- [ ] Monitor WAF (ModSecurity) blocks

### Weekly
- [ ] Verify certificate expiration (90+ days remaining)
- [ ] Check dependency vulnerability updates
- [ ] Review OPA policy decision times
- [ ] Update security metrics dashboard

### Monthly
- [ ] Regenerate SBOM and compare against baseline
- [ ] Conduct security configuration review
- [ ] Update threat model if needed
- [ ] Review and rotate non-production credentials
- [ ] Verify backup procedures

### Quarterly
- [ ] Full security audit
- [ ] Penetration testing engagement
- [ ] Update threat model
- [ ] Review and update disaster recovery plan
- [ ] Compliance assessment (GDPR, SOC 2)

### Annually
- [ ] Third-party security assessment
- [ ] Full compliance audit
- [ ] Update security roadmap
- [ ] Review all security documentation
- [ ] Update disaster recovery and incident response plans

## Rollback Procedures

### If Deployment Fails (Staging or Production)
1. [ ] Stop all deployment processes immediately
2. [ ] Restore previous certificate version (if needed)
3. [ ] Revert OPA policies to last known good
4. [ ] Restore Vault configuration from backup
5. [ ] Verify services connect successfully
6. [ ] Document root cause analysis
7. [ ] Schedule post-incident review

## Sign-Off & Documentation

### Development Deployment
- [ ] Deployment Date: _______________
- [ ] Deployed By: _______________
- [ ] Verified By: _______________
- [ ] Notes: _______________

### Staging Deployment
- [ ] Deployment Date: _______________
- [ ] Deployed By: _______________
- [ ] Security Review: _______________ (Date)
- [ ] Approved By (Security): _______________
- [ ] Issues Found: _______________
- [ ] Remediation Required: _______________

### Production Deployment
- [ ] Deployment Date: _______________
- [ ] Deployed By: _______________
- [ ] Verified By: _______________
- [ ] CTO/CISO Approval: _______________ (Date)
- [ ] Monitoring Confirmed: _______________
- [ ] Incident Response Team: _______________ (Ready/Not Ready)
- [ ] Notes: _______________

## Emergency Contacts

**Security Issues**: security@provenly.io  
**On-Call Security**: Check PagerDuty  
**CTO/CISO**: [Contact Info]  
**Incident Commander**: [Contact Info]  

---

**Document Version**: 1.0  
**Last Updated**: February 21, 2026  
**Next Review**: May 21, 2026  
**Policy Owner**: Security Team
