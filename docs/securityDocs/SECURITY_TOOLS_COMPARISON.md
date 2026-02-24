½# Open-Source Security Tools used for EmploymentVC

## Executive Summary

This document contains open-source security tools to protect EmploymentVC against common attacks:
- **DDoS/DoS**: Rate limiting + WAF + IDS
- **Injections**: Input validation + WAF rules
- **Data breaches**: TLS + Secrets management
- **Credential theft**: JWT rotation + MFA

---

## 1. Rate Limiting & API Protection

### ✅ Already Implemented (In API Gateway)

**Tool**: Bucket4j (Rate Limiting)
- Per-IP rate limiting: 100 req/min
- Token bucket algorithm
- Request size limits: 10MB
- Automatic cleanup

### ➕ Recommended Addition: Resilience4j

**Purpose**: Advanced rate limiting, circuit breakers, retry logic

```java
// Add to build.gradle
implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.1.0'
implementation 'io.github.resilience4j:resilience4j-ratelimiter:2.1.0'

// Usage
@RateLimiter(name = "credentials", fallbackMethod = "rateLimitFallback")
public CredentialResponse issueCredential(CredentialRequest request) {
    // implementation
}

public CredentialResponse rateLimitFallback(CredentialRequest request, 
                                            io.github.resilience4j.ratelimiter.RequestNotPermitted exc) {
    throw new ServiceUnavailableException("Service temporarily unavailable");
}
```

**Why**: Better than Bucket4j for microservices with fallback handling

---

## 2. Web Application Firewall (WAF)

### Option 1: ModSecurity (Open Source) ⭐ Recommended

**Setup**: Self-hosted nginx proxy with ModSecurity

```dockerfile
# Dockerfile.modsecurity
FROM owasp/modsecurity:latest

# Install OWASP CRS (Core Rule Set)
RUN git clone https://github.com/coreruleset/coreruleset.git /etc/modsecurity/rules/crs

# Copy custom rules
COPY ./modsec-rules/ /etc/modsecurity/rules/

# Run
CMD ["nginx", "-g", "daemon off;"]
```

**OWASP CRS Detects**:
- SQL injection
- XSS attacks
- File uploads
- Protocol attacks
- Suspicious patterns

**Why ModSecurity**:
- ✅ Open source (no licensing)
- ✅ Widely adopted
- ✅ OWASP Core Rule Set included
- ✅ Works with nginx/Apache
- ✅ Community support

### Option 2: CrowdSec (Modern Alternative)

**Comparison**:
| Feature | ModSecurity | CrowdSec |
|---------|------------|----------|
| Setup | Complex | Simple |
| Rules | Static OWASP | Dynamic community |
| Detection | Pattern-based | AI-based |
| Community | Large | Growing |
| Docker | Yes | Yes |
| Self-hosted | Yes | Yes |

**Installation**:
```bash
docker run -d \
  -e CROWDSEC_REGISTRATION_KEY=<key> \
  -v /var/log:/var/log:ro \
  --name crowdsec \
  crowdsecurity/crowdsec:latest
```

---

## 3. Intrusion Detection/Prevention System (IDS/IPS)

### Option 1: CrowdSec ⭐ Recommended

**Why Recommended**:
- ✅ Modern, lightweight
- ✅ Community threat intelligence (free)
- ✅ Automatic IP banning
- ✅ Works with Docker/K8s
- ✅ Real-time detection

**What it Detects**:
- Brute force attempts
- Port scanning
- DDoS patterns
- Exploit attempts
- Malicious traffic

### Option 2: Suricata

**If you need**: Advanced threat detection with recording

```bash
docker run -d \
  --network host \
  -v /var/log:/var/log \
  jasonish/suricata:latest
```

### Option 3: Falco (For K8s Runtime)

**If you need**: Kubernetes-native intrusion detection

```yaml
# helm install falco falcosecurity/falco
# Detects suspicious container behavior
```

---

## 4. Secrets Management

### Option 1: HashiCorp Vault ⭐ Recommended

