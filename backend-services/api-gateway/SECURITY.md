# Security Configuration for EmploymentVC API Gateway

This document outlines the security features implemented in the API Gateway.

## Overview

The API Gateway is the entry point for all external traffic. It implements multiple layers of security to prevent common attacks:

- **DDoS Protection**: Rate limiting per IP
- **DoS Protection**: Request size limits and timeouts
- **Injection Prevention**: Input validation and sanitization
- **XSS Prevention**: Content Security Policy headers
- **Clickjacking Protection**: X-Frame-Options header
- **MIME Type Sniffing**: X-Content-Type-Options header

## Implemented Filters

### 1. Rate Limiting Filter

**Purpose**: Prevent DDoS and brute force attacks

**Configuration**:
- 100 requests per minute per IP
- Token bucket algorithm
- Automatic cache cleanup

**Response on limit exceeded**:
```json
{
  "error": "Rate limit exceeded. Please try again later."
}
```

**Status Code**: 429 Too Many Requests

### 2. Size Limit Filter

**Purpose**: Prevent buffer overflow and memory exhaustion attacks

**Limits**:
- Maximum request size: 10MB (configurable)
- Maximum header size: 16KB

**Response on limit exceeded**:
```json
{
  "error": "Request entity too large"
}
```

**Status Codes**:
- 413 Payload Too Large
- 431 Request Header Fields Too Large

### 3. Security Headers Filter

**Purpose**: Prevent common client-side attacks

**Headers Added**:

```
X-Content-Type-Options: nosniff
  Prevents MIME type sniffing attacks

X-Frame-Options: DENY
  Prevents clickjacking attacks

X-XSS-Protection: 1; mode=block
  Enables XSS filter in browser

Referrer-Policy: strict-origin-when-cross-origin
  Controls referrer information leak

Content-Security-Policy: [strict policy]
  Prevents injection attacks

Strict-Transport-Security: max-age=31536000
  Forces HTTPS (only on secure connections)

Permissions-Policy: geolocation=(), microphone=(), camera=()
  Disables unnecessary browser features
```

## Input Validation

### InputValidator Component

Validates against common injection patterns:

**Checks for**:
1. **SQL Injection**: SELECT, INSERT, UPDATE, DELETE, UNION, etc.
2. **XSS**: <script>, javascript:, onerror, onload, etc.
3. **Path Traversal**: ../, ..\, %2e%2e
4. **Command Injection**: &, ;, |, `, $,  (), \n, \r
5. **XXE (XML External Entity)**: <!ENTITY, SYSTEM, PUBLIC

**Usage**:
```java
@Autowired
private InputValidator inputValidator;

// Validate input
if (!inputValidator.validateInput(userInput, "username")) {
    throw new InvalidInputException("Suspicious input detected");
}

// Sanitize output
String safe = inputValidator.htmlEncode(userInput);
```

## Configuration

Edit `application-security.properties`:

```properties
# Rate limiting
security.rate-limit.enabled=true
security.rate-limit.requests-per-minute=100

# Size limits (in bytes)
security.size-limit.max-request-size=10485760  # 10MB
security.size-limit.max-header-size=16384      # 16KB

# CORS allowed origins (comma-separated)
cors.allowed-origins=http://localhost:3000

# Request timeout (seconds)
server.servlet.session.timeout=1800

# Thread pool
server.tomcat.max-threads=200
```

## Dependencies Added

```gradle
// Rate limiting
implementation 'io.github.bucket4j:bucket4j-core:7.6.0'

// Input validation is built-in with Spring Validation
implementation 'org.springframework.boot:spring-boot-starter-validation'
```

## Integration with Dependencies

### Keycloak Integration
- OAuth2/OIDC token validation in SecurityContextHolder
- User context available for audit logging

### Observability
- Rate limit violations logged to Prometheus
- Security events tracked in metrics
- Grafana dashboards for monitoring

### Monitoring Example

```java
@GetMapping("/api/v1/status")
public ResponseEntity<Map<String, Object>> getStatus() {
    return ResponseEntity.ok(Map.of(
        "security_filters", "active",
        "rate_limiting", "enabled",
        "timestamp", System.currentTimeMillis()
    ));
}
```

## Testing Security

### Test Rate Limiting
```bash
# Rapid requests to trigger rate limit
for i in {1..150}; do curl http://localhost:8080/api/v1/health; done
```

### Test Size Limit
```bash
# Create large payload
dd if=/dev/zero bs=1M count=15 of=large.bin

# Send oversized request
curl -X POST -H "Content-Type: application/octet-stream" \
  --data-binary @large.bin http://localhost:8080/api/v1/test
```

### Test Security Headers
```bash
curl -I http://localhost:8080/api/v1/health | grep -i "security\|x-"
```

Expected output:
```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
...
```

## Production Deployment

### Before Going Live

1. **Update Configuration**:
   ```properties
   # In application.yml
   spring.security.require-ssl=true  # Enforce HTTPS only
   cors.allowed-origins=https://yourdomain.com
   ```

2. **Enable Additional Security**:
   - Set up WAF (Web Application Firewall)
   - Configure DDoS protection service
   - Enable audit logging
   - Setup intrusion detection

3. **Monitor Security Metrics**:
   - Rate limit hits per IP
   - Suspicious input patterns
   - Failed security checks

### Monitoring Endpoints

```
GET /actuator/metrics/http.server.requests
GET /actuator/metrics/security.rate.limit.hits
GET /actuator/health
GET /actuator/prometheus
```

Access metrics in Prometheus:
```
http_server_requests_seconds_count{status="429"}
```

## Future Enhancements

1. **WAF Integration**: ModSecurity or similar
2. **DDoS Service**: Cloudflare, AWS Shield, or CrowdSec
3. **Secrets Management**: HashiCorp Vault
4. **Encryption**: TLS 1.3 enforcement
5. **Audit Logging**: Detailed security event logs

## Related Documentation

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security](https://spring.io/projects/spring-security)
- [Bucket4j Rate Limiting](https://github.com/vladimir-bukhtoyarov/bucket4j)
- [Spring Cloud Gateway Security](https://cloud.spring.io/spring-cloud-gateway/reference/html/#writing-custom-filters)

## Support

For security concerns or vulnerabilities:
1. Do not open public issues
2. Email: security@provenly.io
3. Review SECURITY.md in repository root
