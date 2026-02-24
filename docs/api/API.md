# Provenly Employment VC Platform - API Documentation

## Overview

The Provenly Employment VC Platform provides a comprehensive REST API for managing Verifiable Credentials (VCs) in employment contexts. The API follows RESTful principles and supports JSON-LD credentials with selective disclosure capabilities.

## Base URL

- **Development**: `http://localhost:3000/api/v1`
- **Production**: `https://api.provenly.io/api/v1`

## Authentication

All API endpoints (except authentication endpoints) require a valid JWT token in the Authorization header:

```
Authorization: Bearer <jwt_token>
```

### Getting a Token

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password"
}
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 3600,
  "token_type": "Bearer"
}
```

## Core API Endpoints

### 1. Authentication Service (`/api/v1/auth`)

#### Login
```http
POST /api/v1/auth/login
```

#### Refresh Token
```http
POST /api/v1/auth/refresh
```

#### Logout
```http
POST /api/v1/auth/logout
```

### 2. DID Registry (`/api/v1/did`)

#### Create DID
```http
POST /api/v1/did/create
Content-Type: application/json

{
  "method": "ebsi",
  "keyType": "Ed25519",
  "options": {
    "anchor": true
  }
}
```

**Response:**
```json
{
  "did": "did:ebsi:zxHaP8AmTWURkGkuQzKyD5",
  "didDocument": {
    "@context": ["https://www.w3.org/ns/did/v1"],
    "id": "did:ebsi:zxHaP8AmTWURkGkuQzKyD5",
    "verificationMethod": [...],
    "authentication": [...],
    "assertionMethod": [...]
  },
  "keys": {
    "privateKey": "...",
    "publicKey": "..."
  }
}
```

#### Resolve DID
```http
GET /api/v1/did/{did}
```

#### Update DID Document
```http
PUT /api/v1/did/{did}
Content-Type: application/json

{
  "didDocument": {
    // Updated DID document
  }
}
```

### 3. Schema Registry (`/api/v1/schemas`)

#### Register Schema
```http
POST /api/v1/schemas
Content-Type: application/json

{
  "name": "EmploymentCredential",
  "version": "1.0.0",
  "schema": {
    "@context": [
      "https://www.w3.org/2018/credentials/v1",
      "https://provenly.io/contexts/employment/v1"
    ],
    "type": ["VerifiableCredential", "EmploymentCredential"],
    "credentialSubject": {
      "type": "object",
      "properties": {
        "employeeId": {"type": "string"},
        "position": {"type": "string"},
        "department": {"type": "string"},
        "startDate": {"type": "string", "format": "date"},
        "salary": {"type": "number"}
      },
      "required": ["employeeId", "position", "startDate"]
    }
  },
  "selectiveDisclosure": {
    "enabled": true,
    "mandatoryFields": ["employeeId", "position"],
    "optionalFields": ["department", "startDate", "salary"]
  }
}
```

#### Get Schema
```http
GET /api/v1/schemas/{schemaId}
```

#### List Schemas
```http
GET /api/v1/schemas?page=1&limit=10&type=EmploymentCredential
```

### 4. Issuer Service (`/api/v1/issue`)

#### Issue Credential
```http
POST /api/v1/issue/credential
Content-Type: application/json

{
  "schemaId": "employment-credential-v1",
  "issuerDid": "did:ebsi:zxHaP8AmTWURkGkuQzKyD5",
  "subjectDid": "did:ebsi:zxHaP8AmTWURkGkuQzKyD6",
  "credentialData": {
    "employeeId": "EMP001",
    "position": "Software Engineer",
    "department": "Engineering",
    "startDate": "2024-01-15",
    "salary": 75000
  },
  "options": {
    "selectiveDisclosure": true,
    "expirationDate": "2025-01-15T00:00:00Z"
  }
}
```

**Response:**
```json
{
  "credential": {
    "@context": [
      "https://www.w3.org/2018/credentials/v1",
      "https://provenly.io/contexts/employment/v1"
    ],
    "type": ["VerifiableCredential", "EmploymentCredential"],
    "issuer": "did:ebsi:zxHaP8AmTWURkGkuQzKyD5",
    "issuanceDate": "2024-01-15T10:00:00Z",
    "expirationDate": "2025-01-15T00:00:00Z",
    "credentialSubject": {
      "id": "did:ebsi:zxHaP8AmTWURkGkuQzKyD6",
      "employeeId": "EMP001",
      "position": "Software Engineer",
      "department": "Engineering",
      "startDate": "2024-01-15",
      "salary": 75000
    },
    "proof": {
      "type": "BbsBlsSignature2020",
      "created": "2024-01-15T10:00:00Z",
      "proofPurpose": "assertionMethod",
      "verificationMethod": "did:ebsi:zxHaP8AmTWURkGkuQzKyD5#key-1",
      "proofValue": "..."
    }
  },
  "selectiveDisclosureData": {
    "disclosureMap": {...},
    "blinding": "..."
  }
}
```

#### Revoke Credential
```http
POST /api/v1/issue/revoke
Content-Type: application/json