**Setup**:
```yaml
# docker-compose.prod.yml
vault:
  image: vault:latest
  environment:
    VAULT_DEV_ROOT_TOKEN_ID: myroot
  ports:
    - "8200:8200"
  volumes:
    - vault_data:/vault/data
```

**Features**:
- Centralized secret storage
- Automatic secret rotation
- Encryption at rest
- Audit logging
- Kubernetes integration
- Database credential generation

**Spring Boot Integration**:
```java
// pom.xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-vault-config</artifactId>
    <version>3.1.0</version>
</dependency>

// application.yml
spring:
  cloud:
    vault:
      host: vault
      port: 8200
      scheme: http
      authentication: TOKEN
      token: myroot
```

### Option 2: Sealed Secrets (K8s Native)

**If you**: Only deploy to Kubernetes

```bash
# Installation
helm repo add sealed-secrets https://bitnami-labs.github.io/sealed-secrets
helm install sealed-secrets -n kube-system sealed-secrets/sealed-secrets
```

---

## 5. Container Security

### Tool: Trivy (Already In CI/CD) ✅

**You already have**: Trivy vulnerability scanning

**Enhance with**:
```yaml
# .github/workflows/ci-cd.yml
- name: Trivy scan for HIGH/CRITICAL
  run: trivy image --severity HIGH,CRITICAL myimage:latest
  
- name: Snyk scan
  uses: snyk/actions/docker@master
  env:
    SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
```

### Tool: Grype (Supply Chain Scanning)

```bash
# scan container image
grype ghcr.io/robertoukootieno/employmentvc/api-gateway:latest

# scan directory
grype ./backend-services/
```

---

## 6. Network Security (Service Mesh)

### Option: Istio (Advanced) 

**If you need**: Advanced service-to-service security

```bash
# Installation
helm repo add istio https://istio-release.storage.googleapis.com/charts
helm install istio-base istio/base -n istio-system --create-namespace
helm install istiod istio/istiod -n istio-system
```

**Security Features**:
- mTLS between services (automatic)
- Network policies
- Distributed rate limiting
- Request authentication
- Authorization rules

**Example**:
```yaml
apiVersion: "security.istio.io/v1beta1"
kind: "PeerAuthentication"
metadata:
  name: "default"
spec:
  mtls:
    mode: STRICT  # Require mTLS for all traffic
```

---

## 7. DDoS Protection (Cloud-Level)

### Option 1: Cloudflare (Recommended)

**Why**:
- ✅ Free tier available
- ✅ Automatic DDoS mitigation
- ✅ WAF included
- ✅ Easy setup (DNS only)
- ✅ Analytics dashboard

**Setup**: 
1. Point DNS to Cloudflare
2. Enable DDoS protection (automatic)
3. Configure WAF rules

### Option 2: AWS Shield/WAF

**If deployed on**: AWS infrastructure

```bash
# AWS WAF with rate limiting
aws wafv2 create-rule-group \
  --name "rate-limit-rule" \
  --scope REGIONAL \
  --capacity 10
```

---

## 8. Audit Logging & Forensics

### Tool: ELK Stack (Elasticsearch, Logstash, Kibana)

**You already have**: Prometheus + Grafana

**Add ELK for**:
- Centralized logging
- Forensic analysis
- Security event tracking
- Compliance auditing

```yaml
# docker-compose.additions.yml
elasticsearch:
  image: docker.elastic.co/elasticsearch/elasticsearch:8.0.0
  environment:
    - discovery.type=single-node

logstash:
  image: docker.elastic.co/logstash/logstash:8.0.0
  volumes:
    - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf

kibana:
  image: docker.elastic.co/kibana/kibana:8.0.0
  ports:
    - "5601:5601"
```

---

## 9. Configuration Management

### Tool: OPA (Open Policy Agent)

**You already use**: OPA for policy enforcement

**Recommended additions**:
- Network policies
- Image registry validation
- Runtime security policies
- API rate limit policies

```rego
# policies/rate-limit.rego
package api.ratelimit

allow {
    input.requests_per_minute <= 100
}

deny {
    input.requests_per_minute > 100
}
```

