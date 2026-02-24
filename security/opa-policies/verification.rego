# Verification Policy for EmploymentVC
# Controls verification of presented credentials

package verification

import future.keywords.if
import future.keywords.in

# Default deny
default allow = false

# Allow verification if all conditions are met
allow if {
    is_authorized_verifier
    credential_not_expired
    credential_not_revoked
    issuer_is_trusted
    signature_valid
    presentation_challenge_valid
}

# Check if the requester is an authorized verifier
is_authorized_verifier if {
    input.verifier.did
    input.verifier.role in ["employer", "verifier"]
    input.verifier.verified == true
}

# Check if credential is not expired
credential_not_expired if {
    expiration := input.credential.expirationDate
    time.parse_rfc3339_ns(expiration) > time.now_ns()
}

# Check if credential is not revoked
credential_not_revoked if {
    # Check revocation registry
    not is_in_revocation_list
}

is_in_revocation_list if {
    revoked := data.revoked_credentials[_]
    revoked.id == input.credential.id
}

# Check if issuer is in the trusted list
issuer_is_trusted if {
    issuer_did := input.credential.issuer
    trusted := data.trusted_issuers[_]
    trusted.did == issuer_did
    trusted.status == "active"
}

# Verify cryptographic signature (simplified - actual verification done by service)
signature_valid if {
    input.credential.proof
    input.credential.proof.type
    input.credential.proof.proofPurpose == "assertionMethod"
    # Actual signature verification delegated to crypto service
    input.signature_verified == true
}

# Validate presentation challenge
presentation_challenge_valid if {
    input.presentation.proof.challenge
    input.expected_challenge
    input.presentation.proof.challenge == input.expected_challenge
    
    # Challenge should not be expired
    challenge_time := time.parse_rfc3339_ns(input.presentation.proof.created)
    time.now_ns() - challenge_time < 600000000000  # 10 minutes in nanoseconds
}

# Selective disclosure check - ensure only requested attributes are shared
selective_disclosure_valid if {
    requested_attrs := {attr | attr := input.requested_attributes[_]}
    disclosed_attrs := {attr | attr := object.keys(input.credential.credentialSubject)[_]}
    
    # All requested attributes must be present
    requested_attrs - disclosed_attrs == set()
}

# Privacy check - ensure no unnecessary data is disclosed
privacy_compliant if {
    not contains_pii_beyond_necessity
}

contains_pii_beyond_necessity if {
    # Check if sensitive fields are included when not requested
    sensitive_fields := ["ssn", "dateOfBirth", "address"]
    disclosed := object.keys(input.credential.credentialSubject)
    requested := input.requested_attributes
    
    some field in sensitive_fields
    field in disclosed
    not field in requested
}

# Context-specific verification rules
employment_verification_valid if {
    input.credential.type[_] == "EmploymentCredential"
    input.credential.credentialSubject.employmentStatus in ["active", "terminated"]
    
    # If checking current employment, status must be active
    input.verification_purpose == "current_employment"
    input.credential.credentialSubject.employmentStatus == "active"
}

# Detailed deny reasons
deny_reason[msg] {
    not is_authorized_verifier
    msg := "Verifier is not authorized"
}

deny_reason[msg] {
    not credential_not_expired
    msg := "Credential has expired"
}

deny_reason[msg] {
    is_in_revocation_list
    msg := "Credential has been revoked"
}

deny_reason[msg] {
    not issuer_is_trusted
    msg := "Issuer is not in the trusted list"
}

deny_reason[msg] {
    not signature_valid
    msg := "Credential signature is invalid"
}

deny_reason[msg] {
    not presentation_challenge_valid
    msg := "Presentation challenge is invalid or expired"
}

deny_reason[msg] {
    contains_pii_beyond_necessity
    msg := "Presentation contains unnecessary PII - privacy violation"
}
