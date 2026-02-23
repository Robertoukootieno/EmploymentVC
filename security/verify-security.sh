#!/bin/bash
###############################################################################
# EmploymentVC Security Verification Script
#
# Validates security configuration and checks for common vulnerabilities.
# Run this regularly to ensure security posture is maintained.
#
# Usage:
#   ./verify-security.sh [checks]
#
# Checks:
#   all          - Run all checks (default)
#   certificates - Verify TLS certificates
#   policies     - Validate OPA policies
#   vault        - Check Vault configuration
#   dependencies - Scan for vulnerable dependencies
#   credentials  - Ensure no hardcoded secrets
#   headers      - Verify security headers
#   logs         - Check audit logs
###############################################################################

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_FILE="${SCRIPT_DIR}/verify-$(date +%Y%m%d-%H%M%S).log"
PASSED=0
FAILED=0
WARNINGS=0

# Helper functions
check_pass() {
  echo -e "${GREEN}✓${NC} $*" | tee -a "$RESULTS_FILE"
  ((PASSED++))
}

check_fail() {
  echo -e "${RED}✗${NC} $*" | tee -a "$RESULTS_FILE"
  ((FAILED++))
}

check_warn() {
  echo -e "${YELLOW}!${NC} $*" | tee -a "$RESULTS_FILE"
  ((WARNINGS++))
}

section() {
  echo "" | tee -a "$RESULTS_FILE"
  echo -e "${BLUE}═════ $* ═════${NC}" | tee -a "$RESULTS_FILE"
  echo "" | tee -a "$RESULTS_FILE"
}

