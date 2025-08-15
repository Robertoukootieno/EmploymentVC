# Provenly Employment VC Platform - Correct Architecture

## 🏗️ **Architecture Overview**

### **Frontend Applications (User-facing)**
```
frontend-apps/
├── issuer-app/                 # For organizations to issue credentials
│   ├── web/                   # React/Next.js web application
│   └── mobile/                # React Native mobile app
├── verifier-app/              # For verifiers to verify credentials
│   ├── web/                   # React/Next.js with QR code scanning
│   └── mobile/                # React Native with camera integration
└── holder-wallet/             # For individuals to manage credentials
    ├── web/                   # Web wallet interface
    └── mobile/                # Mobile wallet app (custodial & non-custodial)
```

### **Backend Libraries (Core functionality)**
```
backend-libraries/
├── auth-lib/                  # Authentication and authorization
├── credentials-lib/           # VC/VP processing and validation
├── crypto-lib/                # Cryptographic operations (signing, encryption)
├── protocols-lib/             # Communication protocols (DIDComm, etc.)
├── sdjwts-lib/               # Selective Disclosure JWTs
├── core-wallet-lib/          # Wallet operations (custodial & non-custodial)
├── utils-lib/                # Common utilities and helpers
├── did-lib/                  # DID operations and resolution
├── library-commons/          # Shared components and interfaces
└── openid4vc-lib/           # OpenID for Verifiable Credentials/Presentations
```

### **Backend Services (APIs)**
```
backend-services/
├── issuer-api/               # Credential issuance service
├── verifier-api/             # Credential verification service
├── wallet-api/               # Wallet management service
├── web3-login-service/       # Web3 authentication service
├── e2e-test-service/         # End-to-end testing service
├── auth-service/             # Authentication service
├── did-registry/             # DID registry service
├── schema-registry/          # Schema management service
└── api-gateway/              # API gateway and routing
```

## 🎯 **Service Responsibilities**

### **Frontend Applications**

#### **Issuer App**
- **Purpose**: Organizations issue employment credentials
- **Features**:
  - Employee onboarding and credential creation
  - Bulk credential issuance
  - Credential template management
  - Analytics and reporting
  - Integration with HR systems

#### **Verifier App**
- **Purpose**: Verify employment credentials
- **Features**:
  - QR code scanning for credential verification
  - Selective disclosure requests
  - Verification result display
  - Integration with verification workflows
  - Mobile and web interfaces

#### **Holder Wallet**
- **Purpose**: Individuals manage their credentials
- **Features**:
  - Custodial wallet (platform-managed keys)
  - Non-custodial wallet (user-controlled keys)
  - Credential storage and organization
  - Presentation creation with selective disclosure
  - Cross-platform (web and mobile)

### **Backend Libraries**

#### **Auth Library**
- JWT token management
- Multi-method authentication (traditional, Web3, DID)
- Authorization and RBAC
- Session management

#### **Credentials Library**
- VC/VP creation and parsing
- JSON-LD processing
- Schema validation
- Credential lifecycle management

#### **Crypto Library**
- Digital signatures (Ed25519, ECDSA, BBS+)
- Encryption/decryption (AES-GCM)
- Key generation and management
- Hash functions and utilities

#### **Protocols Library**
- DIDComm messaging
- OpenID4VC/VP protocols
- SIWE (Sign-In with Ethereum)
- Custom protocol implementations

#### **SD-JWTs Library**
- Selective Disclosure JWT creation
- Disclosure map generation
- Verification with selective disclosure
- Privacy-preserving presentations

#### **Core Wallet Library**
- Wallet creation and management
- Key storage (custodial/non-custodial)
- Credential storage operations
- Backup and recovery

#### **Utils Library**
- Common utilities and helpers
- Date/time operations
- Validation functions
- Configuration management

#### **DID Library**
- DID creation and resolution
- DID document management
- EBSI integration
- Multi-method DID support

#### **Library Commons**
- Shared interfaces and contracts
- Common data models
- Exception handling
- Logging utilities

#### **OpenID4VC Library**
- OpenID for Verifiable Credentials
- OpenID for Verifiable Presentations
- Authorization server integration
- Client implementations

### **Backend Services**

#### **Issuer API** (Port 8081)
- `POST /api/v1/credentials/issue` - Issue credentials
- `POST /api/v1/credentials/batch-issue` - Bulk issuance
- `GET /api/v1/credentials/templates` - Credential templates
- `POST /api/v1/credentials/revoke` - Revoke credentials

#### **Verifier API** (Port 8082)
- `POST /api/v1/verify/credential` - Verify credential
- `POST /api/v1/verify/presentation` - Verify presentation
- `POST /api/v1/verify/qr-code` - QR code verification
- `GET /api/v1/verify/requirements` - Verification requirements

#### **Wallet API** (Port 8083)
- `POST /api/v1/wallets/custodial` - Create custodial wallet
- `POST /api/v1/wallets/non-custodial` - Register non-custodial wallet
- `GET /api/v1/wallets/{id}/credentials` - List credentials
- `POST /api/v1/wallets/{id}/presentations` - Create presentation

#### **Web3 Login Service** (Port 8084)
- `POST /api/v1/web3/challenge` - Generate Web3 challenge
- `POST /api/v1/web3/verify` - Verify Web3 signature
- `POST /api/v1/web3/link` - Link wallet to account
- `GET /api/v1/web3/chains` - Supported chains

#### **E2E Test Service** (Port 8085)
- `POST /api/v1/test/scenarios` - Run test scenarios
- `GET /api/v1/test/results` - Get test results
- `POST /api/v1/test/mock-data` - Generate mock data
- `GET /api/v1/test/health` - Test service health

## 🔄 **Data Flow**

### **Credential Issuance Flow**
1. **Issuer App** → **Issuer API** → **Credentials Library** → **Crypto Library**
2. **DID Library** resolves issuer DID
3. **Credentials Library** creates VC with **SD-JWTs Library**
4. **Crypto Library** signs credential
5. Result returned to **Issuer App**

### **Credential Verification Flow**
1. **Verifier App** scans QR code → **Verifier API**
2. **Verifier API** → **Credentials Library** → **Crypto Library**
3. **DID Library** resolves issuer/holder DIDs
4. **SD-JWTs Library** processes selective disclosure
5. Verification result returned to **Verifier App**

### **Wallet Operations Flow**
1. **Holder Wallet** → **Wallet API** → **Core Wallet Library**
2. **Auth Library** handles authentication
3. **Crypto Library** manages encryption/decryption
4. **Credentials Library** processes VC operations
5. Results returned to **Holder Wallet**

## 🚀 **Development Workflow**

### **Library Development**
```bash
# Build all libraries
./gradlew :auth-lib:build :credentials-lib:build :crypto-lib:build

# Test specific library
./gradlew :sdjwts-lib:test

# Publish libraries locally
./gradlew publishToMavenLocal
```

### **Service Development**
```bash
# Run specific service
./gradlew :issuer-api:bootRun

# Run all services
./gradlew bootRun --parallel

# Integration tests
./gradlew :e2e-test-service:test
```

### **Frontend Development**
```bash
# Issuer web app
cd frontend-apps/issuer-app/web
npm run dev

# Verifier mobile app
cd frontend-apps/verifier-app/mobile
npx react-native run-android
```

This architecture provides clear separation of concerns, reusable libraries, and scalable services for a comprehensive VC platform.
