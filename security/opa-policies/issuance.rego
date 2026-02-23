# Issuance Policy for EmploymentVC
# Controls who can issue credentials and under what conditions

package issuance

import future.keywords.if
import future.keywords.in

# Default deny
default allow = false

# Allow issuance if all conditions are met
allow if {
    is_authorized_issuer
    has_valid_schema
    has_required_claims
    not is_revoked_issuer
}

# Check if the requester is an authorized issuer
is_authorized_issuer if {
    input.issuer.did
    input.issuer.role == "employer"
    input.issuer.verified == true
}

# Validate that the credential schema exists and is valid
has_valid_schema if {
    input.credential.type
    input.credential.schema_id
    # Schema must be registered in schema registry
    schema := data.schemas[input.credential.schema_id]
    schema.status == "active"
}

# Ensure all required claims are present
has_required_claims if {
    input.credential.credentialSubject.id  # Holder DID
    input.credential.credentialSubject.employmentStatus
    input.credential.credentialSubject.employerId
}

# Check if issuer is not revoked
is_revoked_issuer if {
    revoked_issuer := data.revoked_issuers[_]
    revoked_issuer == input.issuer.did
}

# Additional validation for employment credentials
employment_credential_valid if {
    input.credential.type[_] == "EmploymentCredential"
    input.credential.credentialSubject.startDate
    # Start date should not be in the future
    time.parse_rfc3339_ns(input.credential.credentialSubject.startDate) <= time.now_ns()
}

# Rate limiting check
within_rate_limit if {
    count(data.recent_issuances[input.issuer.did]) < 100  # Max 100 per hour
}

# Compliance check - ensure credential meets regulatory requirements
compliance_check if {
    input.credential.credentialSubject.jurisdiction
    # Add jurisdiction-specific rules here
    true
}

# Detailed allow with reason
allow_with_reason[reason] {
    allow
    reason := "Issuance authorized: All checks passed"
}

# Deny with specific reason
deny_reason[msg] {
    not is_authorized_issuer
    msg := "Issuer is not authorized or verified"
}

deny_reason[msg] {
    not has_valid_schema
    msg := "Invalid or inactive credential schema"
}

deny_reason[msg] {
    not has_required_claims
    msg := "Missing required credential claims"
}

deny_reason[msg] {
    is_revoked_issuer
    msg := "Issuer has been revoked"
}
