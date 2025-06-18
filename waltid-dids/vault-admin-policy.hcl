# vault-admin-policy.hcl

# Allow enabling/disabling secrets engines
path "sys/mounts/*" {
  capabilities = ["create", "read", "update", "delete", "list", "sudo"]
}

# Allow reading from sys/mounts root (for UI)
path "sys/mounts" {
  capabilities = ["read"]
}

# Allow access to all secrets paths (be careful in production)
path "secret/*" {
  capabilities = ["create", "read", "update", "delete", "list"]
}

# Allow access to all transit engine paths
path "transit/*" {
  capabilities = ["create", "read", "update", "delete", "list"]
}

