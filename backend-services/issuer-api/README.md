# Issuer Service

## Overview
The Issuer Service is responsible for issuing W3C Verifiable Credentials for employment verification. It implements the W3C VC Data Model and supports multiple signature formats.

## Architecture Role
- **Tier**: Core Service
- **Port**: 8083
- **Type**: Spring Boot Application
- **Actor**: Employer

## Responsibilities
- Issue verifiable credentials
- Sign credentials with cryptographic proofs
- Validate credential schemas
- Manage credential lifecycle
- Revocation management
- Policy-based issuance control

## Credential Types Supported

### 1. Employment Credential
```json
{
  "@context": [
    "https://www.w3.org/2018/credentials/v1",
    "https://employmentvc.com/contexts/employment/v1"
  ],
  "type": ["VerifiableCredential", "EmploymentCredential"],
  "credentialSubject": {
    "id": "did:key:z6Mk...",
    "employmentStatus": "active",
    "employerId": "did:web:acme.com",
    "position": "Senior Software Engineer",
    "department": "Engineering",
    "startDate": "2024-01-15",
    "salaryBand": "Level 5"
  }
}
```

### 2. Termination Credential
Documents employment termination for compliance purposes.

### 3. Reference Credential
Employee references from former employers.

## Key Features

### Issuance Flow
1. **Request Validation**: Verify requester authorization
2. **Schema Validation**: Validate against registered schema
3. **Policy Check**: OPA policy evaluation
4. **Signing**: Cryptographic proof generation
5. **Storage**: Credential stored in registry
6. **Delivery**: Return signed credential

### Signature Formats
- **JWS (JSON Web Signature)**: Default, RFC 7515
- **Ed25519Signature2020**: W3C standard
- **JsonWebSignature2020**: JOSE-compatible

### Security
- OPA policy enforcement
- Rate limiting per issuer
- Audit logging of all issuances
- Key management via Vault
- Revocation registry integration

### Revocation
- **StatusList2021**: W3C standard bitstring revocation
- **Revocation API**: RESTful revocation interface
- **Notification**: Webhook notifications on revocation

## Configuration

### Environment Variables
| Variable | Description | Required |
|----------|-------------|----------|
| `DATABASE_URL` | PostgreSQL connection | Yes |
| `ISSUER_DID` | Issuer's DID identifier | Yes |
| `ISSUER_KEY_ID` | Key identifier for signing | Yes |
| `VAULT_URL` | Vault server URL | Yes |
| `VAULT_TOKEN` | Vault access token | Yes |
| `OPA_URL` | OPA server URL | Yes |
| `SCHEMA_REGISTRY_URL` | Schema registry URL | Yes |

## API Endpoints

### Issuance
```
POST /issuer/credentials
  - Issue a new credential

GET /issuer/credentials/{id}
  - Retrieve credential details

POST /issuer/credentials/batch
  - Batch credential issuance
```

### Revocation
```
POST /issuer/revoke
  - Revoke a credential

GET /issuer/status/{credentialId}
  - Check revocation status

GET /issuer/revocation-list
  - Get StatusList2021 bitstring
```

### Management
```
GET /issuer/schemas
  - List supported schemas

POST /issuer/verify-schema
  - Validate credential against schema
```

## Database Schema

### Tables
- `issued_credentials`: Issued credential metadata
- `revocation_registry`: Revoked credential IDs
- `issuance_audit`: Audit trail
- `status_lists`: StatusList2021 lists

## OPA Policies

### Issuance Policy (`issuance.rego`)
- Authorized issuer check
- Valid schema verification
- Required claims validation
- Revoked issuer check
- Rate limiting

## Cryptographic Operations

### Key Management
- Keys stored in Vault
- Support for key rotation
- Multiple key formats:
  - Ed25519 (preferred)
  - ECDSA (P-256, P-384)
  - RSA (2048, 4096)

### Signing Process
1. Retrieve private key from Vault
2. Create canonical credential JSON
3. Generate proof object
4. Sign with specified algorithm
5. Attach proof to credential

## Build & Run

### Local Development
```bash
./gradlew :issuer-api:bootRun
```

### Build Docker Image
```bash
docker build -t employmentvc/issuer-service:latest \
  -f backend-services/issuer-api/Dockerfile .
```

## Integration Points

### Schema Registry
- Validates credentials against registered schemas
- Retrieves schema definitions
- Version management

### Credential Registry
- Stores issued credential metadata
- Manages revocation status
- Provides status list endpoint

### Vault
- Retrieves signing keys
- Stores sensitive configuration
- Audit logging

### OPA
- Policy evaluation for issuance
- Authorization decisions
- Compliance checks

## Monitoring

### Metrics
- `issuer_credentials_issued_total`: Total credentials issued
- `issuer_credentials_revoked_total`: Total revocations
- `issuer_issuance_duration_seconds`: Issuance latency
- `issuer_policy_denials_total`: Policy rejections
- `issuer_schema_validation_failures`: Schema validation errors

### Health Checks
- Database connectivity
- Vault connectivity
- OPA connectivity
- Schema Registry connectivity

## Security Best Practices

### Production Deployment
- [ ] Use Vault for all key material
- [ ] Enable TLS for all connections
- [ ] Implement key rotation policy
- [ ] Configure OPA policies
- [ ] Enable audit logging
- [ ] Set up alerting for anomalies
- [ ] Regular security scans
- [ ] Limit issuance rate

## Standards Compliance
- **W3C Verifiable Credentials Data Model 1.1**
- **W3C DID Core**
- **W3C StatusList2021**
- **JSON-LD 1.1**
- **RFC 7515** (JWS)

## Testing

### Unit Tests
```bash
./gradlew :backend-services:issuer-api:test
```

### Integration Tests
Requires PostgreSQL, Vault, and OPA.

### Test Issuance
```bash
curl -X POST http://localhost:8083/issuer/credentials \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d @test-credential-request.json
```

## Troubleshooting

### Common Issues

**Issue**: Schema validation fails
```bash
# Verify schema exists in registry
curl http://schema-registry:8086/schemas/{schemaId}
```

**Issue**: Signing fails
```bash
# Check Vault connectivity and key access
curl http://vault:8200/v1/secret/issuer/keys
```

## References
- [W3C Verifiable Credentials](https://www.w3.org/TR/vc-data-model/)
- [StatusList2021](https://w3c-ccg.github.io/vc-status-list-2021/)
- [JSON-LD](https://www.w3.org/TR/json-ld11/)
