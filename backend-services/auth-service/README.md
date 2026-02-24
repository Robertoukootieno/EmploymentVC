# Auth Service

## Overview
The Auth Service provides authentication and authorization for the EmploymentVC platform, supporting both traditional OIDC and Web3/DID-based authentication.

## Architecture Role
- **Tier**: Core Service
- **Port**: 8081
- **Type**: Spring Boot Application

## Responsibilities
- User authentication (OIDC + DID Auth)
- Session management
- Token issuance and validation (JWT)
- Integration with Keycloak
- DID-based challenge-response authentication
- Multi-factor authentication (MFA)
- Role-based access control (RBAC)

## Authentication Methods

### 1. OIDC/OAuth2 (via Keycloak)
Traditional username/password authentication through Keycloak integration.

**Flow:**
1. User redirects to Keycloak login
2. User authenticates
3. Keycloak issues tokens
4. Auth service validates and enriches tokens

### 2. DID Authentication (Web3 Login)
Decentralized authentication using W3C DIDs and verifiable credentials.

**Flow:**
1. Client requests challenge
2. Auth service generates random challenge
3. Client signs challenge with DID private key
4. Auth service verifies signature via DID Document
5. Issues JWT with DID as subject

## Key Features

### Security
- Stateless JWT tokens
- Refresh token rotation
- DID-based challenge expiry (5 minutes)
- Session invalidation
- Brute force protection
- Account lockout mechanisms

### Integration
- **Keycloak**: OIDC provider
- **DID Registry**: DID resolution for DID Auth
- **OPA**: Policy enforcement
- **Vault**: Secret management

### Observability
- Authentication metrics
- Failed login tracking
- Session analytics
- Distributed tracing

## Configuration

### Environment Variables
| Variable | Description | Required |
|----------|-------------|----------|
| `DATABASE_URL` | PostgreSQL connection string | Yes |
| `KEYCLOAK_URL` | Keycloak server URL | Yes |
| `KEYCLOAK_REALM` | Keycloak realm name | Yes |
| `KEYCLOAK_CLIENT_SECRET` | OAuth client secret | Yes |
| `JWT_SECRET` | JWT signing secret | Yes |
| `DID_RESOLVER_URL` | DID resolver service URL | Yes |

## API Endpoints

### OIDC Authentication
```
POST /auth/login
POST /auth/logout
POST /auth/refresh
GET /auth/user
```

### DID Authentication
```
POST /auth/did/challenge
POST /auth/did/verify
GET /auth/did/session
```

### Session Management
```
GET /auth/sessions
DELETE /auth/sessions/{sessionId}
```

## Database Schema

### Tables
- `users`: User accounts
- `sessions`: Active sessions
- `did_challenges`: Temporary auth challenges
- `refresh_tokens`: Refresh token tracking
- `audit_log`: Authentication events

## Security Policies

### OPA Integration
Auth service enforces policies defined in `/security/opa-policies/access.rego`:
- User role validation
- Permission checks
- Rate limiting
- Suspicious activity detection

## Build & Run

### Local Development
```bash
./gradlew :backend-services:auth-service:bootRun
```

### Run with PostgreSQL
```bash
COMPOSE_PROJECT_NAME=employmentvc docker compose up -d postgres keycloak
./gradlew :backend-services:auth-service:bootRun
```

### Build Docker Image
```bash
docker build -t employmentvc/auth-service:latest -f backend-services/auth-service/Dockerfile .
```

## Testing

### Unit Tests
```bash
./gradlew :backend-services:auth-service:test
```

### Integration Tests
Requires running PostgreSQL and Keycloak instances.

## Monitoring

### Metrics
- `auth_login_attempts_total`: Total login attempts
- `auth_login_failures_total`: Failed logins
- `auth_sessions_active`: Active sessions count
- `auth_did_challenges_issued`: DID challenges issued
- `auth_token_issued_total`: JWTs issued

### Alerts
- High failed login rate
- Unusual authentication patterns
- Token validation failures

## Security Hardening

### Production Checklist
- [ ] Use strong JWT secret (256+ bits)
- [ ] Enable HTTPS only
- [ ] Configure Keycloak with TLS
- [ ] Use Vault for secret management
- [ ] Enable audit logging
- [ ] Configure rate limiting
- [ ] Set up MFA for admin accounts
- [ ] Regular security audits

## Troubleshooting

### Common Issues

**Issue**: Keycloak connection failed
```bash
# Check Keycloak availability
curl http://keycloak:8180/auth/realms/employmentvc

# Verify realm configuration
```

**Issue**: DID authentication fails
```bash
# Check DID resolver service
curl http://did-registry:8082/dids/{did}

# Verify DID document has valid verification method
```

## Compliance
- GDPR compliant (user consent, data portability)
- SOC 2 Type II controls
- OAuth 2.0 / OIDC specification compliant
- W3C DID specification compliant

## References
- [OAuth 2.0 RFC 6749](https://tools.ietf.org/html/rfc6749)
- [OIDC Specification](https://openid.net/specs/openid-connect-core-1_0.html)
- [W3C DID Core](https://www.w3.org/TR/did-core/)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
