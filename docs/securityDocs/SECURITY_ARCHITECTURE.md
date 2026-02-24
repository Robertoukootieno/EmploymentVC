# Security Architecture for EmploymentVC

## 🔒 Overview

This document outlines the comprehensive security architecture for the EmploymentVC platform, addressing DDoS/DoS prevention, injection attacks, authentication, authorization, and data protection.

---

## 1. Attack Prevention Strategy

### 1.1 DDoS/DoS Protection (Layer 1: Infrastructure)

**Recommended Stack**:

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Edge Protection** | Cloudflare / AWS Shield | DDoS mitigation at CDN level |
| **WAF** | ModSecurity + OWASP CRS | Application-level attack filtering |
| **Gateway** | nginx with rate limiting | Request throttling per IP |
| **Intrusion Detection** | CrowdSec / Falco | Real-time threat detection |

**Implementation**:

```yaml
# In docker-compose.prod.yml
  nginx-proxy:
    image: nginx:latest
    volumes:
      - ./infra/docker/nginx.conf:/etc/nginx/nginx.conf:ro
    ports:
      - "80:80"
      - "443:443"
    networks:
      - provenly-network
```

### 1.2 Rate Limiting (Layer 2: API Gateway)

**Already Implemented**:
- ✅ Per-IP rate limiting (100 req/min) - RateLimitingFilter
- ✅ Request size limits (10MB) - SizeLimitFilter
- ✅ Timeout enforcement - application.properties

**Next Steps**:
- [ ] Per-user rate limiting (via JWT claims)
- [ ] Per-endpoint rate limiting (API key based)
- [ ] Circuit breaker patterns (Resilience4j)

### 1.3 Injection Attack Prevention (Layer 2: Application)

**Implemented**:
- ✅ Input validation - InputValidator
- ✅ SQL injection detection - Regex patterns
- ✅ XSS prevention - Output encoding
- ✅ Path traversal protection
- ✅ Command injection detection
- ✅ XXE prevention

**Framework Support**:
- Spring Boot validation annotations (@Valid, @NotBlank, @Email, etc.)
- Spring Security (CSRF tokens, CORS)
- Prepared statements (automatic with Spring JPA)

---

## 2. Security Implementation by Layer

### Layer 1: Edge/CDN (External)

```bash
# Cloudflare (Recommended for quick setup)
# 1. Point DNS to Cloudflare
# 2. Enable DDoS protection (automatic)
# 3. Configure WAF rules
# 4. Enable rate limiting at CDN level
```

### Layer 2: Reverse Proxy (nginx)

**File**: `infra/nginx/nginx.conf` (to be created)

```nginx
# Rate limiting
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;
limit_req_zone $binary_remote_addr zone=auth_limit:10m rate=5r/s;

# Request size limits
client_max_body_size 10M;
client_body_buffer_size 128k;

# Timeouts
proxy_connect_timeout 60s;
proxy_send_timeout 60s;
proxy_read_timeout 60s;

# Security headers
add_header X-Frame-Options "DENY" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Content-Security-Policy "default-src 'self'" always;
```

### Layer 3: API Gateway (Spring Boot)

**Already Implemented**:
- ✅ RateLimitingFilter
- ✅ SizeLimitFilter  
- ✅ SecurityHeadersFilter
- ✅ InputValidator

**Location**: `backend-services/api-gateway/src/main/java/io/provenly/apigateway/security/`

### Layer 4: Application Services

Each microservice should implement:

```java
// Service layer input validation
@Service
public class IssuerService {
    
    @Autowired
    private InputValidator validator;
    
    public CredentialResponse issueCredential(CredentialRequest request) {
        // Validate all inputs
        if (!validator.validateInput(request.getSubjectDid(), "subjectDid")) {
            throw new InvalidInputException("Invalid subject DID");
        }
        
        // Process with confidence
        return issueCredential(request);
    }
}
```

### Layer 5: Database

**SQL Injection Prevention**:

```java
// ✅ GOOD - Using Spring JPA (automatic parameterization)
List<Credential> creds = credentialRepository.findByIssuerDid(issuerDid);

// ❌ AVOID - String concatenation
String query = "SELECT * FROM credentials WHERE issuer = '" + issuerDid + "'";
```

---

## 3. Threat Model & Mitigations