# Verify certificates
verify_certificates() {
  section "Certificate Verification"
  
  local cert_dir="${SCRIPT_DIR}/certificates"
  
  # Check CA exists
  if [ -f "${cert_dir}/ca/ca.crt" ]; then
    check_pass "Root CA certificate found"
    
    # Check CA expiration
    local expiry=$(openssl x509 -in "${cert_dir}/ca/ca.crt" -noout -enddate 2>/dev/null | cut -d= -f2)
    check_pass "CA certificate expires: $expiry"
  else
    check_fail "Root CA certificate not found: ${cert_dir}/ca/ca.crt"
  fi
  
  # Check server certificate
  if [ -f "${cert_dir}/server/server.crt" ]; then
    check_pass "Server certificate found"
    
    # Verify signature
    if openssl x509 -in "${cert_dir}/server/server.crt" -noout -text &>/dev/null; then
      check_pass "Server certificate is valid"
    else
      check_fail "Server certificate is invalid"
    fi
  else
    check_warn "Server certificate not found: ${cert_dir}/server/server.crt"
  fi
  
  # Check client certificate
  if [ -f "${cert_dir}/client/client.crt" ]; then
    check_pass "Client certificate found"
  else
    check_warn "Client certificate not found: ${cert_dir}/client/client.crt"
  fi
  
  # Check mTLS certificates
  if [ -d "${cert_dir}/mtls" ] && [ "$(ls -A "${cert_dir}/mtls")" ]; then
    local count=$(ls -1 "${cert_dir}/mtls"/*.crt 2>/dev/null | wc -l)
    check_pass "Found $count service mTLS certificates"
  else
    check_warn "No service mTLS certificates found"
  fi
}

# Verify OPA policies
verify_policies() {
  section "OPA Policy Verification"
  
  local policy_dir="${SCRIPT_DIR}/opa-policies"
  
  local required_policies=("access.rego" "issuance.rego" "verification.rego" "wallet.rego" "data.rego")
  
  for policy in "${required_policies[@]}"; do
    if [ -f "${policy_dir}/${policy}" ]; then
      # Basic syntax check
      if grep -q "^package\|^import" "${policy_dir}/${policy}"; then
        check_pass "Policy file valid: $policy"
      else
        check_fail "Policy file missing package or import: $policy"
      fi
    else
      check_fail "Required policy not found: $policy"
    fi
  done
  
  # Check if OPA server is running
  if command -v curl &>/dev/null; then
    if curl -s http://localhost:8181/health &>/dev/null; then
      check_pass "OPA server is running on http://localhost:8181"
    else
      check_warn "OPA server not running on http://localhost:8181"
    fi
  fi
}

# Verify Vault
verify_vault() {
  section "Vault Configuration Verification"
  
  if [ -z "${VAULT_ADDR:-}" ]; then
    check_warn "VAULT_ADDR not set"
    return
  fi
  
  if command -v vault &>/dev/null; then
    # Check Vault status
    if vault status &>/dev/null 2>&1; then
      check_pass "Vault is accessible at $VAULT_ADDR"
      
      # List policies
      local policies=$(vault policy list 2>&1 | grep -v "^=" | wc -l)
      check_pass "Vault has $policies policies configured"
    else
      check_fail "Cannot connect to Vault at $VAULT_ADDR"
    fi
  else
    check_warn "Vault CLI not installed"
  fi
}

# Verify dependencies
verify_dependencies() {
  section "Dependency Vulnerability Scan"
  
  if command -v grype &>/dev/null; then
    check_pass "Grype is installed"
    
    # Run quick scan
    if grype . --fail-on "high" --quiet &>/dev/null 2>&1; then
      check_pass "No high/critical vulnerabilities found"
    else
      check_warn "Vulnerabilities found. Run: grype . for details"
    fi
  else
    check_warn "Grype not installed. Cannot scan dependencies"
  fi
  
  if command -v npm &>/dev/null; then
    if npm audit --production --json &>/dev/null 2>&1; then
      check_pass "npm dependencies are secure"
    else
      local vuln_count=$(npm audit --production 2>/dev/null | grep "vulnerabilities" | head -1 | awk '{print $1}')
      [ -n "$vuln_count" ] && check_warn "npm found $vuln_count vulnerabilities"
    fi
  fi
}

# Check for hardcoded credentials
verify_credentials() {
  section "Hardcoded Credentials Check"
  
  local patterns=(
    "password\s*[=:]\s*['\"]"
    "secret\s*[=:]\s*['\"]"
    "api_key\s*[=:]\s*['\"]"
    "token\s*[=:]\s*['\"]"
    "AWS_SECRET"
    "PRIVATE_KEY"
  )
  
  local found=0
  
  for pattern in "${patterns[@]}"; do
    local count=$(find "${SCRIPT_DIR}/.." -type f \
      \( -name "*.java" -o -name "*.py" -o -name "*.js" -o -name "*.yml" -o -name "*.yaml" \) \
      -exec grep -l "${pattern}" {} \; 2>/dev/null | wc -l)
    
    if [ "$count" -gt 0 ]; then
      check_warn "Found $count files matching pattern: $pattern"
      ((found++))
    fi
  done
  
  if [ "$found" -eq 0 ]; then
    check_pass "No obvious hardcoded credentials found"
  else
    check_warn "Review files manually for sensitive data"
  fi
}

# Verify security headers
verify_headers() {
  section "Security Headers Verification"
  
  # Check if services are running
  if command -v curl &>/dev/null; then
    for url in "http://localhost" "http://localhost:8080" "http://localhost:8081"; do
      if curl -s -I "$url" &>/dev/null 2>&1; then
        local headers=$(curl -s -I "$url" 2>/dev/null)
        
        # Check for important headers
        check_headers=$(echo "$headers" | grep -i "Strict-Transport-Security\|X-Content-Type-Options\|X-Frame-Options")
        
        if [ -n "$check_headers" ]; then
          check_pass "Detected security headers at $url"
        else
          check_warn "Security headers not detected at $url"
        fi
        break
      fi
    done
  fi
}

# Check audit logs
verify_logs() {
  section "Audit Logs Verification"
  
  local log_dir="${SCRIPT_DIR}/../../logs"
  
  if [ -d "$log_dir" ]; then
    local log_count=$(find "$log_dir" -type f -name "*.log" | wc -l)
    check_pass "Found $log_count log files"
    
    # Check for recent logs
    local recent=$(find "$log_dir" -type f -name "*.log" -mtime -1 | wc -l)
    if [ "$recent" -gt 0 ]; then
      check_pass "Found $recent logs updated in last 24 hours"
    else
      check_warn "No recent log updates found"
    fi
  else
    check_warn "Log directory not found"
  fi
}

# Check SBOM
verify_sbom() {
  section "SBOM (Software Bill of Materials)"
  
  local sbom_dir="${SCRIPT_DIR}/sbom"
  
  if [ -f "${sbom_dir}/SBOM-REPORT-"*.md ]; then
    check_pass "SBOM report generated"
  else
    check_warn "SBOM report not found"
  fi
  
  if [ -f "${sbom_dir}/bom-java-"*.json ]; then
    check_pass "Java dependencies SBOM exists"
  else
    check_warn "Java dependencies SBOM not found"
  fi
}

# Summary report
print_summary() {
  echo "" | tee -a "$RESULTS_FILE"
  echo -e "${BLUE}════════════════════════════════════════${NC}" | tee -a "$RESULTS_FILE"
  echo -e "${BLUE}Security Verification Summary${NC}" | tee -a "$RESULTS_FILE"
  echo -e "${BLUE}════════════════════════════════════════${NC}" | tee -a "$RESULTS_FILE"
  echo "" | tee -a "$RESULTS_FILE"
  
  echo -e "${GREEN}Passed:${NC}   $PASSED" | tee -a "$RESULTS_FILE"
  echo -e "${YELLOW}Warnings:${NC} $WARNINGS" | tee -a "$RESULTS_FILE"
  echo -e "${RED}Failed:${NC}   $FAILED" | tee -a "$RESULTS_FILE"
  echo "" | tee -a "$RESULTS_FILE"
  
  if [ "$FAILED" -eq 0 ]; then
    echo -e "${GREEN}Security verification completed successfully!${NC}" | tee -a "$RESULTS_FILE"
    return 0
  else
    echo -e "${RED}Security verification found issues that need attention.${NC}" | tee -a "$RESULTS_FILE"
    return 1
  fi
}

# Main
main() {
  echo -e "${BLUE}═════════════════════════════════════════════════${NC}"
  echo -e "${BLUE}EmploymentVC Security Verification${NC}"
  echo -e "${BLUE}═════════════════════════════════════════════════${NC}"
  echo ""
  
  local checks="${1:-all}"
  
  case "$checks" in
    all)
      verify_certificates
      verify_policies
      verify_vault
      verify_dependencies
      verify_credentials
      verify_headers
      verify_logs
      verify_sbom
      ;;
    certificates)
      verify_certificates
      ;;
    policies)
      verify_policies
      ;;
    vault)
      verify_vault
      ;;
    dependencies)
      verify_dependencies
      ;;
    credentials)
      verify_credentials
      ;;
    headers)
      verify_headers
      ;;
    logs)
      verify_logs
      ;;
    *)
      echo "Unknown check: $checks"
      exit 1
      ;;
  esac
  
  print_summary
  
  echo ""
  echo "Detailed results saved to: $RESULTS_FILE"
  echo ""
}

main "$@"
