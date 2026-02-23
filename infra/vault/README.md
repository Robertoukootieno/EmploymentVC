# EmploymentVC Vault Infrastructure

This directory contains configuration and deployment files for running HashiCorp Vault as a secrets management solution for EmploymentVC.

## Files
- `vault-config.hcl`: Main Vault configuration (listener, storage, UI, logging)
- `docker-compose.vault.yml`: Compose file to run Vault in a container

## Quick Start
```bash
cd infra/vault
# Start Vault (development mode, no TLS)
COMPOSE_PROJECT_NAME=employmentvc-vault docker compose -f docker-compose.vault.yml up -d
# Access UI: http://localhost:8200 (token: root)
```

## Production Notes
- For production, set `tls_disable = 0` in `vault-config.hcl` and configure certificates.
- Use a secure storage backend (e.g., Consul, cloud storage) instead of file storage.
- Remove the default root token and enable authentication methods (AppRole, JWT, etc).

## Integration
- See PHASE3_DEPLOYMENT_GUIDE.md for Spring Boot integration and secret storage examples.

## Clean Up
- Remove `.gitkeep` after adding real files to this directory.

## Network Setup

If you see the error:
```
network employmentvc-security declared as external, but could not be found
```
Create the external Docker network before starting Vault:
```bash
docker network create employmentvc-security
```
Then start Vault:
```bash
COMPOSE_PROJECT_NAME=employmentvc-vault docker compose -f docker-compose.vault.yml up -d
```
This ensures Vault can connect to the correct network for secure service communication.
