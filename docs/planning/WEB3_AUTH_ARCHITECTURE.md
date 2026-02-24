# Web3 Authentication & Keycloak Integration Architecture

## Overview

This document outlines how to integrate Web3 authentication (wallet-based) and Keycloak into the existing microservices architecture without creating additional services.

## Architecture Approach

### 🏗️ **Enhanced Authentication Service**
Instead of creating separate microservices, we enhance the existing **Authentication Service** to support multiple authentication methods:

1. **Traditional Auth** (email/password via Keycloak)
2. **Web3 Auth** (wallet signatures via SIWE - Sign-In with Ethereum)
3. **DID Auth** (DID-based authentication)

### 🔄 **Authentication Flow Options**

#### Option 1: Traditional OAuth2/OIDC Flow
```
User → API Gateway → Keycloak → JWT Token → Services
```

#### Option 2: Web3 Wallet Authentication
```
User Wallet → Sign Challenge → Auth Service → Verify Signature → JWT Token → Services
```

#### Option 3: DID-based Authentication
```
User DID → Sign Challenge → Auth Service → Verify DID → JWT Token → Services
```

## Implementation Strategy

### 1. **Enhanced Authentication Service**

The auth service will handle:
- **Keycloak Integration**: Traditional user management
- **Web3 Wallet Auth**: SIWE (Sign-In with Ethereum) protocol
- **DID Authentication**: Using DID documents for verification
- **Unified JWT Tokens**: Single token format for all auth methods

### 2. **API Gateway Enhancements**

The API Gateway will:
- Route authentication requests to appropriate handlers
- Validate JWT tokens from any authentication method
- Extract user context (wallet address, DID, or traditional user ID)
- Forward user context to downstream services

### 3. **Keycloak Configuration**

Keycloak will be configured with:
- **Custom Identity Providers**: For Web3 wallets
- **User Federation**: Link wallet addresses to user accounts
- **Custom Authenticators**: For DID-based auth
- **Unified User Model**: Single user representation

## Technical Implementation

### Authentication Service Enhancements

```typescript
// Enhanced auth service structure
src/
├── controllers/
│   ├── traditional-auth.controller.ts    # Email/password auth
│   ├── web3-auth.controller.ts          # Wallet-based auth
│   ├── did-auth.controller.ts           # DID-based auth
│   └── unified-auth.controller.ts       # Unified auth endpoints
├── services/
│   ├── keycloak.service.ts              # Keycloak integration
│   ├── web3-auth.service.ts             # Web3 signature verification
│   ├── did-auth.service.ts              # DID verification
│   └── token.service.ts                 # JWT token management
├── middleware/
│   ├── wallet-verification.middleware.ts
│   ├── did-verification.middleware.ts
│   └── keycloak.middleware.ts
└── models/
    ├── user.model.ts                    # Unified user model
    ├── wallet.model.ts                  # Wallet associations
    └── did.model.ts                     # DID associations
```

### API Endpoints

```typescript
// Traditional Authentication
POST /api/v1/auth/login              # Email/password login
POST /api/v1/auth/register           # User registration
POST /api/v1/auth/refresh            # Token refresh

// Web3 Authentication
POST /api/v1/auth/web3/challenge     # Get signing challenge
POST /api/v1/auth/web3/verify        # Verify wallet signature
POST /api/v1/auth/web3/link          # Link wallet to existing account

// DID Authentication
POST /api/v1/auth/did/challenge      # Get DID challenge
POST /api/v1/auth/did/verify         # Verify DID signature
POST /api/v1/auth/did/register       # Register new DID

// Unified Endpoints
GET  /api/v1/auth/profile            # Get user profile (any auth method)
POST /api/v1/auth/logout             # Logout (any auth method)
```

## Configuration Updates

### Environment Variables

```bash
# Keycloak Configuration
KEYCLOAK_URL=http://keycloak:8080
KEYCLOAK_REALM=provenly
KEYCLOAK_CLIENT_ID=provenly-client
KEYCLOAK_CLIENT_SECRET=your_secret

# Web3 Configuration
WEB3_PROVIDER_URL=https://mainnet.infura.io/v3/your_key
SUPPORTED_CHAINS=1,137,42161  # Ethereum, Polygon, Arbitrum
SIWE_DOMAIN=provenly.io
SIWE_STATEMENT="Sign in to Provenly Employment VC Platform"

# DID Configuration
DID_RESOLVER_URL=https://resolver.identity.foundation
SUPPORTED_DID_METHODS=did:ebsi,did:ethr,did:key
```