{
  "credentialId": "urn:uuid:12345678-1234-5678-9012-123456789012",
  "reason": "Employment terminated"
}
```

### 5. Verifier Service (`/api/v1/verify`)

#### Verify Credential
```http
POST /api/v1/verify/credential
Content-Type: application/json

{
  "credential": {
    // Verifiable Credential object
  },
  "options": {
    "checkRevocation": true,
    "checkExpiration": true
  }
}
```

**Response:**
```json
{
  "verified": true,
  "results": [
    {
      "proof": {
        "verified": true,
        "proofPurpose": "assertionMethod"
      },
      "issuer": {
        "verified": true,
        "did": "did:ebsi:zxHaP8AmTWURkGkuQzKyD5"
      },
      "expiration": {
        "verified": true,
        "expirationDate": "2025-01-15T00:00:00Z"
      },
      "revocation": {
        "verified": true,
        "revoked": false
      }
    }
  ]
}
```

#### Verify Presentation
```http
POST /api/v1/verify/presentation
Content-Type: application/json

{
  "presentation": {
    "@context": ["https://www.w3.org/2018/credentials/v1"],
    "type": ["VerifiablePresentation"],
    "holder": "did:ebsi:zxHaP8AmTWURkGkuQzKyD6",
    "verifiableCredential": [...],
    "proof": {...}
  },
  "challenge": "challenge-string",
  "domain": "verifier.example.com"
}
```

### 6. Holder Wallet (`/api/v1/wallet`)

#### Store Credential
```http
POST /api/v1/wallet/credentials
Content-Type: application/json

{
  "credential": {
    // Verifiable Credential object
  },
  "metadata": {
    "tags": ["employment", "current"],
    "notes": "Current employment credential"
  }
}
```

#### List Credentials
```http
GET /api/v1/wallet/credentials?type=EmploymentCredential&status=active
```

#### Create Presentation
```http
POST /api/v1/wallet/presentations
Content-Type: application/json

{
  "credentialIds": ["cred-1", "cred-2"],
  "selectiveDisclosure": {
    "cred-1": ["employeeId", "position"],
    "cred-2": ["degree", "institution"]
  },
  "challenge": "challenge-string",
  "domain": "verifier.example.com"
}
```

## Error Responses

All endpoints return consistent error responses:

```json
{
  "error": "BadRequest",
  "message": "Invalid credential format",
  "details": {
    "field": "credentialSubject",
    "issue": "Missing required field 'employeeId'"
  },
  "timestamp": "2024-01-15T10:00:00Z",
  "path": "/api/v1/issue/credential"
}
```

### HTTP Status Codes

- `200` - Success
- `201` - Created
- `400` - Bad Request
- `401` - Unauthorized
- `403` - Forbidden
- `404` - Not Found
- `409` - Conflict
- `422` - Unprocessable Entity
- `500` - Internal Server Error

## Rate Limiting

API endpoints are rate-limited:
- **Default**: 100 requests per 15 minutes per IP
- **Authentication**: 10 requests per 15 minutes per IP
- **Credential Operations**: 50 requests per 15 minutes per authenticated user

Rate limit headers are included in responses:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1642248000
```

## Pagination

List endpoints support pagination:

```http
GET /api/v1/schemas?page=2&limit=20&sort=createdAt&order=desc
```

**Response:**
```json
{
  "data": [...],
  "pagination": {
    "page": 2,
    "limit": 20,
    "total": 150,
    "pages": 8,
    "hasNext": true,
    "hasPrev": true
  }
}
```

## Webhooks

The platform supports webhooks for real-time notifications:

### Credential Issued
```json
{
  "event": "credential.issued",
  "timestamp": "2024-01-15T10:00:00Z",
  "data": {
    "credentialId": "urn:uuid:12345678-1234-5678-9012-123456789012",
    "issuer": "did:ebsi:zxHaP8AmTWURkGkuQzKyD5",
    "subject": "did:ebsi:zxHaP8AmTWURkGkuQzKyD6",
    "type": "EmploymentCredential"
  }
}
```

### Credential Verified
```json
{
  "event": "credential.verified",
  "timestamp": "2024-01-15T10:00:00Z",
  "data": {
    "credentialId": "urn:uuid:12345678-1234-5678-9012-123456789012",
    "verifier": "did:ebsi:zxHaP8AmTWURkGkuQzKyD7",
    "result": "valid"
  }
}
```

## SDKs and Libraries

Official SDKs are available for:
- JavaScript/TypeScript
- Python
- Java
- C#

Example usage (JavaScript):
```javascript
import { ProvenlyClient } from '@provenly/sdk';

const client = new ProvenlyClient({
  baseUrl: 'https://api.provenly.io',
  apiKey: 'your-api-key'
});

const credential = await client.issuer.issueCredential({
  schemaId: 'employment-credential-v1',
  issuerDid: 'did:ebsi:zxHaP8AmTWURkGkuQzKyD5',
  subjectDid: 'did:ebsi:zxHaP8AmTWURkGkuQzKyD6',
  credentialData: {
    employeeId: 'EMP001',
    position: 'Software Engineer'
  }
});
```
