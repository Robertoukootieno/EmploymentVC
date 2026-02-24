#!/usr/bin/env bash

# Vault Configuration Script for EmploymentVC
# Sets up secrets, auth methods, and policies

set -euo pipefail

VAULT_ADDR="${VAULT_ADDR:-http://localhost:8200}"
VAULT_TOKEN="${VAULT_TOKEN:-}"

if [ -z "$VAULT_TOKEN" ]; then
  echo "Error: VAULT_TOKEN not set"
  exit 1
fi

export VAULT_ADDR
export VAULT_TOKEN

echo "Configuring Vault at $VAULT_ADDR"

# 1. Enable necessary auth methods
echo "Enabling auth methods..."
vault auth enable kubernetes 2>/dev/null || true
vault auth enable jwt 2>/dev/null || true

# 2. Create secret engines
echo "Creating secret engines..."
vault secrets enable -path=employmentvc/data kv-v2 2>/dev/null || true
vault secrets enable pki 2>/dev/null || true

# 3. Store database credentials
echo "Storing database credentials..."
vault kv put employmentvc/data/database/postgres \
  host="postgres" \
  port="5432" \
  username="app_user" \
  password="changeme_app_password"

vault kv put employmentvc/data/database/redis \
  host="redis" \
  port="6379" \
  password="changeme_redis_password"

# 4. Store API credentials
echo "Storing API credentials..."
vault kv put employmentvc/data/api-keys/keycloak \
  client_id="employmentvc-admin" \
  client_secret="changeme_keycloak_secret" \
  realm_url="http://keycloak:8092"

vault kv put employmentvc/data/api-keys/blockchain \
  rpc_url="http://besu-node:8545" \
  ws_url="ws://besu-node:8546"

# 5. Store encryption keys
echo "Storing encryption keys..."
vault kv put employmentvc/data/credentials/encryption \
  master_key="$(openssl rand -base64 32)" \
  jwe_key="$(openssl rand -base64 32)"

# 6. Store signing keys
echo "Storing signing keys..."
vault kv put employmentvc/data/credentials/signing \
  private_key="@/etc/secrets/signing.key" \
  key_id="vc-signing-key-2026"

# 7. Create policies
echo "Creating Vault policies..."
for policy_name in auth wallet issuer verifier gateway; do
  vault policy write "employmentvc-$policy_name" \
    "/etc/vault/policies/${policy_name}-policy.hcl"
done

# 8. Configure Kubernetes auth
echo "Configuring Kubernetes authentication..."
vault write auth/kubernetes/config \
  kubernetes_host="https://kubernetes.default.svc" \
  kubernetes_ca_cert=@/var/run/secrets/kubernetes.io/serviceaccount/ca.crt \
  token_reviewer_jwt=@/var/run/secrets/kubernetes.io/serviceaccount/token

# 9. Create Kubernetes auth roles
for role in auth-service wallet-api issuer-api verifier-api api-gateway; do
  vault write "auth/kubernetes/role/$role" \
    "bound_service_account_names=$role" \
    "bound_service_account_namespaces=provenly" \
    "policies=employmentvc-$role" \
    "ttl=1h"
done

echo "✓ Vault configuration complete"