### Package.json Updates

```json
{
  "dependencies": {
    // Existing dependencies...
    "ethers": "^6.8.1",
    "web3": "^4.3.0",
    "siwe": "^2.1.4",
    "keycloak-connect": "^23.0.3",
    "@keycloak/keycloak-admin-client": "^23.0.3",
    "did-resolver": "^4.1.0",
    "ethr-did-resolver": "^10.1.5",
    "did-jwt": "^7.4.7"
  }
}
```

## Database Schema Updates

### User Model Enhancement

```sql
-- Enhanced user table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE,
    keycloak_id VARCHAR(255) UNIQUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Wallet associations
CREATE TABLE user_wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    wallet_address VARCHAR(42) UNIQUE NOT NULL,
    chain_id INTEGER NOT NULL,
    verified_at TIMESTAMP DEFAULT NOW(),
    is_primary BOOLEAN DEFAULT FALSE
);

-- DID associations
CREATE TABLE user_dids (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    did VARCHAR(255) UNIQUE NOT NULL,
    did_method VARCHAR(50) NOT NULL,
    verified_at TIMESTAMP DEFAULT NOW(),
    is_primary BOOLEAN DEFAULT FALSE
);
```

## Security Considerations

### 1. **Signature Verification**
- Verify SIWE message format and signature
- Check message timestamp and nonce
- Validate wallet ownership

### 2. **DID Verification**
- Resolve DID documents
- Verify signature against DID document keys
- Check DID document validity

### 3. **Token Security**
- Short-lived access tokens (15 minutes)
- Refresh tokens with rotation
- Audience and issuer validation

### 4. **Rate Limiting**
- Stricter limits on auth endpoints
- Challenge request rate limiting
- Failed attempt tracking

## Frontend Integration

### Web3 Wallet Connection

```typescript
// Example Web3 auth flow
import { ethers } from 'ethers';
import { SiweMessage } from 'siwe';

async function authenticateWithWallet() {
  // 1. Connect wallet
  const provider = new ethers.BrowserProvider(window.ethereum);
  const signer = await provider.getSigner();
  const address = await signer.getAddress();

  // 2. Get challenge from backend
  const challengeResponse = await fetch('/api/v1/auth/web3/challenge', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ address })
  });
  const { message, nonce } = await challengeResponse.json();

  // 3. Sign message
  const siweMessage = new SiweMessage(message);
  const signature = await signer.signMessage(siweMessage.prepareMessage());

  // 4. Verify signature and get JWT
  const authResponse = await fetch('/api/v1/auth/web3/verify', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, signature })
  });
  
  const { access_token } = await authResponse.json();
  return access_token;
}
```

## Deployment Considerations

### 1. **Keycloak Custom Extensions**
- Deploy custom authenticators for Web3/DID auth
- Configure identity providers for wallet linking
- Set up user federation for unified accounts

### 2. **Environment-Specific Configuration**
- Different RPC endpoints per environment
- Test networks for development
- Production security hardening

### 3. **Monitoring & Logging**
- Track authentication method usage
- Monitor failed authentication attempts
- Log wallet/DID verification events

## Migration Strategy

### Phase 1: Keycloak Integration
1. Set up Keycloak with existing user base
2. Migrate traditional authentication
3. Test OAuth2/OIDC flows

### Phase 2: Web3 Authentication
1. Add Web3 auth endpoints
2. Implement SIWE protocol
3. Add wallet linking functionality

### Phase 3: DID Authentication
1. Integrate DID resolution
2. Add DID-based auth flows
3. Link DIDs to user accounts

### Phase 4: Unified Experience
1. Single sign-on across all methods
2. Account linking and management
3. Progressive enhancement

This approach provides a comprehensive authentication system that supports traditional, Web3, and DID-based authentication without requiring additional microservices, maintaining the clean architecture while adding powerful new capabilities.
