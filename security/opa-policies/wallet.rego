# Wallet Policy for EmploymentVC
# Controls wallet operations and key management

package wallet

import future.keywords.if
import future.keywords.in

# Default deny
default allow = false

# Allow wallet operations if authorized
allow if {
    is_wallet_owner
    operation_allowed
    not wallet_locked
    within_security_thresholds
}

# Check if requester owns the wallet
is_wallet_owner if {
    input.user.did == input.wallet.owner_did
    input.user.authenticated == true
}

# Check if wallet is not locked
wallet_locked if {
    wallet_status := data.wallets[input.wallet.id]
    wallet_status.locked == true
}

# Operation-specific rules
operation_allowed if {
    input.operation == "create_credential"
    custodial_wallet_operation
}

operation_allowed if {
    input.operation == "sign_presentation"
    can_sign_presentation
}

operation_allowed if {
    input.operation == "export_key"
    can_export_key
}

operation_allowed if {
    input.operation == "store_credential"
    can_store_credential
}

# Custodial wallet specific rules
custodial_wallet_operation if {
    input.wallet.type == "custodial"
    input.user.consent_given == true
}

# Presentation signing rules
can_sign_presentation if {
    input.presentation.holder == input.wallet.owner_did
    input.presentation.verifier  # Verifier must be specified
    has_valid_credentials_for_presentation
}

has_valid_credentials_for_presentation if {
    # All credentials in presentation belong to wallet owner
    count([c | c := input.presentation.verifiableCredential[_]; c.credentialSubject.id != input.wallet.owner_did]) == 0
}

# Key export rules - highly restricted
can_export_key if {
    input.wallet.type == "custodial"
    input.user.mfa_verified == true
    input.export_reason in ["backup", "migration"]
    input.user.admin_approved == true
}

# Credential storage rules
can_store_credential if {
    input.credential.credentialSubject.id == input.wallet.owner_did
    credential_not_malicious
}

credential_not_malicious if {
    # Basic sanity checks
    input.credential.type
    input.credential.issuer
    input.credential.credentialSubject
    # Size limit check
    json.marshal(input.credential) <= 1048576  # 1MB max
}

# Security thresholds
within_security_thresholds if {
    not rate_limit_exceeded
    not suspicious_activity_detected
}

rate_limit_exceeded if {
    operations_count := count(data.wallet_operations[input.wallet.id])
    operations_count > 1000  # Max 1000 operations per hour
}

suspicious_activity_detected if {
    # Check for unusual patterns
    recent_failed_attempts := count([op | op := data.wallet_operations[input.wallet.id][_]; op.status == "failed"])
    recent_failed_attempts > 5
}

# Non-custodial wallet rules
noncustodial_wallet_operation if {
    input.wallet.type == "noncustodial"
    external_signature_valid
}

external_signature_valid if {
    # Signature validation delegated to wallet gateway
    input.external_signature_verified == true
}

# Wallet recovery rules
allow_recovery if {
    input.operation == "recover_wallet"
    recovery_method_valid
    recovery_not_recently_used
}

recovery_method_valid if {
    input.recovery.method in ["seed_phrase", "guardian", "social_recovery"]
    input.recovery.verified == true
}

recovery_not_recently_used if {
    last_recovery := data.wallet_recoveries[input.wallet.id]
    last_recovery == null
} else {
    last_recovery_time := time.parse_rfc3339_ns(last_recovery.timestamp)
    time.now_ns() - last_recovery_time > 2592000000000000  # 30 days
}

# Deny reasons
deny_reason[msg] {
    not is_wallet_owner
    msg := "User is not the wallet owner"
}

deny_reason[msg] {
    wallet_locked
    msg := "Wallet is locked due to security concerns"
}

deny_reason[msg] {
    rate_limit_exceeded
    msg := "Rate limit exceeded for wallet operations"
}

deny_reason[msg] {
    suspicious_activity_detected
    msg := "Suspicious activity detected - wallet temporarily restricted"
}

deny_reason[msg] {
    input.operation == "export_key"
    not input.user.mfa_verified
    msg := "MFA verification required for key export"
}
