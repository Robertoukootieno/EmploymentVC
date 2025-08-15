# Web3 & Keycloak Authentication Integration

## Overview

The Provenly Employment VC Platform now supports **three authentication methods** without requiring additional microservices:

1. **Traditional Authentication** (Email/Password via Keycloak)
2. **Web3 Authentication** (Wallet signatures via SIWE)
3. **DID Authentication** (DID-based signatures)

## Architecture Decision

### ✅ **Enhanced Authentication Service Approach**
Instead of creating separate microservices, we enhanced the existing **Authentication Service** to handle all authentication methods. This approach provides:

- **Unified User Management**: Single user model across all auth methods
- **Simplified Architecture**: No additional services to manage
- **Consistent API**: Single authentication endpoint with multiple methods
- **Shared Security**: Common rate limiting, session management, and token handling

### 🏗️ **Service Integration**

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Gateway   │    │  Auth Service   │    │    Keycloak     │
│                 │    │                 │    │                 │
│ • Route Auth    │◄──►│ • Traditional   │◄──►│ • User Store    │
│ • Validate JWT  │    │ • Web3 (SIWE)   │    │ • OAuth2/OIDC   │
│ • Extract User  │    │ • DID Auth      │    │ • Role Mgmt     │
└─────────────────┘    │ • Unified Tokens│    └─────────────────┘
                       └─────────────────┘
                                │
                       ┌─────────────────┐
                       │     Redis       │
                       │                 │
                       │ • Challenges    │
                       │ • Sessions      │
                       │ • Rate Limits   │
                       └─────────────────┘
```

## Implementation Details

### 1. **Enhanced Authentication Service**

#### New Dependencies Added:
```json
{
  "ethers": "^6.8.1",                    // Ethereum wallet interactions
  "web3": "^4.3.0",                     // Web3 utilities
  "siwe": "^2.1.4",                     // Sign-In with Ethereum
  "keycloak-connect": "^23.0.3",        // Keycloak integration
  "@keycloak/keycloak-admin-client": "^23.0.3", // Admin operations
  "did-resolver": "^4.1.0",             // DID resolution
  "ethr-did-resolver": "^10.1.5",       // Ethereum DID resolver
  "did-jwt": "^7.4.7"                   // DID JWT handling
}
```

#### New Controllers:
- `Web3AuthController` - Handles wallet-based authentication
- `TraditionalAuthController` - Handles email/password authentication  
- `DIDAuthController` - Handles DID-based authentication
- `UnifiedAuthController` - Unified user management

#### New Services:
- `Web3AuthService` - SIWE protocol implementation
- `KeycloakService` - Keycloak integration and user management
- `DIDAuthService` - DID resolution and verification
- `TokenService` - Unified JWT token management

### 2. **API Endpoints**

#### Traditional Authentication:
```
POST /api/v1/auth/login          # Email/password login
POST /api/v1/auth/register       # User registration
POST /api/v1/auth/refresh        # Token refresh
```

#### Web3 Authentication:
```
POST /api/v1/auth/web3/challenge # Get signing challenge
POST /api/v1/auth/web3/verify    # Verify wallet signature
POST /api/v1/auth/web3/link      # Link wallet to account
GET  /api/v1/auth/web3/wallets   # Get linked wallets
```

#### DID Authentication:
```
POST /api/v1/auth/did/challenge  # Get DID challenge
POST /api/v1/auth/did/verify     # Verify DID signature
POST /api/v1/auth/did/register   # Register DID
POST /api/v1/auth/did/link       # Link DID to account
```

#### Unified Endpoints:
```
GET  /api/v1/auth/profile        # Get user profile (any auth method)
POST /api/v1/auth/logout         # Logout (any auth method)
GET  /api/v1/auth/sessions       # Get active sessions
```

### 3. **Database Schema Updates**

#### Enhanced User Model:
```sql
-- Main users table
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

### 4. **Authentication Flows**

#### Web3 Wallet Authentication:
```typescript
// 1. Frontend requests challenge
const challenge = await fetch('/api/v1/auth/web3/challenge', {
  method: 'POST',
  body: JSON.stringify({ address: walletAddress, chainId: 1 })
});

// 2. User signs message with wallet
const signature = await signer.signMessage(challenge.message);

// 3. Backend verifies signature and returns JWT
const auth = await fetch('/api/v1/auth/web3/verify', {
  method: 'POST',
  body: JSON.stringify({ message: challenge.message, signature })
});
```

