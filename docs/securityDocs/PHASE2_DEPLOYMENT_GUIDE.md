# 🔒 Phase 2: Security Tools Deployment Guide

## Overview

Phase 2 implements three critical security enhancements:
1. **ModSecurity WAF** - Web Application Firewall with OWASP rules
2. **CrowdSec IDS/IPS** - Intrusion Detection & Prevention System
3. **Failed Login Throttling** - Brute force attack prevention

**Duration**: 2-4 weeks
**Priority**: HIGH
**Status**: Ready for implementation

---

## 1. ModSecurity + nginx WAF Setup

### What It Does
- Inspects all HTTP requests/responses at the gateway
- Detects and blocks SQL injection, XSS, command injection, path traversal
- Protects against malicious user agents and bots
- Rate limiting per endpoint (configurable)
- Request/response logging and forensics

### Architecture
```
Client
  ↓
(HTTPS)
  ↓
nginx + ModSecurity (Port 443)
  ↓
Spring Boot API Gateway (Port 8080)
  ↓
Backend Services
```

### Files Created
- `infra/security/modsecurity/modsecurity.conf` - Core ModSecurity rules
- `infra/security/modsecurity/custom-rules.conf` - EmploymentVC-specific rules
- `infra/security/modsecurity/nginx.conf` - nginx configuration with ModSecurity
- `infra/security/docker-compose.security.yml` - Container orchestration

### Deployment Steps

#### Step 1: Generate SSL Certificates

```bash
# Create certificate directory
mkdir -p infra/security/certs

# Generate self-signed certificate for testing (DEVELOPMENT ONLY)
openssl req -x509 -newkey rsa:4096 -keyout infra/security/certs/key.pem \
  -out infra/security/certs/cert.pem -days 365 -nodes \
  -subj "/CN=localhost/O=EmploymentVC/C=US"

# For production, use valid certificates:
# - From Let's Encrypt: certbot certonly --manual -d yourdomain.com
# - From your SSL provider
# Place them in: infra/security/certs/

# Create chain reference (for OCSP stapling)
cp infra/security/certs/cert.pem infra/security/certs/chain.pem
```

#### Step 2: Create Log Directories

```bash
# Create directories for log volumes
mkdir -p /var/log/nginx
mkdir -p /var/log/modsecurity
mkdir -p /var/log/employmentvc/api-gateway
mkdir -p /var/log/employmentvc/auth-service
mkdir -p /var/log/employmentvc/did-registry

# Set proper permissions
chmod 755 /var/log/nginx
chmod 755 /var/log/modsecurity
chmod 755 /var/log/employmentvc/*
```

#### Step 3: Update Docker Compose

```bash
# Backup existing docker-compose.yml
cp docker-compose.yml docker-compose.yml.backup

# Add security network to existing docker-compose.yml:
cat >> docker-compose.yml << 'EOF'

networks:
  employmentvc-security:
    driver: bridge
    external: false
EOF

# Or use the provided security compose file alongside:
COMPOSE_PROJECT_NAME=employmentvc-security docker compose -f docker-compose.yml -f infra/security/docker-compose.security.yml up -d
```

#### Step 4: Start ModSecurity Stack

```bash
# Navigate to security directory
cd infra/security

# Start only nginx + ModSecurity
COMPOSE_PROJECT_NAME=employmentvc-security docker compose -f docker-compose.security.yml up -d nginx-modsecurity

# Verify it's running
docker ps | grep modsecurity

# Check logs
COMPOSE_PROJECT_NAME=employmentvc-security docker compose -f docker-compose.security.yml logs -f nginx-modsecurity
```

#### Step 5: Configure API Gateway

Update `backend-services/api-gateway/src/main/resources/application.yml`:

```yaml
# Before ModSecurity:
server:
  port: 8080
  servlet:
    context-path: /

# After ModSecurity (API Gateway is now internal):
server:
  port: 8080
  servlet:
    context-path: /
  # Only listen on internal network
  address: 0.0.0.0
```

#### Step 6: Configure nginx Upstream

The nginx configuration already includes:
```nginx
upstream api_gateway {
    server api-gateway:8080 max_fails=3 fail_timeout=30s;
    keepalive 32;
}
```

If your API Gateway has a different hostname/port, edit `infra/security/modsecurity/nginx.conf`.

### Testing ModSecurity