| Threat | Layer | Mitigation | Status |
|--------|-------|-----------|--------|
| **DDoS - Volumetric** | CDN | Cloudflare / AWS Shield | 📋 Recommended |
| **DDoS - Protocol** | WAF | ModSecurity / WAF rules | 📋 Recommended |
| **DDoS - Application** | Gateway | Rate limiting per IP | ✅ Implemented |
| **Brute Force - Auth** | Auth Service | Failed login throttling | 🔨 Follow-up |
| **SQL Injection** | App Layer | Prepared statements | ✅ Implemented |
| **XSS** | API Gateway | Output encoding + CSP headers | ✅ Implemented |
| **CSRF** | Web Layer | CSRF tokens + SameSite cookies | ✅ Via Keycloak |
| **XXE** | Parsers | XML entity resolution disabled | ✅ Implemented |
| **Command Injection** | App Layer | Input validation | ✅ Implemented |
| **Path Traversal** | Middleware | Regex filtering | ✅ Implemented |
| **Credential Theft** | Transport | TLS 1.3 + mTLS | 📋 Recommended |
| **Token Hijacking** | Auth | JWT rotation + secure cookies | 🔨 Follow-up |

---

## 4. Recommended Additional Tools

### 4.1 Web Application Firewall (WAF)

**Option A: ModSecurity (Self-Hosted)**

```bash
# Installation
docker run -d \
  -v /path/to/modsec-rules:/etc/modsecurity/rules \
  --name modsecurity \
  modsecurity/modsecurity-docker
```

**Benefits**:
- ✅ Open source
- ✅ No licensing costs
- ✅ OWASP Core Rule Set included
- ✅ Full control

**Setup**: See `infra/modsecurity/setup.sh` (to be created)

### 4.2 Intrusion Detection System (IDS)

**Option A: CrowdSec (Recommended)**

```bash
# Installation
docker run -d \
  -e CROWDSEC_REGISTRATION_KEY=<key> \
  -v /path/to/logs:/var/log:ro \
  --name crowdsec \
  crowdsecurity/crowdsec:latest
```

**Why CrowdSec?**:
- ✅ Modern, lightweight
- ✅ Community-driven threat intelligence
- ✅ Real-time detection
- ✅ Automated response (ban bad IPs)
- ✅ Works with Docker/K8s

### 4.3 Secrets Management

**Option A: HashiCorp Vault (Recommended)**

```yaml
# In docker-compose.prod.yml
  vault:
    image: vault:latest
    environment:
      VAULT_DEV_ROOT_TOKEN_ID: myroot
      VAULT_DEV_LISTEN_ADDRESS: 0.0.0.0:8200
    ports:
      - "8200:8200"
    volumes:
      - vault_data:/vault/data
```

**Benefits**:
- ✅ Centralized secret management
- ✅ Automatic secret rotation
- ✅ Audit logging
- ✅ Kubernetes integration
- ✅ Database credential generation

### 4.4 Dependency Vulnerability Scanning

**Already in CI/CD**:
- ✅ Trivy (file system scanning)
- ✅ OWASP Dependency-Check (Java dependencies)

**Add Snyk** (for more comprehensive scanning):

```yaml
# .github/workflows/ci-cd.yml
- name: Snyk Security Scanning
  uses: snyk/actions/gradle@master
  env:
    SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
```

---

## 5. Implementation Roadmap

### Phase 1: Immediate (This Week) ✅
- [x] Rate limiting in API Gateway
- [x] Security headers
- [x] Input validation
- [x] Request size limits
- [ ] Enable security logging

### Phase 2: Short-term (2-4 Weeks)
- [ ] Setup ModSecurity (local/dev)
- [ ] Add failed login throttling
- [ ] Implement JWT rotation
- [ ] Add CORS configuration

### Phase 3: Mid-term (1-2 Months)
- [ ] Deploy CrowdSec
- [ ] Setup HashiCorp Vault
- [ ] Implement mTLS between services
- [ ] Add Istio Service Mesh (optional but recommended)

### Phase 4: Long-term (2-3 Months)
- [ ] Cloudflare/CDN integration
- [ ] Advanced threat detection
- [ ] Penetration testing
- [ ] Security hardening review

---

## 6. Configuration Management

### Environment-Specific Settings

