#!/bin/bash
###############################################################################
# EmploymentVC Security Setup Orchestration Script
# 
# Complete security infrastructure setup with certificates, Vault, OPA, and
# security testing framework initialization.
#
# Usage:
#   ./setup-security.sh [environment] [options]
#
# Environments:
#   development   - Local development with self-signed certs (90 days)
#   staging       - Staging environment (180 days)
#   production    - Production (730 days, requires external input)
#
# Options:
#   --skip-certs      - Skip certificate generation
#   --skip-vault      - Skip Vault setup
#   --skip-tests      - Skip security testing
#   --skip-sbom       - Skip SBOM generation
#   --vault-addr URL  - Vault server address (default: http://localhost:8200)
#   --vault-token TOKEN - Vault token (default: read from env)
#   --dry-run         - Show what would be executed (don't run)
#
# Examples:
#   ./setup-security.sh development
#   ./setup-security.sh production --vault-addr https://vault.prod.example.com
#   ./setup-security.sh staging --skip-vault --skip-tests
###############################################################################

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
ENVIRONMENT="${1:-development}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CERT_DIR="${SCRIPT_DIR}/certificates"
VAULT_DIR="${SCRIPT_DIR}/vault-config"
TEST_DIR="${SCRIPT_DIR}/security-tests"
SBOM_DIR="${SCRIPT_DIR}/sbom"
LOG_FILE="${SCRIPT_DIR}/setup-$(date +%Y%m%d-%H%M%S).log"

# Defaults
SKIP_CERTS=false
SKIP_VAULT=false
SKIP_TESTS=false
SKIP_SBOM=false
DRY_RUN=false
CERT_VALIDITY=90
VAULT_ADDR="${VAULT_ADDR:-http://localhost:8200}"

# Parse options
while [[ $# -gt 1 ]]; do
  case "$2" in
    --skip-certs)
      SKIP_CERTS=true
      shift
      ;;
    --skip-vault)
      SKIP_VAULT=true
      shift
      ;;
    --skip-tests)
      SKIP_TESTS=true
      shift
      ;;
    --skip-sbom)
      SKIP_SBOM=true
      shift
      ;;
    --vault-addr)
      VAULT_ADDR="$3"
      shift 2
      ;;
    --vault-token)
      export VAULT_TOKEN="$3"
      shift 2
      ;;
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    *)
      echo "Unknown option: $2"
      exit 1
      ;;
  esac
done

# Helper functions
log() {
  echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*" | tee -a "$LOG_FILE"
}

success() {
  echo -e "${GREEN}✓${NC} $*" | tee -a "$LOG_FILE"
}

error() {
  echo -e "${RED}✗${NC} $*" | tee -a "$LOG_FILE"
}

warning() {
  echo -e "${YELLOW}!${NC} $*" | tee -a "$LOG_FILE"
}

run_cmd() {
  if [ "$DRY_RUN" = true ]; then
    echo "[DRY-RUN] $*"
  else
    log "Running: $*"
    "$@" | tee -a "$LOG_FILE"
  fi
}

# Validate environment
validate_environment() {
  log "Validating environment..."
  
  case "$ENVIRONMENT" in
    development)
      CERT_VALIDITY=90
      success "Environment: Development (90-day certificates)"
      ;;
    staging)
      CERT_VALIDITY=180
      success "Environment: Staging (180-day certificates)"
      ;;
    production)
      CERT_VALIDITY=730
      warning "Environment: Production (730-day certificates)"
      warning "Ensure you have proper PKI infrastructure in place"
      ;;
    *)
      error "Unknown environment: $ENVIRONMENT"
      error "Supported: development, staging, production"
      exit 1
      ;;
  esac
}