```bash
# Test 1: Normal request (should pass)
curl -v https://localhost/api/v1/health -k

# Test 2: SQL injection attempt (should be blocked)
curl -v "https://localhost/api/credentials?id=1' OR '1'='1" -k

# Test 3: XSS attempt (should be blocked)
curl -v "https://localhost/api/test?name=<script>alert('xss')</script>" -k

# Test 4: Command injection (should be blocked)
curl -v "https://localhost/api/test?cmd=$(whoami)" -k

# Test 5: Path traversal (should be blocked)
curl -v "https://localhost/api/test?file=../../../../etc/passwd" -k

# Test 6: Rate limiting (send 60+ requests in short time)
for i in {1..70}; do curl -s https://localhost/api/health -k -w "%{http_code}\n" | head -1; done | sort | uniq -c
```

### Monitoring ModSecurity

```bash
# View access logs
COMPOSE_PROJECT_NAME=employmentvc-security docker compose -f docker-compose.security.yml exec -T nginx-modsecurity tail -f /var/log/nginx/access.log

# View error logs
COMPOSE_PROJECT_NAME=employmentvc-security docker compose -f docker-compose.security.yml exec -T nginx-modsecurity tail -f /var/log/nginx/error.log

# View ModSecurity audit log
COMPOSE_PROJECT_NAME=employmentvc-security docker compose -f docker-compose.security.yml exec -T nginx-modsecurity tail -f /var/log/modsecurity/audit.log | jq .

# Check nginx status
curl -s http://localhost:8080/nginx_status
```

### Configuration Tuning

**Disable specific rules (if needed):**
```conf
# In custom-rules.conf, to exclude a rule:
SecRuleRemoveById 30001
```

**Change rate limits:**
```nginx
# In nginx.conf, modify:
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=100r/m;
              # ↑ change this number
```

**Adjust request size limits:**
```conf
# In modsecurity.conf:
SecRequestBodyLimit 10485760  # 10MB
SecResponseBodyLimit 10485760
# AND in nginx.conf:
client_max_body_size 10M;
```

---

## 2. CrowdSec IDS/IPS Setup

### What It Does
- Real-time intrusion detection system
- Analyzes logs from nginx, ModSecurity, application logs
- Detects attack patterns and malicious IP addresses
- Automatically blocks intruders
- Community-powered threat intelligence
- Beautiful dashboard for visualization

### Architecture
```
Logs (nginx, ModSecurity, Apps)
  ↓
CrowdSec Agent (analyzes logs)
  ↓
CrowdSec LAPI (local API)
  ↓
CrowdSec Bouncer (enforces blocks)
  ↓
Block suspicious IPs
  +
CrowdSec UI (Dashboard at localhost:3001)
```

### Files Created
- `infra/security/crowdsec/crowdsec-config.yaml` - Core CrowdSec configuration
- `infra/security/crowdsec/acquirers.yaml` - Log sources to monitor
- Part of `infra/security/docker-compose.security.yml` - Services definition

### Deployment Steps

#### Step 1: Prepare Environment Variables

```bash
# Create .env file in infra/security/
cp infra/security/.env.example infra/security/.env

# Then edit if needed:
cat > infra/security/.env << 'EOF'
# CrowdSec API Keys (generate during first run)
CROWDSEC_AGENT_PASSWORD=yourSecurePasswordHere123!
CROWDSEC_AGENT_USERNAME=admin
CROWDSEC_BOUNCER_API_KEY=generated_during_setup
CROWDSEC_API_KEY=generated_during_setup

# Optional: for sending to CrowdSec cloud
CROWDSEC_ONLINE_API_KEY=optional_cloud_integration_key
EOF

chmod 600 infra/security/.env
```

#### Step 2: Start CrowdSec Services

```bash
cd infra/security

# Start CrowdSec, Bouncer, and UI
COMPOSE_PROJECT_NAME=employmentvc-security docker compose -f docker-compose.security.yml up -d crowdsec crowdsec-bouncer crowdsec-ui

# Wait for initialization
sleep 30

# Verify services are running
docker ps | grep crowdsec
```

#### Step 3: Generate API Keys

```bash
# Get API key for bouncer
BOUNCER_KEY=$(COMPOSE_PROJECT_NAME=employmentvc-security docker compose -f docker-compose.security.yml exec -T crowdsec cscli bouncers add nginx-bouncer -o json | jq -r '.api_key')

# Get API key for UI
UI_KEY=$(COMPOSE_PROJECT_NAME=employmentvc-security docker compose -f docker-compose.security.yml exec -T crowdsec cscli machines list -o json | jq -r '.[0].machineID')

# Update .env with these keys
sed -i "s/CROWDSEC_BOUNCER_API_KEY=.*/CROWDSEC_BOUNCER_API_KEY=${BOUNCER_KEY}/" infra/security/.env
sed -i "s/CROWDSEC_API_KEY=.*/CROWDSEC_API_KEY=${UI_KEY}/" infra/security/.env

# Restart services with updated keys
COMPOSE_PROJECT_NAME=employmentvc-security docker compose -f docker-compose.security.yml restart crowdsec-ui crowdsec-bouncer
```

