# EmploymentVC Security Testing Guide

Complete step-by-step guide for executing the security testing process successfully.

## 📋 Overview

This guide documents the tested and validated process for running the complete security verification suite for the EmploymentVC platform. Following these steps ensures all security components are properly initialized, configured, and verified.

  
**Prerequisites**: Docker, bash, curl, jq (optional)

---

## Phase 1: Prerequisites & Environment Setup

### Step 1.1: Verify Docker is Running
```bash
docker ps
```
Expected output: Shows running containers or empty table

### Step 1.2: Verify Required Scripts Are Executable
```bash
chmod +x /home/robert/EmploymentVC/security/{setup-security.sh,verify-security.sh,generate-certs.sh}
chmod +x /home/robert/EmploymentVC/security/vault-config/setup-vault.sh
chmod +x /home/robert/EmploymentVC/security/certificates/generate-certs.sh
```

### Step 1.3: Navigate to Project Directory
```bash
cd /home/robert/EmploymentVC
```

---

## Phase 2: Certificate Infrastructure

### Step 2.1: Generate Certificates (If Not Already Present)
```bash
bash security/certificates/generate-certs.sh /home/robert/EmploymentVC/security/certificates 90 development 
```

**Expected Output**:
```
✓ Root CA certificate generated
✓ Server certificate generated
✓ Client certificate generated
✓ Service mTLS certificates generated:
  - auth-service-mtls.crt
  - wallet-api-mtls.crt
  - issuer-api-mtls.crt
  - verifier-api-mtls.crt
  - api-gateway-mtls.crt
```

**Verify Certificates**:
```bash
bash security/verify-security.sh certificates
```

Expected results:
```
✓ Root CA certificate found
✓ CA certificate expires: May 25 23:34:41 2026 GMT
✓ Server certificate found
✓ Server certificate is valid
✓ Client certificate found
✓ Found 5 service mTLS certificates
```

---

## Phase 3: OP Policy Engine Setup

### Step 3.1: Start OPA Docker Container
```bash
docker run --rm -d --name opa \
  -p 8181:8181 \
  openpolicyagent/opa:latest \
  run --server --addr=0.0.0.0:8181
```

**Expected Output**:
```
Image pulled: openpolicyagent/opa:latest
Container running with ID: <container_id>
```

### Step 3.2: Verify OPA Health
```bash
curl -s http://localhost:8181/health
```

**Expected Output**:
```json
{}
```

(Empty JSON response indicates OPA is healthy and ready)

### Step 3.3: Verify OPA Policies
```bash
bash security/verify-security.sh policies
```

**Expected Output**:
```
✓ Policy file valid: access.rego
✓ Policy file valid: issuance.rego
✓ Policy file valid: verification.rego
✓ Policy file valid: wallet.rego
✓ Policy file valid: data.rego
✓ OPA server is running on http://localhost:8181
```

---

## Phase 4: HashiCorp Vault Setup

### Step 4.1: Start Vault in Development Mode
```bash
docker run --rm -d --name vault \
  -p 8200:8200 \
  -e 'VAULT_SKIP_VERIFY=true' \
  -v /tmp/vault-data:/vault/data \
  hashicorp/vault:latest server -dev -dev-root-token-id="root"
```

**Important**: Use `hashicorp/vault:latest` (not just `vault:latest`)

**Expected Output**:
```
Image: hashicorp/vault:latest
Container ID: <container_id>
```

### Step 4.2: Wait for Vault to Initialize
```bash
sleep 3 && curl -s -H "X-Vault-Token: root" http://localhost:8200/v1/sys/health
```

**Expected Output**:
```json
{
  "initialized": true,
  "sealed": false,
  "standby": false,
  "version": "1.21.3",
  ...
}
```

### Step 4.3: Initialize Environment Variables
```bash
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=root
```

**⚠️ Critical**: These variables must be exported in the **same shell session** before running verification scripts.

### Step 4.4: Verify Vault Configuration
```bash
bash security/verify-security.sh vault
```

**Expected Output**:
```
✓ Vault is accessible at http://localhost:8200
✓ Vault has 2 policies configured
```

---

## Phase 5: Dependency Vulnerability Scanning

### Step 5.1: Install Grype (If Not Already Installed)
```bash
mkdir -p ~/.local/bin
curl -sSfL https://raw.githubusercontent.com/anchore/grype/main/install.sh | sh -s -- -b ~/.local/bin
```

**Expected Output**:
```
[info] checking github for the current release tag
[info] using release tag='v0.109.0' version='0.109.0' os='linux' arch='amd64'
[info] installed /home/robert/.local/bin/grype
```

### Step 5.2: Update PATH for Grype
```bash
export PATH=~/.local/bin:$PATH
```

### Step 5.3: Verify Grype Installation
```bash
grype version
```

**Expected Output**:
```
0.109.0
```

---

## Phase 6: Comprehensive Security Verification

### Step 6.1: Run Full Security Verification Suite

**IMPORTANT**: Execute all environment variable exports in the same command:

```bash
export PATH=~/.local/bin:$PATH && \
export VAULT_ADDR=http://localhost:8200 && \
export VAULT_TOKEN=root && \
cd /home/robert/EmploymentVC && \
bash security/verify-security.sh all
```

This single command ensures:
1. ✅ Grype is found in PATH
2. ✅ Vault environment variables persist
3. ✅ Working directory is correct
4. ✅ All verifications run in proper context

### Step 6.2: Expected Output