---

## 10. Recommended Stack Summary

### Minimum (Small/Dev)

```
┌─────────────────────┐
│   Cloudflare CDN    │ (DDoS protection)
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│   Spring Boot API   │ (Rate limiting + validation)
│   + Bucket4j        │
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│   PostgreSQL + TLS  │ (Encrypted DB)
└─────────────────────┘
```

### Recommended (Production)

```
┌────────────────────────────────────────────┐
│         Cloudflare / AWS Shield             │ (DDoS)
└──────────────┬───────────────────┬──────────┘
               │                   │
       ┌───────▼────────┐  ┌──────▼──────────┐
       │  ModSecurity   │  │  CrowdSec IDS   │ (WAF + Detection)
       │  (nginx+WAF)   │  │                 │
       └───────┬────────┘  └──────┬──────────┘
               │                   │
        ┌──────▼─────────────────────▼────┐
        │     Spring Boot API Gateway      │
        │  + Bucket4j / Resilience4j       │
        │  + Input Validation              │
        └──────┬────────────────────┬──────┘
               │                    │
        ┌──────▼────┐    ┌──────────▼──────┐
        │  Services  │    │  Vault + Secrets│
        │ (mTLS +    │    │  Management     │
        │  Istio)    │    │                 │
        └──────┬────┘    └──────┬───────────┘
               │                │
        ┌──────▼────────────────▼──────┐
        │   Postgres + Vault Creds      │ (Encrypted)
        │   + Network Policies          │
        └────────────────────────────────┘
```

---

## 11. Integration Checklist

- [x] **API Gateway**: Rate limiting + input validation (Done)
- [ ] **WAF**: ModSecurity or Cloudflare (Recommended)
- [ ] **IDS**: CrowdSec (Recommended)
- [ ] **Secrets**: HashiCorp Vault (Phase 3)
- [ ] **Logging**: ELK Stack (Phase 3)
- [ ] **Service Mesh**: Istio (Optional, advanced)
- [ ] **DDoS**: Cloudflare (Phase 2)
- [ ] **Image Scanning**: Trivy + Snyk (Already in CI/CD)

---

## 12. Cost Analysis (Open Source)

| Tool | Cost | Effort | Value |
|------|------|--------|-------|
| Bucket4j | Free | Low | High |
| ModSecurity | Free | Medium | High |
| CrowdSec | Free (Community) | Low | High |
| Vault | Free (Open Source) | Medium | High |
| ELK | Free | High | High |
| Istio | Free | High | Medium |
| OPA | Free | Medium | High |

**Note**: All recommended tools are 100% open source

---

## 13. Getting Started

### Week 1: Core Implementation (Already Done)
- ✅ API Gateway security filters

### Week 2-3: Add WAF
```bash
# Option A: ModSecurity
cd infra/docker
./setup-modsecurity.sh

# Option B: Cloudflare (5 min setup)
# Point DNS to Cloudflare
```

### Week 4: Add CrowdSec
```bash
COMPOSE_PROJECT_NAME=employmentvc-security docker compose -f infra/security/crowdsec-compose.yml up -d
```

### Month 2: Add Vault
```bash
COMPOSE_PROJECT_NAME=employmentvc-vault docker compose -f infra/security/vault-compose.yml up -d
```

---

## 14. References

- [OWASP Top 10](https://owasp.org/Top10/)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)
- [CIS Controls](https://www.cisecurity.org/cis-controls/)
- [ModSecurity](https://modsecurity.org/)
- [CrowdSec](https://www.crowdsec.net/)
- [HashiCorp Vault](https://www.vaultproject.io/)
- [OPA (Open Policy Agent)](https://www.openpolicyagent.org/)
- [Istio Service Mesh](https://istio.io/)

---

## Contact & Support

For security implementations, reach out:
- **Security Questions**: security@provenly.io
- **DevOps Support**: devops@provenly.io
- **Community**: #security-implementation Slack

---

**Last Updated**: February 18, 2026  
**Version**: 1.0