#### Step 4: Enable Log Analysis

```bash
# Update CrowdSec to monitor logs
COMPOSE_PROJECT_NAME=employmentvc-security docker compose -f docker-compose.security.yml exec -T crowdsec cscli collections install crowdsecurity/nginx-http-logs
COMPOSE_PROJECT_NAME=employmentvc-security docker compose -f docker-compose.security.yml exec -T crowdsec cscli collections install crowdsecurity/modsecurity-logs
COMPOSE_PROJECT_NAME=employmentvc-security docker compose -f docker-compose.security.yml exec -T crowdsec cscli collections install crowdsecurity/sshguard

# Reload CrowdSec
COMPOSE_PROJECT_NAME=employmentvc-security docker compose -f docker-compose.security.yml exec -T crowdsec cscli reload
```

#### Step 5: Register for Community Blocklist

```bash
# Optional: Enable CrowdSec cloud for IP reputation
docker exec employmentvc-crowdsec cscli console enroll

# This sends anonymized logs to CrowdSec for community intelligence
```

### Testing CrowdSec

```bash
# Test 1: Generate SQL injection alert
for i in {1..10}; do
  curl -s "https://localhost/api/test?id=' OR '1'='1" -k -o /dev/null -w "%{http_code}\n"
done

# Test 2: Check dashboard
# Open browser: http://localhost:3001
# Default credentials: created during setup

# Test 3: View detected incidents
docker exec employmentvc-crowdsec cscli decisions list

# Test 4: Check metrics
docker exec employmentvc-crowdsec cscli metrics

# Test 5: Simulate attack
for i in {1..100}; do
  curl -s https://localhost/api/test -k -o /dev/null
done | head -50

# Test 6: Verify IP is blocked
curl -s https://localhost/api/test -k -w "Status: %{http_code}\n"
# Should eventually return 403 when rate limit is hit
```

### Monitoring CrowdSec

```bash
# View real-time logs
docker exec -it employmentvc-crowdsec tail -f /var/log/crowdsec/crowdsec.log | jq .

# List current decisions (banned IPs)
docker exec employmentvc-crowdsec cscli decisions list

# View alerts
docker exec employmentvc-crowdsec cscli alerts list

# Get scenario stats
docker exec employmentvc-crowdsec cscli lapi status

# Check bouncer status
docker exec employmentvc-crowdsec cscli bouncers list
```

### Configuration Tuning

**Enable specific scenarios:**
```yaml
# In crowdsec-config.yaml, add to scenarios_enabled:
scenarios_enabled:
  - crowdsecurity/http_fast_requests  # Detect fast attackers
  - crowdsecurity/http_rare_methods   # Detect unusual HTTP methods
```

**Adjust sensitivity:**
```yaml
# Add to crowdsec-config.yaml:
thresholds:
  suspicious_score: 5
  malicious_score: 10
```

**Custom rules:**
```bash
# Create custom scenario
docker exec employmentvc-crowdsec cat > /etc/crowdsec/scenarios/custom-rules.yaml << 'EOF'
name: custom/employment-vc-attack
description: Custom rules for EmploymentVC
detection:
  keywords:
    - attack_pattern
EOF
```

---

## 3. Failed Login Throttling Setup

### What It Does
- Tracks failed login attempts per user + IP
- Locks account after 5 failed attempts
- Lockout duration: 15 minutes
- Provides admin endpoints to unlock accounts
- Tracks metrics for monitoring

### Architecture
```
Login Request
  ↓
Check if account locked?
  ├─ Yes → Return 429 (Locked)
  └─ No → Attempt authentication
         ├─ Success → Clear attempts
         └─ Failure → Increment counter
                       ├─ Count < 5 → Return 401
                       └─ Count >= 5 → Lock account + Return 429
```

