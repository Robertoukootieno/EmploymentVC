# Keycloak Integration for EmploymentVC

## Overview
This directory contains the Keycloak deployment and configuration for local, dev, and test environments. Keycloak provides OIDC/OAuth2 authentication and the "provenly" realm with pre-configured roles, groups, and clients.

## Quick Start

1. **Start Keycloak and PostgreSQL:**
   ```sh
   cp .env.example .env
   COMPOSE_PROJECT_NAME=employmentvc-keycloak docker compose -f docker-compose.keycloak.yml up -d
   ```

2. **Access Keycloak Admin Console:**
   - URL: http://localhost:8092/admin/
   - Username: `admin`
   - Password: `admin` (from `.env`)
   - Wait 15-20 seconds for Keycloak to fully start

3. **Access the Provenly Realm:**
   - Realm URL: http://localhost:8092/realms/provenly
   - OIDC Discovery: http://localhost:8092/realms/provenly/.well-known/openid-configuration

## Realm Configuration

The **"provenly"** realm is automatically imported on startup with:

### Roles
- **ADMIN** - Platform administrator with full access
- **ISSUER** - Organization that can issue employment credentials
- **VERIFIER** - Entity that can verify employment credentials
- **HOLDER** - Individual who holds employment credentials

### Groups
- **Organizations** - Issuers with ISSUER role
- **Verifiers** - Verifiers with VERIFIER role
- **Holders** - Holders with HOLDER role

### Authentication Flows
All standard Keycloak authentication flows are configured:
- **browser** - Cookie + interactive form authentication
- **registration** - User self-registration flow
- **direct grant** - Resource Owner Password Credentials (service-to-service)
- **reset credentials** - Password reset flow
- **clients** - Client authentication (secret-basic, JWT, etc.)
- **docker auth** - Docker registry authentication

### Clients
Configured clients for backend services and frontend applications (see realm in admin console for full list).

## Updating the Realm

To export and update the realm configuration:

1. Make changes in the admin console: http://localhost:8092/admin/
2. Export the realm from **Realm Settings** → **Action** → **Export**
3. Download the JSON and replace `realm-export.json`
4. Restart Keycloak:
   ```sh
   COMPOSE_PROJECT_NAME=employmentvc-keycloak docker compose -f docker-compose.keycloak.yml down
   COMPOSE_PROJECT_NAME=employmentvc-keycloak docker compose -f docker-compose.keycloak.yml up -d
   ```

## Environment Variables & Secrets

Configure via `.env` file (copy from `.env.example`):

| Variable | Default | Purpose |
|----------|---------|---------|
| `COMPOSE_PROJECT_NAME` | `employmentvc-keycloak` | Docker Compose project name |
| `KEYCLOAK_HOST_PORT` | `8092` | Host port for external access |
| `KEYCLOAK_DB_HOST_PORT` | `5433` | PostgreSQL host port |
| `KEYCLOAK_ADMIN_PASSWORD` | `admin` | Initial admin password |
| `KEYCLOAK_DB_NAME` | `keycloak` | Database name |
| `KEYCLOAK_DB_USER` | `keycloak` | Database user |
| `KEYCLOAK_DB_PASSWORD` | `keycloak` | Database password |

**For production:** Override with secure secrets via:
- Docker Secrets
- Vault
- CI/CD pipeline environment variables
- Kubernetes Secrets

## Environment Variables for Backend/Frontend OIDC Integration

Add the following variables to your backend and frontend `.env` files (see `.env.example`):

| Variable                        | Example Value                                 | Description                                 |
|----------------------------------|-----------------------------------------------|---------------------------------------------|
| OIDC_ISSUER_URL                  | http://localhost:8092/realms/provenly         | OIDC issuer URL (Keycloak realm)            |
| OIDC_CLIENT_ID                   | backend-service                               | OIDC client ID for backend                  |
| OIDC_CLIENT_SECRET               | your-backend-client-secret                    | OIDC client secret for backend              |
| OIDC_FRONTEND_CLIENT_ID          | frontend-app-web                              | OIDC client ID for frontend                 |
| OIDC_FRONTEND_CLIENT_SECRET      | your-frontend-client-secret                   | OIDC client secret for frontend (if needed) |
| OIDC_REDIRECT_URI                | http://localhost:3000/callback                | OIDC redirect URI for frontend              |
| OIDC_POST_LOGOUT_REDIRECT_URI    | http://localhost:3000/                        | OIDC post-logout redirect URI               |