# Validate prerequisites
validate_prerequisites() {
  log "Checking prerequisites..."
  
  local missing=()
  
  if [ "$SKIP_CERTS" = false ]; then
    command -v openssl &>/dev/null || missing+=("openssl")
  fi
  
  if [ "$SKIP_TESTS" = false ]; then
    command -v grype &>/dev/null || missing+=("grype")
  fi
  
  if [ "$SKIP_VAULT" = false ] && [ -z "${VAULT_TOKEN:-}" ]; then
    if [ -z "${VAULT_SKIP_VERIFY:-}" ]; then
      warning "VAULT_TOKEN not set. Vault setup will be skipped."
      SKIP_VAULT=true
    fi
  fi
  
  if [ ${#missing[@]} -eq 0 ]; then
    success "All prerequisites validated"
  else
    warning "Some tools are missing: ${missing[*]}"
    warning "Install them before running full security setup"
  fi
}

# Generate certificates
setup_certificates() {
  log "Setting up TLS/mTLS certificates..."
  
  if [ "$SKIP_CERTS" = true ]; then
    warning "Skipping certificate generation (--skip-certs)"
    return
  fi
  
  if [ ! -f "${CERT_DIR}/generate-certs.sh" ]; then
    error "Certificate generation script not found: ${CERT_DIR}/generate-certs.sh"
    exit 1
  fi
  
  run_cmd "${CERT_DIR}/generate-certs.sh" "${CERT_DIR}" "$ENVIRONMENT" "$CERT_VALIDITY"
  
  if [ -d "${CERT_DIR}/ca" ]; then
    success "Certificates generated successfully"
    log "  Root CA: ${CERT_DIR}/ca/ca.crt"
    log "  Server:  ${CERT_DIR}/server/server.crt"
    log "  Client:  ${CERT_DIR}/client/client.crt"
    log "  mTLS:    ${CERT_DIR}/mtls/"
  else
    error "Certificate generation failed"
    exit 1
  fi
}

# Setup Vault
setup_vault() {
  log "Setting up Vault secret management..."
  
  if [ "$SKIP_VAULT" = true ]; then
    warning "Skipping Vault setup (--skip-vault)"
    return
  fi
  
  if [ ! -f "${VAULT_DIR}/setup-vault.sh" ]; then
    error "Vault setup script not found: ${VAULT_DIR}/setup-vault.sh"
    exit 1
  fi
  
  if [ -z "${VAULT_TOKEN:-}" ]; then
    error "VAULT_TOKEN environment variable not set"
    error "Set it before running: export VAULT_TOKEN=your-token"
    return 1
  fi
  
  export VAULT_ADDR
  run_cmd "${VAULT_DIR}/setup-vault.sh"
  
  if [ $? -eq 0 ]; then
    success "Vault configured successfully"
    log "  Vault Address: $VAULT_ADDR"
    log "  Policies: Check ${VAULT_DIR}/default-policy.hcl"
  else
    error "Vault setup failed"
    exit 1
  fi
}

# Run security tests
run_security_tests() {
  log "Running security testing suite..."
  
  if [ "$SKIP_TESTS" = true ]; then
    warning "Skipping security tests (--skip-tests)"
    return
  fi
  
  if [ ! -f "${TEST_DIR}/run-security-tests.sh" ]; then
    error "Security test script not found: ${TEST_DIR}/run-security-tests.sh"
    exit 1
  fi
  
  run_cmd "${TEST_DIR}/run-security-tests.sh"
  
  if [ -d "${TEST_DIR}/reports" ]; then
    success "Security tests completed"
    log "  Reports location: ${TEST_DIR}/reports/"
  fi
}

# Generate SBOM
generate_sbom() {
  log "Generating Software Bill of Materials..."
  
  if [ "$SKIP_SBOM" = true ]; then
    warning "Skipping SBOM generation (--skip-sbom)"
    return
  fi
  
  if [ ! -f "${SBOM_DIR}/generate-sbom.sh" ]; then
    error "SBOM generation script not found: ${SBOM_DIR}/generate-sbom.sh"
    exit 1
  fi
  
  run_cmd "${SBOM_DIR}/generate-sbom.sh"
  
  if [ -f "${SBOM_DIR}/SBOM-REPORT-"* ]; then
    success "SBOM generated successfully"
    log "  SBOM location: ${SBOM_DIR}/"
  fi
}

# Display OPA policy information
display_opa_info() {
  local opa_dir="${SCRIPT_DIR}/opa-policies"
  
  if [ ! -d "$opa_dir" ]; then
    return
  fi
  
  log "OPA Policies deployed:"
  for policy in "$opa_dir"/*.rego; do
    if [ -f "$policy" ]; then
      echo "  - $(basename "$policy")"
    fi
  done
  
  log "To load OPA policies, run:"
  log "  curl -X PUT http://localhost:8181/v1/policies/data -d @${opa_dir}/data.rego"
  log "  curl -X PUT http://localhost:8181/v1/policies/access -d @${opa_dir}/access.rego"
}

# Display post-setup information
display_post_setup() {
  echo ""
  echo -e "${GREEN}═══════════════════════════════════════════════════════════${NC}"
  echo -e "${GREEN}Security Setup Complete${NC}"
  echo -e "${GREEN}═══════════════════════════════════════════════════════════${NC}"
  echo ""
  
  if [ "$SKIP_CERTS" = false ]; then
    echo -e "${BLUE}Certificates:${NC}"
    echo "  ✓ Generated in: $CERT_DIR"
    echo "  ✓ Validity: $CERT_VALIDITY days"
    echo "  ✓ For Docker: Mount ${CERT_DIR}/server/cert.pem and key.pem"
    echo "  ✓ For K8s: Create secret with generated certificates"
    echo ""
  fi
  
  if [ "$SKIP_VAULT" = false ]; then
    echo -e "${BLUE}Vault:${NC}"
    echo "  ✓ Configured at: $VAULT_ADDR"
    echo "  ✓ Policies loaded"
    echo "  ✓ Auth methods enabled: Kubernetes, JWT"
    echo "  ✓ Secret engines: KV, PKI, Database"
    echo ""
  fi
  
  if [ "$SKIP_SBOM" = false ]; then
    echo -e "${BLUE}SBOM:${NC}"
    echo "  ✓ Generated in: $SBOM_DIR"
    echo "  ✓ Formats: CycloneDX, SPDX"
    echo "  ✓ Includes: Java, Python, Node.js dependencies"
    echo ""
  fi
  
  echo -e "${BLUE}Next Steps:${NC}"
  echo "  1. Review generated certificates in: $CERT_DIR"
  echo "  2. Review threat model: ${SCRIPT_DIR}/threat-models/threat-model.md"
  echo "  3. Deploy OPA policies to your OPA server"
  echo "  4. Configure services to use Vault for secrets"
  echo "  5. Enable ModSecurity WAF in your reverse proxy"
  echo "  6. Set up monitoring alerts for security events"
  echo ""
  echo "Documentation:"
  echo "  - Security Implementation: ${SCRIPT_DIR}/IMPLEMENTATION.md"
  echo "  - Responsible Disclosure: $(dirname "$SCRIPT_DIR")/security.md"
  echo "  - Setup log: $LOG_FILE"
  echo ""
}

# Main execution
main() {
  echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
  echo -e "${BLUE}EmploymentVC Security Setup Orchestration${NC}"
  echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
  echo ""
  
  log "Starting security setup for: $ENVIRONMENT"
  [ "$DRY_RUN" = true ] && log "(DRY-RUN MODE - no actual changes)"
  
  validate_environment
  validate_prerequisites
  
  setup_certificates
  setup_vault
  run_security_tests
  generate_sbom
  
  display_opa_info
  display_post_setup
  
  success "Security setup completed successfully"
  log "Full log saved to: $LOG_FILE"
}

# Execute
main "$@"