### Files Created
- `backend-services/auth-service/src/main/java/io/provenly/auth/security/FailedLoginThrottleManager.java`
- `backend-services/auth-service/src/main/java/io/provenly/auth/security/ThrottledAuthenticationProvider.java`
- `backend-services/auth-service/src/main/java/io/provenly/auth/security/ClientIpResolver.java`
- `backend-services/auth-service/src/main/java/io/provenly/auth/exception/AccountLockedException.java`
- `backend-services/auth-service/src/main/java/io/provenly/auth/controller/LoginThrottlingAdminController.java`
- `backend-services/auth-service/src/main/resources/application-throttle.properties`

### Deployment Steps

#### Step 1: Update Auth Service Dependencies

Edit `backend-services/auth-service/build.gradle`:

```gradle
dependencies {
    // Existing dependencies...
    
    // Throttling (already included if using Spring Security)
    implementation 'org.springframework.boot:spring-boot-starter-security'
    
    // Lombok (for reducing boilerplate)
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Testing
    testImplementation 'org.springframework.security:spring-security-test'
}
```

#### Step 2: Update Auth Service Security Configuration

Update `backend-services/auth-service/src/main/java/io/provenly/auth/config/SecurityConfig.java`:

```java
package io.provenly.auth.config;

import io.provenly.auth.security.ThrottledAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ThrottledAuthenticationProvider throttledAuthProvider;

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(throttledAuthProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/api/auth/login", "/api/auth/register").permitAll()
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            .and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        return http.build();
    }
}
```

#### Step 3: Enable Throttling in Application Config

Update `backend-services/auth-service/src/main/resources/application.yml`:

```yaml
spring:
  profiles:
    active: security,throttle  # Add throttle profile
  
security:
  throttle:
    enabled: true
    max-attempts: 5
    lockout-duration-minutes: 15
    reset-window-minutes: 30
```

#### Step 4: Update Login Controller

Update `backend-services/auth-service/src/main/java/io/provenly/auth/controller/AuthController.java`:

```java
// Add this method to handle login
@PostMapping("/login")
public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
    try {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );
        
        // Generate JWT token
        String token = jwtProvider.generateToken(authentication);
        
        return ResponseEntity.ok(new LoginResponse(token));
    } catch (AccountLockedException e) {
        // Return 429 Too Many Requests with retry-after header
        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", String.valueOf(e.getSecondsUntilUnlock()));
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .headers(headers)
            .body(null);
    }
}
```

#### Step 5: Build and Deploy

```bash
# Navigate to auth-service
cd backend-services/auth-service

# Clean build
./gradlew clean build -x test

# Run with throttling enabled
./gradlew bootRun --args='--spring.profiles.active=security,throttle'

# Or build Docker image
docker build -f Dockerfile -t employmentvc/auth-service:v1-throttled .

# Deploy with docker-compose
COMPOSE_PROJECT_NAME=employmentvc docker compose up -d auth-service
```

### Testing Failed Login Throttling

```bash
# Test 1: Successful login (should work)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"correct_password"}'

# Test 2: Failed login (should show 401 + remaining attempts)
for i in {1..5}; do
  echo "Attempt $i:"
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"user1","password":"wrong_password"}'
  echo ""
done

# Test 3: Account should be locked (return 429)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"any_password"}'
# Should return: 429 Too Many Requests
# Header: Retry-After: 900 (seconds)

# Test 4: Check admin throttling status
curl -X GET "http://localhost:8080/api/admin/security/throttle/status/user1?ipAddress=127.0.0.1" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Test 5: Unlock account (admin only)
curl -X POST "http://localhost:8080/api/admin/security/throttle/unlock/user1?ipAddress=127.0.0.1" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Test 6: Verify account unlocked
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"correct_password"}'
# Should work again
```

### Monitoring Login Throttling

```java
// Create Prometheus metrics
@Bean
public MeterRegistry meterRegistry() {
    MeterRegistry registry = new SimpleMeterRegistry();
    
    // Track locked accounts
    Gauge.builder("security.throttle.locked.accounts", throttleManager::getLockedCount)
        .description("Number of locked accounts")
        .register(registry);
    
    // Track failed attempts
    Counter.builder("auth.login.failed.total")
        .description("Total failed login attempts")
        .register(registry);
    
    return registry;
}
```

### Alerting

Set up Grafana alerts for:

```
Threshold: > 10 locked accounts
Severity: WARNING
Action: Page on-call support

Threshold: > 50 failed attempts in 5 minutes
Severity: CRITICAL
Action: Page on-call security
```

---

## 4. Integration Testing

### Full Security Stack Test