**Never hardcode these values in code. Always use environment variables or config files.**

## Health Checks

Both services have health checks:
- **Keycloak:** Checks `/realms/master` endpoint (20s interval)
- **PostgreSQL:** Checks database readiness (10s interval)

Verify health:
```sh
COMPOSE_PROJECT_NAME=employmentvc-keycloak docker compose -f docker-compose.keycloak.yml ps
```

## OIDC Integration

### Discovery Endpoint
```
http://localhost:8092/realms/provenly/.well-known/openid-configuration
```

### Authorization Endpoint
```
http://localhost:8092/realms/provenly/protocol/openid-connect/auth
```

### Token Endpoint
```
http://localhost:8092/realms/provenly/protocol/openid-connect/token
```

### Userinfo Endpoint
```
http://localhost:8092/realms/provenly/protocol/openid-connect/userinfo
```

## Common Operations

### Stop Keycloak
```sh
COMPOSE_PROJECT_NAME=employmentvc-keycloak docker compose -f docker-compose.keycloak.yml down
```

### View Logs
```sh
COMPOSE_PROJECT_NAME=employmentvc-keycloak docker compose -f docker-compose.keycloak.yml logs keycloak -f
```

### Fresh Start (Delete Data)
```sh
COMPOSE_PROJECT_NAME=employmentvc-keycloak docker compose -f docker-compose.keycloak.yml down -v
COMPOSE_PROJECT_NAME=employmentvc-keycloak docker compose -f docker-compose.keycloak.yml up -d
```

### List All Docker Compose Projects
```sh
docker compose ls
```

## Port Conflicts

If port 8092 is already in use:

1. Check current usage:
   ```sh
   lsof -i :8092
   ```

2. Either stop the conflicting service or change `KEYCLOAK_HOST_PORT` in `.env` to a different port

## Troubleshooting

### Admin Console Won't Load
- Ensure Keycloak has been running for at least 15 seconds
- Check logs: `COMPOSE_PROJECT_NAME=employmentvc-keycloak docker compose logs keycloak`
- Verify the auth server URL is correct: `http://localhost:8092`

### Database Connection Issues
- Verify PostgreSQL is running: `COMPOSE_PROJECT_NAME=employmentvc-keycloak docker compose ps`
- Check database logs: `COMPOSE_PROJECT_NAME=employmentvc-keycloak docker compose logs keycloak-db`
- Ensure database credentials in `.env` match the compose file

### Realm Won't Import
- Validate `realm-export.json` is valid JSON
- Check Keycloak logs for import errors
- Ensure all referenced authentication flows are defined in the export file

## Production Deployment

For production environments:

1. **Use Kubernetes** with the manifests in `infra/k8s/`
2. **Enable HTTPS** with valid certificates
3. **Use managed PostgreSQL** (AWS RDS, Azure Database, etc.)
4. **Externalize secrets** via:
   - Kubernetes Secrets
   - HashiCorp Vault
   - Cloud provider secret managers
5. **Configure proper logging and monitoring**
6. **Set resource limits and requests**
7. **Use readiness/liveness probes** (included in k8s manifests)
8. **Disable development mode** - set `KC_LOG_LEVEL=WARN` or `ERROR`

## References

- [Keycloak Official Docs](https://www.keycloak.org/documentation)
- [Keycloak Docker Guide](https://www.keycloak.org/server/containers)

- Start stack:
   ```sh
   COMPOSE_PROJECT_NAME=employmentvc-keycloak docker compose -f docker-compose.keycloak.yml up -d
   ```
- Stop stack cleanly:
   ```sh
   COMPOSE_PROJECT_NAME=employmentvc-keycloak docker compose -f docker-compose.keycloak.yml down
   ```
- Inspect all Compose projects on your machine:
   ```sh
   docker compose ls
   ```
- Remove stale/exited containers occasionally:
   ```sh
   docker container prune
   ```

## References
- [Keycloak Docs](https://www.keycloak.org/documentation)
- [Keycloak Docker](https://www.keycloak.org/server/containers)
- [Keycloak OIDC](https://www.keycloak.org/docs/latest/securing_apps/#openid-connect)
