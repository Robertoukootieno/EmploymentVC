# Verifier Service

## Overview
The Verifier Service validates W3C Verifiable Presentations and Credentials, performing cryptographic verification, revocation checks, and policy enforcement.

## Architecture Role
- **Tier**: Core Service
- **Port**: 8084
- **Type**: Spring Boot Application
- **Actor**: Employer (as Verifier)

## Responsibilities
- Verify verifiable presentations
- Validate credential signatures
- Check revocation status
- Enforce trust policies
- Challenge-response management
- Privacy-preserving verification
- Selective disclosure validation

## Verification Flow

### 1. Challenge Generation
```
GET /verifier/challenge
→ Returns unique challenge for presentation
```

### 2. Presentation Submission
```
POST /verifier/verify
← Verifiable presentation with challenge proof
→ Verification result
```

### 3. Verification Steps
1. **Structural Validation**: JSON-LD + JSON Schema
2. **Challenge Verification**: Proof challenge matches
3. **Signature Verification**: Cryptographic proofs valid
4. **Issuer Trust**: Issuer in trusted list
5. **Revocation Check**: Credential not revoked
6. **Policy Check**: OPA policy evaluation
7. **Expiration Check**: Credential not expired
8. **Holder Binding**: Holder controls presentation

## Key Features

### Trust Framework
- **Trusted Issuer Registry**: Whitelist of acceptable issuers
- **Trust Levels**: Different verification rigor levels
- **Dynamic Trust**: Trust can be updated in real-time

### Revocation Checking
- **StatusList2021**: Bitstring-based revocation
- **Caching**: Revocation lists cached (TTL: 5 min)
- **Fallback**: Graceful degradation if unreachable

### Privacy Features
- **Selective Disclosure**: Verify only requested attributes
- **Zero-Knowledge Proofs**: Support for ZKP credentials
- **Minimal Disclosure**: Enforce privacy policies

### Verification Types

#### 1. Full Verification
Complete cryptographic and policy checks.

#### 2. Quick Verification
Skip expensive revocation checks (use cached data).

#### 3. Threshold Verification
Require only subset of checks to pass.

## Configuration

### Environment Variables
| Variable | Description | Required |
|----------|-------------|----------|
| `DATABASE_URL` | PostgreSQL connection | Yes |
| `VERIFIER_DID` | Verifier's DID | Yes |
| `DID_RESOLVER_URL` | DID resolver service | Yes |
| `OPA_URL` | OPA policy server | Yes |
| `TRUSTED_ISSUERS` | Comma-separated issuer DIDs | No |
| `REVOCATION_CHECK_ENABLED` | Enable revocation checks | No (true) |

## API Endpoints

### Verification
```
GET /verifier/challenge
  - Generate presentation challenge

POST /verifier/verify
  - Verify presentation

POST /verifier/verify/batch
  - Batch verification

GET /verifier/result/{verificationId}
  - Get verification result
```

### Trust Management
```
GET /verifier/trusted-issuers
  - List trusted issuers

POST /verifier/trusted-issuers
  - Add trusted issuer

DELETE /verifier/trusted-issuers/{did}
  - Remove trusted issuer
```

### Policy
```
POST /verifier/policy/evaluate
  - Evaluate verification policy
```

## Database Schema

### Tables
- `verification_sessions`: Active verification sessions
- `challenges`: Generated challenges (TTL: 10 min)
- `trusted_issuers`: Trusted issuer registry
- `verification_results`: Historical results
- `revocation_cache`: Cached revocation data

## Verification Policies

### OPA Integration (`verification.rego`)
- Authorized verifier check
- Credential expiration
- Revocation status
- Issuer trust validation
- Signature validity
- Presentation challenge validation
- Selective disclosure compliance
- Privacy compliance

## Cryptographic Verification

### Signature Verification
1. Fetch issuer's DID Document
2. Extract verification method
3. Verify signature with public key
4. Validate proof purpose

### Supported Proof Types
- `Ed25519Signature2020`
- `JsonWebSignature2020`
- `EcdsaSecp256k1Signature2019`

## Build & Run

### Local Development
```bash
./gradlew :backend-services:verifier-api:bootRun
```

### Build Docker Image
```bash
docker build -t employmentvc/verifier-service:latest \
  -f backend-services/verifier-api/Dockerfile .
```

## Integration Points

### DID Resolver
- Resolves issuer DIDs
- Retrieves verification methods
- Caches DID Documents

### Revocation Registry
- Checks credential status
- Retrieves StatusList2021
- Updates cache

### OPA
- Policy evaluation
- Access control
- Compliance validation

## Security Considerations

### Challenge Security
- Random 256-bit challenges
- 10-minute expiration
- Single-use only
- Replay attack prevention

### Privacy Protection
- Only request necessary attributes
- No unnecessary data retention
- Audit log selective disclosure requests
- GDPR compliant

## Monitoring

### Metrics
- `verifier_presentations_verified_total`: Total verifications
- `verifier_verification_failures_total`: Failed verifications
- `verifier_verification_duration_seconds`: Verification latency
- `verifier_revocation_checks_total`: Revocation lookups
- `verifier_challenges_issued_total`: Challenges generated

### Alerts
- High verification failure rate
- Revocation service unavailable
- Policy violations spike
- Unusual verification patterns

## Testing

### Unit Tests
```bash
./gradlew :backend-services:verifier-api:test
```

### Test Verification
```bash
# Get challenge
CHALLENGE=$(curl http://localhost:8084/verifier/challenge | jq -r '.challenge')

# Submit presentation
curl -X POST http://localhost:8084/verifier/verify \
  -H "Content-Type: application/json" \
  -d @test-presentation.json
```

## Standards Compliance
- **W3C Verifiable Credentials Data Model 1.1**
- **W3C Verifiable Presentations**
- **W3C DID Core**
- **W3C StatusList2021**
- **JSON-LD 1.1**
- **RFC 7515** (JWS)

## Common Scenarios

### Employment Verification
```json
{
  "verificationRequest": {
    "type": "EmploymentVerification",
    "requestedAttributes": [
      "employmentStatus",
      "position",
      "startDate"
    ],
    "purpose": "Background check"
  }
}
```

### Income Verification
```json
{
  "verificationRequest": {
    "type": "IncomeVerification",
    "requestedAttributes": [
      "salaryBand",
      "employmentStatus"
    ],
    "purpose": "Loan application"
  }
}
```

## Troubleshooting

### Verification Fails

**Signature Invalid**
- Check issuer DID Document
- Verify key ID matches
- Ensure correct signature algorithm

**Revocation Check Fails**
- Check revocation service availability
- Verify StatusList2021 URL
- Check cache configuration

**Policy Denial**
- Review OPA policy logs
- Check policy decision output
- Verify input data format

## References
- [W3C Verifiable Presentations](https://www.w3.org/TR/vc-data-model/#presentations)
- [Presentation Request Specification](https://w3c-ccg.github.io/vp-request-spec/)
- [DIDComm Messaging](https://identity.foundation/didcomm-messaging/spec/)
