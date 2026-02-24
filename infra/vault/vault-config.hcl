# HashiCorp Vault Configuration for EmploymentVC
listener "tcp" {
  address     = "0.0.0.0:8200"
  tls_disable = 1 # For dev only! Set to 0 and configure certs for production
}

storage "file" {
  path = "/vault/data"
}

ui = true

# Enable audit logging
log_level = "info"
