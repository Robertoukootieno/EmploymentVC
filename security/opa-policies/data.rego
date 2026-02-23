# OPA Data Definitions for EmploymentVC
# Defines additional data structures and rules used by policies

package data

# Trusted issuers - employment verification authorities
trusted_issuers = [
  {
    "did": "did:example:issuer:001",
    "name": "HR Authority",
    "status": "active",
    "verified": true
  },
  {
    "did": "did:example:issuer:002",
    "name": "Government Employment Agency",
    "status": "active",
    "verified": true
  }
]

# Revoked issuers - no longer trusted
revoked_issuers = [
  "did:example:issuer:bad-actor"
]

# Revoked credentials - invalid and should not be accepted
revoked_credentials = [
  {
    "id": "urn:uuid:bad-credential-001",
    "reason": "fraud"
  }
]

# Credential schemas - valid schema types
credential_schemas = {
  "employment-verification": {
    "id": "urn:uuid:schema:001",
    "status": "active",
    "version": "1.0",
    "issuer_whitelist": [
      "did:example:issuer:001",
      "did:example:issuer:002"
    ]
  },
  "employment-history": {
    "id": "urn:uuid:schema:002",
    "status": "active",
    "version": "1.0",
    "issuer_whitelist": [
      "did:example:issuer:001"
    ]
  },
  "skills-certification": {
    "id": "urn:uuid:schema:003",
    "status": "active",
    "version": "1.0"
  }
}

# Rate limiting configuration
rate_limits = {
  "api.login": {
    "requests": 5,
    "window_seconds": 300,
    "block_duration_seconds": 900
  },
  "api.credential_issue": {
    "requests": 100,
    "window_seconds": 3600
  },
  "api.credential_verify": {
    "requests": 1000,
    "window_seconds": 3600
  }
}

# Operator roles and permissions
operator_roles = {
  "super_admin": {
    "permissions": ["*"]
  },
  "security_admin": {
    "permissions": [
      "manage_users",
      "manage_roles",
      "view_audit_logs",
      "issue_certificates",
      "manage_blocklist"
    ]
  },
  "employment_issuer": {
    "permissions": [
      "issue_credentials",
      "revoke_credentials:self",
      "view_own_credentials",
      "update_profile"
    ]
  },
  "employment_verifier": {
    "permissions": [
      "verify_credentials",
      "view_credential_schemas",
      "audit_verifications"
    ]
  },
  "credential_holder": {
    "permissions": [
      "store_credentials",
      "present_credentials",
      "manage_wallet",
      "view_own_data",
      "request_revocation"
    ]
  }
}

# Approved endpoint patterns
approved_endpoints = [
  "/api/v1/auth/login",
  "/api/v1/auth/logout",
  "/api/v1/auth/refresh",
  "/api/v1/credentials/verify",
  "/api/v1/credentials/issue",
  "/api/v1/credentials/revoke",
  "/api/v1/wallet/*",
  "/api/v1/profile/*",
  "/api/v1/health",
  "/api/v1/metrics"
]

# Sensitive operations requiring additional approval
sensitive_operations = [
  "revoke_all_credentials",
  "export_private_key",
  "disable_mfa",
  "delete_user_account",
  "modify_issuer_status"
]

# Compliance requirements by operation
compliance_requirements = {
  "issue_employment_credential": {
    "requires_mfa": true,
    "requires_audit_log": true,
    "min_permission_level": "employment_issuer",
    "data_retention_days": 2555
  },
  "verify_credential": {
    "requires_audit_log": true,
    "requires_challenge": true,
    "max_token_age_seconds": 600
  },
  "export_credential": {
    "requires_mfa": true,
    "requires_approval": true,
    "requires_reason": true,
    "data_anonymization": true
  }
}