**Development** (`application-dev.properties`):
```properties
security.rate-limit.requests-per-minute=1000  # More lenient
security.size-limit.enabled=true
cors.allowed-origins=http://localhost:3000,http://localhost:3001
```

**Production** (`application-prod.properties`):
```properties
security.rate-limit.requests-per-minute=100   # Strict
security.size-limit.enabled=true
cors.allowed-origins=https://yourdomain.com
spring.security.require-ssl=true
```

---

## 7. Monitoring & Alerting

### Key Metrics to Monitor

```prometheus
# Rate limit hits
http_server_requests_seconds_count{status="429"}

# Security events
security_validation_failures_total
security_injection_attempts_total
security_size_limit_violations_total

# Authentication failures
auth_failures_total{reason="invalid_credentials"}
auth_failures_total{reason="token_expired"}

# API latency
http_server_requests_seconds{endpoint="/api/credentials"}
```

### Alert Rules

```yaml
# prometheus alerts
- alert: HighRateLimitViolations
  expr: rate(http_server_requests_seconds_count{status="429"}[5m]) > 10
  for: 5m

- alert: SuspiciousInputDetected
  expr: rate(security_injection_attempts_total[5m]) > 5
  for: 5m

- alert: ManyAuthFailures
  expr: rate(auth_failures_total[5m]) > 10
  for: 5m
```

---

## 8. Security Testing

### Manual Testing

```bash
# Test rate limiting
for i in {1..150}; do curl http://localhost:8080/api/v1/health; done

# Test security headers
curl -I http://localhost:8080/api/v1/health | grep -i "security\|x-\|content-security"

# Test SQL injection attempt
curl "http://localhost:8080/api/credentials?issuer=1' OR '1'='1"

# Test XSS attempt
curl "http://localhost:8080/api/credentials?name=<script>alert('xss')</script>"

# Test oversized request
dd if=/dev/zero bs=1M count=15 | curl -X POST --data-binary @- http://localhost:8080/api/test
```

### Automated Testing

```java
@SpringBootTest
public class SecurityFilterTests {
    
    @Test
    public void testRateLimitingFilter() {
        // Send 150 requests in rapid succession
        // Verify requests 101+ return 429
    }
    
    @Test
    public void testInputValidation() {
        // Test SQL injection patterns
        // Test XSS patterns
        // Test path traversal
    }
    
    @Test
    public void testSecurityHeaders() {
        // Verify all security headers present
        // Verify CSP policy enforced
    }
}
```

---

## 9. Compliance & Standards

**Implemented Standards**:
- ✅ OWASP Top 10 protections
- ✅ NIST Cybersecurity Framework basics
- ✅ CIS Controls

**Recommended Frameworks**:
- [ ] ISO 27001 (Information Security Management)
- [ ] SOC 2 (if required for enterprise clients)
- [ ] GDPR compliance (if EU operations)

---

## 10. Resources & References

- [OWASP Top 10 2021](https://owasp.org/Top10/)
- [OWASP API Security Top 10](https://owasp.org/www-project-api-security/)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [Bucket4j Rate Limiting](https://github.com/vladimir-bukhtoyarov/bucket4j)
- [ModSecurity Documentation](https://modsecurity.org/)
- [CrowdSec Documentation](https://www.crowdsec.net/)
- [HashiCorp Vault](https://www.hashicorp.com/products/vault)

---

## 11. Security Incident Response

### Procedure

1. **Detection**: Monitor security metrics and alerts
2. **Containment**: Rate limit / block suspicious IPs
3. **Investigation**: Review logs via ELK Stack
4. **Remediation**: Patch vulnerability / update rules
5. **Documentation**: Log incident for audit trail

### Key Contacts

- **Security Team**: security@provenly.io
- **DevOps**: devops@provenly.io
- **Management**: management@provenly.io

---

## 12. Next Actions

### Immediate (Today)
- [x] Implement API Gateway security filters ✅
- [ ] Review and confirm these recommendations

### This Week
- [ ] Add security headers to docker-compose
- [ ] Create ModSecurity nginx config
- [ ] Setup security monitoring dashboard

### This Month
- [ ] Deploy WAF (ModSecurity or similar)
- [ ] Implement CrowdSec
- [ ] Add penetration testing

---

**Document Version**: 1.0  
**Last Updated**: February 18, 2026  
**Next Review**: March 18, 2026
