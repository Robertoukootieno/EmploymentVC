# API Gateway

## Overview
The API Gateway serves as the single entry point for all client requests to the EmploymentVC platform. It handles routing, authentication, rate limiting, and provides a unified API interface.

## Architecture Role
- **Tier**: Gateway/Edge
- **Port**: 8080
- **Type**: Spring Cloud Gateway

## Responsibilities
- Route requests to appropriate backend microservices
- Authentication and authorization enforcement
- Rate limiting and throttling
- CORS handling
- Request/response transformation
- API versioning
- Circuit breaking
- Load balancing

## Key Features

### Routing
Routes are configured for all backend services:
- `/api/v1/auth/**` → Auth Service
- `/api/v1/issuer/**` → Issuer Service
- `/api/v1/verifier/**` → Verifier Service
- `/api/v1/wallet/**` → Custodial Wallet Service
- `/api/v1/did/**` → DID Registry
- `/api/v1/schemas/**` → Schema Registry
- `/api/v1/workflow/**` → Workflow Service

### Security
- JWT token validation
- Integration with Keycloak for OIDC
- DID-based authentication support
- Request signing verification

### Observability
- Prometheus metrics at `/actuator/prometheus`
- Distributed tracing with OpenTelemetry
- Structured logging
- Health checks at `/actuator/health`

## Configuration

### Environment Variables
| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Service port | `8080` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` |
| `AUTH_SERVICE_URL` | Auth service URL | `http://localhost:8081` |
| `KEYCLOAK_URL` | Keycloak server URL | `http://localhost:8180` |

## Dependencies
- Spring Cloud Gateway
- Spring Security
- Micrometer + Prometheus
- OpenTelemetry

## Build & Run

### Local Development
```bash
./gradlew :backend-services:api-gateway:bootRun
```

### Build Docker Image
```bash
docker build -t employmentvc/api-gateway:latest -f backend-services/api-gateway/Dockerfile .
```

### Run Container
```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  employmentvc/api-gateway:latest
```

## API Documentation
OpenAPI documentation available at: `http://localhost:8080/v3/api-docs`
Swagger UI available at: `http://localhost:8080/swagger-ui.html`

## Health & Monitoring
- Health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Prometheus: `GET /actuator/prometheus`
- Info: `GET /actuator/info`

## Security Considerations
- All requests must include valid JWT or DID auth
- Rate limiting applied per client
- CORS configured for allowed origins only
- TLS/HTTPS required in production
- No sensitive data logged

## Deployment
See Kubernetes manifests: `infra/kubernetes/services/gateway/`

## Contact
For issues or questions, refer to the main project documentation.
