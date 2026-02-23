#!/bin/bash

# Security Implementation Guide for EmploymentVC
# This script helps setup recommended security tools

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_step() {
    echo -e "${BLUE}==>${NC} $1"
}

print_success() {
    echo -e "${GREEN}✅${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠️${NC} $1"
}

print_section() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "${BLUE}$1${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
}

# Color output for better visibility
print_section "EmploymentVC Security Implementation Guide"

print_step "Checking current security status..."
echo ""

# Check 1: API Gateway security filters
if grep -q "RateLimitingFilter" backend-services/api-gateway/build.gradle 2>/dev/null; then
    print_success "API Gateway security filters implemented"
else
    print_warning "API Gateway security filters not found"
fi

# Check 2: Rate limiting dependency
if grep -q "bucket4j" backend-services/api-gateway/build.gradle 2>/dev/null; then
    print_success "Rate limiting library (bucket4j) configured"
else
    print_warning "Rate limiting library not configured"
fi

# Check 3: Security configuration
if [ -f "backend-services/api-gateway/src/main/resources/application-security.properties" ]; then
    print_success "Security configuration file exists"
else
    print_warning "Security configuration file not found"
fi

echo ""
print_section "Phase 1: Immediate Setup (Already Done) ✅"

echo "✅ API Gateway Rate Limiting - RateLimitingFilter.java"
echo "✅ Security Headers - SecurityHeadersFilter.java"
echo "✅ Request Size Limits - SizeLimitFilter.java"
echo "✅ Input Validation - InputValidator.java"
echo "✅ Security Configuration - SecurityConfig.java"
echo ""

print_section "Phase 2: Next Steps (2-4 Weeks)"

echo "1. Add Failed Login Throttling"
echo "   Location: backend-services/auth-service"
echo "   File: LoginAttemptService.java (to create)"
echo "   Purpose: Prevent brute force attacks"
echo ""

echo "2. Implement JWT Token Rotation"
echo "   Location: backend-services/auth-service"
echo "   Purpose: Reduce token hijacking risk"
echo ""

echo "3. Setup Security Monitoring"
echo "   Location: infra/prometheus/prometheus.yml"
echo "   Purpose: Track security metrics"
echo ""

print_section "Phase 3: Recommended Tools Setup (1-2 Months)"

echo "1. ModSecurity + OWASP Rules"
echo "   Command: docker-compose -f infra/security/modsecurity-compose.yml up -d"
echo "   Status: Recommended (self-hosted WAF)"
echo ""

echo "2. CrowdSec (Intrusion Detection)"
echo "   Command: docker-compose -f infra/security/crowdsec-compose.yml up -d"
echo "   Status: Recommended (modern IDS)"
echo ""

echo "3. HashiCorp Vault (Secrets Management)"
echo "   Command: docker-compose -f infra/security/vault-compose.yml up -d"
echo "   Status: Recommended (centralized secrets)"
echo ""

print_section "Implementation Checklist"

echo "Immediate Actions (This Month):"
echo "  [ ] Review SECURITY_ARCHITECTURE.md"
echo "  [ ] Enable security logging in API Gateway"
echo "  [ ] Test rate limiting implementation"
echo "  [ ] Configure CORS properly"
echo "  [ ] Enable HTTPS (set spring.security.require-ssl=true)"
echo ""

echo "Short-term Actions (1-2 Months):"
echo "  [ ] Setup ModSecurity (WAF)"
echo "  [ ] Implement CrowdSec"
echo "  [ ] Add failed login throttling"
echo "  [ ] Implement JWT rotation"
echo "  [ ] Add security monitoring dashboard"
echo ""

echo "Medium-term Actions (2-3 Months):"
echo "  [ ] Deploy HashiCorp Vault"
echo "  [ ] Implement mTLS between services"
echo "  [ ] Setup Service Mesh (Istio) for security policies"
echo "  [ ] Security hardening review"
echo ""

print_section "Testing Security Implementation"

echo "Rate Limiting Test:"
echo "  $ for i in {1..150}; do curl http://localhost:8080/api/v1/health; done"
echo ""

echo "Security Headers Test:"
echo "  $ curl -I http://localhost:8080/api/v1/health | grep -i 'security\\|x-'"
echo ""

echo "Input Validation Test:"
echo "  $ curl \"http://localhost:8080/api/credentials?issuer=1' OR '1'='1\""
echo ""

print_section "Quick Start Commands"

echo "1. Build API Gateway with security features:"
echo "   $ ./gradlew :backend-services:api-gateway:build"
echo ""

echo "2. Run with security configuration:"
echo "   $ ./gradlew :backend-services:api-gateway:bootRun --args='--spring.profiles.active=security'"
echo ""

echo "3. Deploy to Docker:"
echo "   $ docker build -f backend-services/api-gateway/Dockerfile -t employmentvc/api-gateway:latest ."
echo ""

print_section "Monitoring Security"

echo "Access Prometheus metrics:"
echo "  http://localhost:9090/api/v1/query?query=http_server_requests_seconds_count%7Bstatus%3D%22429%22%7D"
echo ""

echo "Grafana dashboard:"
echo "  http://localhost:3001/d/api-gateway"
echo ""

print_section "Need More Information?"

echo ""
echo "📖 Document Links:"
echo "   • Full SecurityArchitecture: ./SECURITY_ARCHITECTURE.md"
echo "   • API Gateway Security: ./backend-services/api-gateway/SECURITY.md"
echo "   • Deployment Guide: ./docs/DEPLOYMENT.md"
echo "   • OWASP Top 10: https://owasp.org/Top10/"
echo ""

print_success "Security implementation guide complete!"

echo ""
echo "Next Step: Review ./SECURITY_ARCHITECTURE.md and implement Phase 2"
echo ""