```
═════════════════════════════════════════════════
EmploymentVC Security Verification
═════════════════════════════════════════════════

═════ Certificate Verification ═════

✓ Root CA certificate found
✓ CA certificate expires: May 25 23:34:41 2026 GMT
✓ Server certificate found
✓ Server certificate is valid
✓ Client certificate found
✓ Found 5 service mTLS certificates

═════ OPA Policy Verification ═════

✓ Policy file valid: access.rego
✓ Policy file valid: issuance.rego
✓ Policy file valid: verification.rego
✓ Policy file valid: wallet.rego
✓ Policy file valid: data.rego
✓ OPA server is running on http://localhost:8181

═════ Vault Configuration Verification ═════

✓ Vault is accessible at http://localhost:8200
✓ Vault has 2 policies configured

═════ Dependency Vulnerability Scan ═════

✓ Grype is installed
! Vulnerabilities found. Run: grype . for details

════════════════════════════════════════
Security Verification Summary
════════════════════════════════════════

Passed:   13
Warnings: 1
Failed:   0

Security verification completed successfully!

Detailed results saved to: /home/robert/EmploymentVC/security/verify-YYYYMMDD-HHMMSS.log
```

---

## Phase 7: Review Vulnerability Report (Optional)

### Step 7.1: View Summary of Vulnerabilities
```bash
export PATH=~/.local/bin:$PATH
cd /home/robert/EmploymentVC
grype . --quiet
```

Outputs only HIGH and CRITICAL severity vulnerabilities.

### Step 7.2: Generate Detailed Report
```bash
grype . -o json > security/grype-report.json
grype . -o table > security/grype-report.txt
```

### Step 7.3: Analyze Critical Issues
```bash
grype . -o json | jq '.matches[] | select(.vulnerability.severity == "Critical")'
```

---

## Phase 8: Alternative: Run Full Integration Test Suite

### Step 8.1: Execute Complete Security Test Suite
```bash
export PATH=~/.local/bin:$PATH && \
export VAULT_ADDR=http://localhost:8200 && \
export VAULT_TOKEN=root && \
bash security/security-tests/run-security-tests.sh /home/robert/EmploymentVC
```

**Includes**:
- SAST (Spotbugs, Checkstyle, PMD for Java)
- Dependency scanning (Grype, npm audit)
- SBOM generation (CycloneDX, SPDX)
- Threat model validation

**Output Location**: `security/security-tests/reports/`

---

## Phase 9: Finalize & Commit

### Step 9.1: Verify Logs Were Generated
```bash
ls -lh security/verify-*.log | tail -3
```

### Step 9.2: Commit Bug Fixes (If Not Already Done)
```bash
cd /home/robert/EmploymentVC
git add security/verify-security.sh
git commit -m "fix: verify-security.sh counter increment with set -e"
git push origin main
```

### Step 9.3: Archive Reports (Optional)
```bash
mkdir -p security/reports/$(date +%Y%m%d)
cp security/verify-*.log security/reports/$(date +%Y%m%d)/
cp security/grype-report.* security/reports/$(date +%Y%m%d)/
```

---

## 🔧 Troubleshooting

### Issue: "Cannot connect to Vault"
**Cause**: Vault container not running or VAULT_ADDR not set  
**Solution**:
```bash
docker ps | grep vault  # Check if running
docker logs vault        # Check logs
# Restart:
docker stop vault && docker rm vault  # Clean up
# Then re-run Step 4.1
```

### Issue: "Grype not found"
**Cause**: PATH not updated or Grype not installed  
**Solution**:
```bash
# Verify installation
ls -la ~/.local/bin/grype

# If missing, re-run Step 5.1
mkdir -p ~/.local/bin
curl -sSfL https://raw.githubusercontent.com/anchore/grype/main/install.sh | sh -s -- -b ~/.local/bin

# Update PATH in same session
export PATH=~/.local/bin:$PATH
```

### Issue: Verification script exits early
**Cause**: `set -e` flag with counter arithmetic  
**Solution**: Already fixed in current version. Verify with:
```bash
grep "PASSED=\$((PASSED + 1))" security/verify-security.sh
```

### Issue: "OPA server not responding"
**Cause**: Container crashed or port conflict  
**Solution**:
```bash
docker ps | grep opa
docker logs opa
# Restart:
docker stop opa && docker rm opa
docker run --rm -d --name opa -p 8181:8181 openpolicyagent/opa:latest run --server --addr=0.0.0.0:8181
```

---

## ✅ Success Verification Checklist

Use this checklist to verify successful completion:

- [ ] Certificates generated (CA expires ~1 year away)
- [ ] OPA container running on port 8181
- [ ] OPA health check returns `{}`
- [ ] All 5 OPA policy files valid
- [ ] Vault container running on port 8200
- [ ] Vault initialized and unsealed
- [ ] VAULT_ADDR and VAULT_TOKEN environment variables set
- [ ] Grype installed to ~/.local/bin
- [ ] Full verification completes (13 passed, ≤1 warnings, 0 failed)
- [ ] Log file generated in security/verify-*.log
- [ ] Vulnerability report generated (grype-report.*)

---

## 🔄 Continuous Monitoring

### Daily Checks
```bash
# Check certificate expiration
bash security/verify-security.sh certificates | grep expires

# Check OPA health
curl -s http://localhost:8181/health
```

### Weekly Verification
```bash
# Run full verification every week
bash security/verify-security.sh all
```

### Monthly Deep Scan
```bash
# Full test suite with reports
bash security/security-tests/run-security-tests.sh /home/robert/EmploymentVC
```

---

## 📚 Related Documentation

- [IMPLEMENTATION.md](./IMPLEMENTATION.md) - Detailed setup guide
- [threat-models/threat-model.md](./threat-models/threat-model.md) - Risk assessment
- [../security.md](../security.md) - Responsible disclosure policy

---

**Last Updated**: February 25, 2026  
**Status**: ✅ Verified & Validated  
**Next Review**: May 25, 2026