```bash
#!/bin/bash
# test-phase2-security.sh

echo "=== Phase 2 Security Testing ==="

echo "1. Testing ModSecurity WAF..."
curl -v "https://localhost/api/test?id=1' OR '1'='1" -k 2>&1 | grep -i "403\|blocked"

echo "2. Testing Failed Login Throttling..."
for i in {1..6}; do
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"testuser","password":"wrong"}' 2>/dev/null
done

echo "3. Checking CrowdSec Alerts..."
docker exec employmentvc-crowdsec cscli alerts list

echo "4. Viewing Security Metrics..."
curl -s http://localhost:8080/metrics | grep security

echo "=== Tests Complete ==="
```

---

## 5. Security Verification Checklist

### ModSecurity
- [ ] nginx is listening on port 443 (HTTPS)
- [ ] API Gateway is behind nginx (not directly accessible)
- [ ] SSL certificates are valid
- [ ] ModSecurity rules are loaded (check error.log)
- [ ] Custom EmploymentVC rules are enabled
- [ ] Rate limiting is working (test with loop)
- [ ] Logs are being written to /var/log/modsecurity/

### CrowdSec
- [ ] All three services running (agent, bouncer, UI)
- [ ] Logs are being analyzed
- [ ] CrowdSec UI accessible at localhost:3001
- [ ] Community blocklist updated
- [ ] Bouncer is configured to block IPs
- [ ] Decisions are being logged

### Failed Login Throttling
- [ ] FailedLoginThrottleManager is injected in auth-service
- [ ] ThrottledAuthenticationProvider is registered
- [ ] Failed attempts are tracked per user + IP
- [ ] Account locks after 5 attempts
- [ ] 15-minute lockout is enforced
- [ ] Admin can unlock accounts
- [ ] Metrics are exported to Prometheus

---

## 6. Troubleshooting

### ModSecurity Issues

**nginx fails to start:**
```bash
docker logs employmentvc-waf

# Common issue: Missing OWASP rules
# Solution: Download rules
docker exec employmentvc-waf /usr/local/modsecurity/inclusions download
```

**Legitimate requests blocked:**
```bash
# Check which rule triggered
docker exec employmentvc-waf tail -f /var/log/modsecurity/audit.log | jq '.id'

# Disable problematic rule
echo "SecRuleRemoveById 941160" >> infra/security/modsecurity/custom-rules.conf

# Reload nginx
docker exec employmentvc-waf nginx -s reload
```

### CrowdSec Issues

**No alerts being generated:**
```bash
# Check if logs are being read
docker exec employmentvc-crowdsec tail -f /var/log/crowdsec/crowdsec.log

# Verify log file permissions
docker exec employmentvc-crowdsec ls -la /var/log/nginx/
```

**API key expired:**
```bash
# Regenerate bouncer key
docker exec employmentvc-crowdsec cscli bouncers delete nginx-bouncer
docker exec employmentvc-crowdsec cscli bouncers add nginx-bouncer -o json
```

### Failed Login Throttling Issues

**Account not locking:**
```bash
# Check if FailedLoginThrottleManager is enabled
docker logs employmentvc-auth-service | grep -i throttle

# Verify ClientIpResolver is working
docker logs employmentvc-auth-service | grep -i "Client IP"
```

**Legitimate users locked:**
```bash
# Use admin endpoint to unlock
curl -X POST "http://localhost:8080/api/admin/security/throttle/unlock/username" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Or reduce MAX_FAILED_ATTEMPTS in code and redeploy
```

---

## 7. Next Steps After Phase 2

### Immediate (Week 1)
- [ ] Deploy and test all three components
- [ ] Verify logs are being collected
- [ ] Configure alerting rules
- [ ] Train team on new tools

### Short-term (Weeks 2-4)
- [ ] Integrate CrowdSec with Prometheus
- [ ] Create Grafana dashboards
- [ ] Document runbooks for security events
- [ ] Start analyzing CrowdSec alerts

### Medium-term (Phase 3)
- [ ] Deploy HashiCorp Vault
- [ ] Implement JWT token rotation
- [ ] Setup ELK Stack for centralized logging
- [ ] Enable mTLS between services

---

## Resources

- [ModSecurity Documentation](https://modsecurity.org/)
- [OWASP ModSecurity CRS](https://github.com/coreruleset/coreruleset)
- [CrowdSec Documentation](https://docs.crowdsec.net/)
- [nginx Documentation](https://nginx.org/en/docs/)
- [Spring Security Throttling](https://spring.io/projects/spring-security)

---

**Version**: 1.0
**Last Updated**: February 18, 2026
