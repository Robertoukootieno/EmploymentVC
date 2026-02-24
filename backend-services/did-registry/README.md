# DID Registry Service

## Overview
The DID Registry Service manages Decentralized Identifiers (DIDs) for the EmploymentVC platform, providing DID resolution, registration, and caching capabilities.

## Architecture Role
- **Tier**: Core Service
- **Port**: 8082
- **Type**: Spring Boot Application

## Responsibilities
- DID registration and management
- DID document resolution
- DID method driver coordination
- Caching layer for DID documents
- Integration with Hyperledger Besu (did:ethr)
- Universal Resolver integration

## Supported DID Methods

### 1. did:web
Web-based DIDs using HTTPS.
```
did:web:example.com
did:web:example.com:user:alice
```

### 2. did:key
Self-contained DIDs for quick onboarding.
```
did:key:z6MkhaXgBZDvotDkL5257faiztiGiC2QtKLGpbnnEGta2doK
```

### 3. did:ethr
Ethereum-based DIDs via Besu ledger.
```
did:ethr:0x1337:0xabc123...
```

## Key Features

### DID Resolution
- Multi-method support
- Caching with Redis (1 hour TTL)
- Fallback to Universal Resolver
- Metadata tracking

### DID Registration
- Create new DIDs
- Update DID Documents
- Deactivate DIDs
- Key rotation

### Integration
- **Hyperledger Besu**: Ethereum-based DID registry
- **Redis**: High-performance caching
- **Universal Resolver**: Fallback resolution

## Configuration

### Environment Variables
| Variable | Description | Required |
|----------|-------------|----------|
| `DATABASE_URL` | PostgreSQL connection | Yes |
| `REDIS_HOST` | Redis hostname | Yes |
| `REDIS_PORT` | Redis port | No (6379) |
| `BESU_RPC_URL` | Besu RPC endpoint | Yes (for did:ethr) |
| `DID_REGISTRY_CONTRACT` | Smart contract address | Yes (for did:ethr) |
| `UNIVERSAL_RESOLVER_URL` | External resolver | No |

## API Endpoints

### Resolution
```
GET /dids/{did}
  - Resolve DID to DID Document

GET /dids/{did}/metadata
  - Get DID metadata
  
POST /dids/resolve/batch
  - Batch resolution
```

### Registration
```
POST /dids
  - Register new DID

PUT /dids/{did}
  - Update DID Document

DELETE /dids/{did}
  - Deactivate DID

POST /dids/{did}/keys
  - Add verification method
```

### Cache Management
```
DELETE /dids/{did}/cache
  - Invalidate cached DID

POST /dids/cache/warm
  - Pre-cache frequently used DIDs
```

## DID Document Structure

### Example: did:web
```json
{
  "@context": ["https://www.w3.org/ns/did/v1"],
  "id": "did:web:employmentvc.com:users:alice",
  "verificationMethod": [{
    "id": "did:web:employmentvc.com:users:alice#key-1",
    "type": "Ed25519VerificationKey2020",
    "controller": "did:web:employmentvc.com:users:alice",
    "publicKeyMultibase": "z6MkhaXg..."
  }],
  "authentication": ["#key-1"],
  "assertionMethod": ["#key-1"]
}
```

### Example: did:ethr
```json
{
  "@context": ["https://www.w3.org/ns/did/v1"],
  "id": "did:ethr:0x1337:0xabc123...",
  "verificationMethod": [{
    "id": "did:ethr:0x1337:0xabc123...#controller",
    "type": "EcdsaSecp256k1RecoveryMethod2020",
    "controller": "did:ethr:0x1337:0xabc123...",
    "blockchainAccountId": "eip155:1337:0xabc123..."
  }]
}
```

## Database Schema

### Tables
- `did_registry`: Registered DIDs and documents
- `did_metadata`: DID metadata (created, updated, status)
- `verification_methods`: Extracted verification keys
- `resolution_cache`: Cached resolutions

## Caching Strategy

### Redis Caching
- **TTL**: 1 hour for DID Documents
- **Invalidation**: On DID updates
- **Warm-up**: Pre-cache trusted issuers/verifiers

### Performance
- Cache hit rate target: >95%
- Resolution latency: <50ms (cached)
- Resolution latency: <500ms (uncached)

## Ledger Integration

### Hyperledger Besu
- **Chain ID**: Configurable (default 1337)
- **Contract**: ERC-1056 DID Registry
- **Operations**:
  - setAttribute
  - revokeAttribute
  - changeOwner

### Smart Contract ABI
Uses standard ERC-1056 registry interface.

## Build & Run

### Local Development
```bash
./gradlew :backend-services:did-registry:bootRun
```

### Build Docker Image
```bash
docker build -t employmentvc/did-registry:latest \
  -f backend-services/did-registry/Dockerfile .
```

### Run with Dependencies
```bash
COMPOSE_PROJECT_NAME=employmentvc docker compose up -d postgres redis besu
./gradlew :backend-services:did-registry:bootRun
```

## Monitoring

### Metrics
- `did_resolution_total`: Total resolutions
- `did_cache_hit_ratio`: Cache effectiveness
- `did_resolution_duration_seconds`: Resolution latency
- `did_registrations_total`: New DIDs registered
- `did_besu_calls_total`: Besu RPC calls

### Health Checks
- Database connectivity
- Redis connectivity
- Besu RPC connectivity
- Universal Resolver reachability

## Security

### Access Control
- Public: DID resolution
- Authenticated: DID registration
- Owner-only: DID updates/deactivation

### Key Management
- Keys never stored by registry
- Only public key material in DID Documents
- Key rotation supported

## Testing

### Unit Tests
```bash
./gradlew :backend-services:did-registry:test
```

### Test Resolution
```bash
# Resolve a DID
curl http://localhost:8082/dids/did:web:example.com

# Register a new did:key
curl -X POST http://localhost:8082/dids \
  -H "Content-Type: application/json" \
  -d '{"method": "key", "keyType": "Ed25519"}'
```

## Standards Compliance
- **W3C DID Core 1.0**
- **DID Resolution Specification**
- **ERC-1056**: Ethereum DID Registry
- **did:web Method Specification**
- **did:key Method Specification**

## Common Operations

### Creating a new DID
```bash
curl -X POST http://localhost:8082/dids \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"method": "ethr", "network": "besu"}'
```

### Updating DID Document
```bash
curl -X PUT http://localhost:8082/dids/did:ethr:0x123... \
  -H "Authorization: Bearer $TOKEN" \
  -d @updated-did-document.json
```

## Troubleshooting

### Resolution Fails
- Check DID method is supported
- Verify network connectivity
- Check cache status
- Review Besu logs (for did:ethr)

### Registration Fails
- Verify authentication
- Check Besu gas limits (for did:ethr)
- Ensure unique DID

## References
- [W3C DID Core](https://www.w3.org/TR/did-core/)
- [DID Resolution](https://w3c-ccg.github.io/did-resolution/)
- [ERC-1056](https://github.com/ethereum/EIPs/issues/1056)
- [Universal Resolver](https://dev.uniresolver.io/)