#### DID Authentication:
```typescript
// 1. Frontend requests DID challenge
const challenge = await fetch('/api/v1/auth/did/challenge', {
  method: 'POST',
  body: JSON.stringify({ did: 'did:ebsi:zxHaP8AmTWURkGkuQzKyD5' })
});

// 2. Sign challenge with DID key
const signature = await didKey.sign(challenge.data);

// 3. Verify and authenticate
const auth = await fetch('/api/v1/auth/did/verify', {
  method: 'POST',
  body: JSON.stringify({ challenge: challenge.data, signature, did })
});
```

### 5. **Keycloak Configuration**

#### Custom Identity Providers:
- **Web3 Provider**: Links wallet addresses to Keycloak users
- **DID Provider**: Links DIDs to Keycloak users

#### User Federation:
- Automatic user creation for Web3/DID authentication
- Attribute mapping for wallet addresses and DIDs
- Role synchronization

#### Custom Authenticators:
- SIWE message verification
- DID signature verification
- Multi-factor authentication support

## Configuration

### Environment Variables:
```bash
# Web3 Configuration
WEB3_PROVIDER_URL=https://mainnet.infura.io/v3/your_key
SUPPORTED_CHAINS=1,137,42161
SIWE_DOMAIN=provenly.io
SIWE_STATEMENT="Sign in to Provenly Employment VC Platform"

# DID Configuration  
DID_RESOLVER_URL=https://resolver.identity.foundation
SUPPORTED_DID_METHODS=did:ebsi,did:ethr,did:key
DID_CACHE_TTL=3600

# Keycloak Configuration
KEYCLOAK_URL=http://keycloak:8080
KEYCLOAK_REALM=provenly
KEYCLOAK_CLIENT_ID=provenly-client
KEYCLOAK_CLIENT_SECRET=your_secret
KEYCLOAK_ADMIN_USERNAME=admin
KEYCLOAK_ADMIN_PASSWORD=admin_password

# Authentication Methods
AUTH_METHODS_ENABLED=traditional,web3,did
DEFAULT_AUTH_METHOD=traditional
ALLOW_ACCOUNT_LINKING=true
```

## Security Features

### 1. **Multi-Method Security**:
- Rate limiting per authentication method
- Challenge-response for Web3/DID auth
- Token rotation and expiration
- Session management across methods

### 2. **Wallet Security**:
- SIWE protocol compliance
- Signature verification
- Nonce-based challenge system
- Chain ID validation

### 3. **DID Security**:
- DID document resolution
- Key verification against DID document
- Proof validation
- Method-specific security

### 4. **Unified Security**:
- Single JWT format for all methods
- Consistent authorization across services
- Audit logging for all authentication events
- Account linking security

## Benefits of This Approach

### ✅ **Advantages**:
1. **No Additional Services**: Uses existing authentication service
2. **Unified User Experience**: Single account, multiple auth methods
3. **Simplified Architecture**: Fewer moving parts to manage
4. **Consistent Security**: Shared security policies and monitoring
5. **Easy Account Linking**: Users can link multiple auth methods
6. **Scalable**: Can add new auth methods without new services

### 🔄 **Migration Path**:
1. **Phase 1**: Deploy enhanced authentication service
2. **Phase 2**: Configure Keycloak with custom providers
3. **Phase 3**: Enable Web3 authentication
4. **Phase 4**: Enable DID authentication
5. **Phase 5**: Account linking and unified experience

## Frontend Integration

### React/TypeScript Example:
```typescript
import { AuthProvider, useAuth } from '@provenly/auth-sdk';

function App() {
  return (
    <AuthProvider 
      apiUrl="https://api.provenly.io"
      supportedMethods={['traditional', 'web3', 'did']}
    >
      <AuthenticatedApp />
    </AuthProvider>
  );
}

function LoginComponent() {
  const { login, loginWithWallet, loginWithDID } = useAuth();
  
  // Traditional login
  const handleEmailLogin = async (email, password) => {
    await login({ email, password });
  };
  
  // Web3 wallet login
  const handleWalletLogin = async () => {
    await loginWithWallet({ provider: window.ethereum });
  };
  
  // DID login
  const handleDIDLogin = async (did) => {
    await loginWithDID({ did, keyPair: userKeyPair });
  };
}
```

This integration provides a comprehensive, secure, and scalable authentication system that supports modern Web3 and DID-based authentication while maintaining compatibility with traditional authentication methods.
