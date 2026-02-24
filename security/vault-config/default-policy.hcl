# Vault Configuration for EmploymentVC
# Policies for secret management and access control

# Main policy - general access
path "secret/data/employmentvc/*" {
  capabilities = ["create", "read", "update", "list"]
}

path "secret/data/employmentvc/database/*" {
  capabilities = ["read"]
}

path "secret/data/employmentvc/api-keys/*" {
  capabilities = ["read"]
}

path "secret/data/employmentvc/credentials/*" {
  capabilities = ["create", "read", "update", "delete"]
}

# Auth service policy
path "secret/data/employmentvc/auth/*" {
  capabilities = ["read"]
}

path "pki/issue/auth-service" {
  capabilities = ["create", "update"]
}

# Wallet service policy
path "secret/data/employmentvc/wallet/*" {
  capabilities = ["read", "create", "update"]
}

path "pki/issue/wallet-api" {
  capabilities = ["create", "update"]
}

# Issuer service policy
path "secret/data/employmentvc/issuer/*" {
  capabilities = ["read", "create", "update"]
}

path "pki/issue/issuer-api" {
  capabilities = ["create", "update"]
}

# Verifier service policy
path "secret/data/employmentvc/verifier/*" {
  capabilities = ["read"]
}

path "pki/issue/verifier-api" {
  capabilities = ["create", "update"]
}

# Database credentials
path "database/creds/readonly" {
  capabilities = ["read"]
}

path "database/creds/readwrite" {
  capabilities = ["read"]
}

# SSH key access
path "ssh/sign/application" {
  capabilities = ["create", "update"]
}

# Kubernetes auth
path "auth/kubernetes/*" {
  capabilities = ["read"]
}
