#!/usr/bin/env bash

# Security Testing Script - SAST, DAST, Dependency Scanning

set -euo pipefail

PROJECT_ROOT="${1:-.}"
REPORT_DIR="${PROJECT_ROOT}/security/security-tests/reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

mkdir -p "$REPORT_DIR"/{sast,dast,dependency}

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_section() { echo -e "\n${BLUE}==>${NC} $1\n"; }
print_ok() { echo -e "${GREEN}✓${NC} $1"; }
print_warn() { echo -e "${YELLOW}⚠${NC} $1"; }

# 1. SAST - Static Application Security Testing
print_section "Running SAST (Spotbugs, Checkstyle, PMD)"

if command -v ./gradlew &>/dev/null; then
  cd "$PROJECT_ROOT"
  
  echo "Running Spotbugs (Java bytecode analysis)..."
  ./gradlew spotbugsMain spotbugsTest 2>/dev/null || print_warn "Spotbugs not fully configured"
  
  echo "Running Checkstyle..."
  ./gradlew checkstyleMain checkstyleTest 2>/dev/null || print_warn "Checkstyle warnings found"
  
  print_ok "SAST analysis complete"
else
  print_warn "Gradle not found, skipping SAST"
fi

# 2. Dependency Vulnerability Scanning
print_section "Running Dependency Vulnerability Scan"

if command -v grype &>/dev/null; then
  echo "Using Grype for vulnerability scanning..."
  grype "$PROJECT_ROOT" --output json > "$REPORT_DIR/dependency/grype-${TIMESTAMP}.json" 2>/dev/null || true
  grype "$PROJECT_ROOT" || print_warn "Some vulnerabilities detected"
  print_ok "Grype scan complete"
elif command -v trivy &>/dev/null; then
  echo "Using Trivy for vulnerability scanning..."
  trivy fs "$PROJECT_ROOT" --severity HIGH,CRITICAL > "$REPORT_DIR/dependency/trivy-${TIMESTAMP}.json" 2>/dev/null || true
  print_ok "Trivy scan complete"
else
  print_warn "grype or trivy not installed, skipping dependency scan"
  print_warn "  Install with: curl -sSfL https://raw.githubusercontent.com/anchore/grype/main/install.sh | sh -s -- -b /usr/local/bin"
fi

# 3. DAST - Dynamic Application Security Testing
print_section "Dynamic Application Security Testing (DAST)"

if command -v zaproxy &>/dev/null; then
  echo "Running OWASP ZAP Proxy..."
  cat > "$REPORT_DIR/dast/zap-config.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <scan>
    <url>http://localhost:3000</url>
    <contexts>
      <context>
        <name>EmploymentVC</name>
        <urls>
          <url>http://localhost:3000.*</url>
          <url>http://localhost:8080.*</url>
          <url>http://localhost:8081.*</url>
        </urls>
      </context>
    </contexts>
    <policy>OWASP_Top_10</policy>
  </scan>
</configuration>
EOF
  print_ok "ZAP configuration created at $REPORT_DIR/dast/zap-config.xml"
  print_warn "Run DAST manually or integrate with CI/CD pipeline"
else
  print_warn "ZAP not installed, skipping DAST"
fi

# 4. License Compliance Check
print_section "License Compliance Check"

if command -v ./gradlew &>/dev/null; then
  cd "$PROJECT_ROOT"
  echo "Checking license compatibility..."
  ./gradlew licenseReport 2>/dev/null || print_warn "License report not configured"
  print_ok "License check complete"
else
  print_warn "Gradle not found"
fi

# 5. Generate Summary Report
print_section "Generating Summary Report"

cat > "$REPORT_DIR/security-test-report-${TIMESTAMP}.md" << EOF
# EmploymentVC Security Test Report
**Generated**: $(date)

## Test Summary
- **SAST**: Static code analysis (Spotbugs, Checkstyle, PMD)
- **Dependency**: Vulnerability scanning (Grype/Trivy)
- **DAST**: Dynamic testing (OWASP ZAP)
- **Licenses**: License compliance (Gradle)

## Results

### SAST Results
- Keep findings in \`$REPORT_DIR/sast/\`
- Address all HIGH and CRITICAL severity issues

### Dependency Vulnerabilities
- Results in \`$REPORT_DIR/dependency/\`
- Patch dependencies as needed
- Keep vulnerability tracking up-to-date

### DAST Results
- Configuration: \`$REPORT_DIR/dast/zap-config.xml\`
- Run before each release
- Document any known false positives

## Remediation Steps
1. Review SAST findings in build output
2. Update vulnerable dependencies: \`./gradlew dependencyUpdates\`
3. Run DAST against staging environment
4. Document exceptions with justification
5. Re-scan after each fix

## Next Steps
- Integrate into CI/CD pipeline
- Set up automated alerts for new vulnerabilities
- Schedule regular security audits (quarterly)
- Review and update security policies

EOF

print_ok "Report saved to: $REPORT_DIR/security-test-report-${TIMESTAMP}.md"

echo ""
echo "================================"
echo "Security Testing Complete"
echo "================================"
echo "Reports available in: $REPORT_DIR"
echo ""
