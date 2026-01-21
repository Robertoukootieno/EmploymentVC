# Provenly Employment VC Platform - Corrected Architecture

## 🎯 **You Were Absolutely Right!**

Thank you for the correction! The architecture is now properly structured with clear separation of concerns:

## 🏗️ **Correct Architecture Overview**

### **🖥️ Frontend Applications (User-facing)**
```
employmentVC-Applications/
├── issuer-app/                 # For organizations to issue credentials
│   ├── web/                   # React/Next.js web application
│   └── mobile/                # React Native mobile app
├── verifier-app/              # For verifiers to verify credentials
│   ├── web/                   # React/Next.js with QR code scanning
│   └── mobile/                # React Native with camera integration
└── holder-wallet/             # For individuals to manage credentials
    ├── web/                   # Web wallet interface
    └── mobile/                # Mobile wallet (custodial & non-custodial)
```

### **📚 Backend Libraries (Core functionality)**
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

### **🔧 Backend Services (APIs)**
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

## 🎯 **Key Responsibilities**

### **Frontend Applications**

#### **🏢 Issuer App (Organizations)**
- **Web Interface**: HR departments, credential administrators
- **Mobile App**: Field managers, mobile credential issuance
- **Features**:
  - Employee onboarding workflows
  - Bulk credential issuance
  - Credential template management
  - Analytics and reporting dashboard
  - Integration with HR systems (SAP, Workday, etc.)

#### **🔍 Verifier App (Verification)**
- **Web Interface**: Desktop verification workflows
- **Mobile App**: QR code scanning, field verification
- **Features**:
  - **QR Code Scanning** - Camera-based credential verification
  - Selective disclosure requests
  - Real-time verification results
  - Verification history and audit trails
  - Integration with access control systems

#### **👤 Holder Wallet (Individuals)**
- **Web Interface**: Desktop credential management
- **Mobile App**: Mobile-first wallet experience
- **Features**:
  - **Custodial Mode**: Platform manages keys, easy onboarding
  - **Non-Custodial Mode**: User controls keys, maximum privacy
  - Credential organization and tagging
  - Presentation creation with selective disclosure
  - Cross-platform synchronization

### **Backend Libraries (Reusable Components)**

#### **🔐 Auth Library**
- Multi-method authentication (traditional, Web3, DID)
- JWT token management and validation
- Role-based access control (RBAC)
- Session management and security

#### **📜 Credentials Library**
- VC/VP creation, parsing, and validation
- JSON-LD processing and context resolution
- Schema validation and compliance checking
- Credential lifecycle management

#### **🔒 Crypto Library**
- Digital signatures (Ed25519, ECDSA, BBS+)
- Encryption/decryption (AES-GCM, ChaCha20)
- Key generation and management
- Hash functions and cryptographic utilities

#### **📡 Protocols Library**
- DIDComm messaging protocols
- OpenID4VC/VP protocol implementations
- SIWE (Sign-In with Ethereum) protocol
- Custom communication protocols

#### **🎭 SD-JWTs Library**
- **Selective Disclosure JWT** creation and processing
- Disclosure map generation and validation
- Privacy-preserving presentations
- Holder binding and verification

#### **💼 Core Wallet Library**
- Wallet creation and management
- Key storage (custodial/non-custodial)
- Credential storage operations
- Backup and recovery mechanisms

#### **🛠️ Utils Library**
- Common utilities and helper functions
- Date/time operations and formatting
- Validation functions and constraints
- Configuration management

#### **🆔 DID Library**
- DID creation, resolution, and management
- DID document operations
- **EBSI integration** for European DIDs
- Multi-method DID support (did:ebsi, did:ethr, did:key)

#### **🤝 Library Commons**
- Shared interfaces and contracts
- Common data models and DTOs
- Exception handling framework
- Logging and monitoring utilities

#### **🌐 OpenID4VC Library**
- **OpenID for Verifiable Credentials** implementation
- **OpenID for Verifiable Presentations** support
- Authorization server integration
- Client SDK implementations

### **Backend Services (Focused APIs)**

#### **🏭 Issuer API** (Port 8081)
```http
POST /api/v1/credentials/issue           # Issue single credential
POST /api/v1/credentials/batch-issue     # Bulk credential issuance
GET  /api/v1/credentials/templates       # Credential templates
POST /api/v1/credentials/revoke          # Revoke credentials
GET  /api/v1/issuers/{id}/analytics      # Issuance analytics
```

#### **✅ Verifier API** (Port 8082)
```http
POST /api/v1/verify/credential           # Verify credential
POST /api/v1/verify/presentation         # Verify presentation
POST /api/v1/verify/qr-code             # QR code verification
GET  /api/v1/verify/requirements        # Verification requirements
POST /api/v1/verify/selective-disclosure # Selective disclosure verification
```

#### **💼 Wallet API** (Port 8083)
```http
POST /api/v1/wallets/custodial          # Create custodial wallet
POST /api/v1/wallets/non-custodial      # Register non-custodial wallet
GET  /api/v1/wallets/{id}/credentials   # List wallet credentials
POST /api/v1/wallets/{id}/presentations # Create presentations
PUT  /api/v1/wallets/{id}/backup        # Backup wallet
```

#### **🌐 Web3 Login Service** (Port 8084)
```http
POST /api/v1/web3/challenge             # Generate Web3 challenge
POST /api/v1/web3/verify                # Verify Web3 signature
POST /api/v1/web3/link                  # Link wallet to account
GET  /api/v1/web3/chains                # Supported blockchain networks
```

#### **🧪 E2E Test Service** (Port 8085)
```http
POST /api/v1/test/scenarios             # Run test scenarios
GET  /api/v1/test/results               # Get test results
POST /api/v1/test/mock-data             # Generate mock data
GET  /api/v1/test/coverage              # Test coverage reports
```

## 🔄 **Data Flow Examples**

### **Credential Issuance Flow**
1. **Issuer App** → **Issuer API** → **Credentials Library** → **Crypto Library**
2. **DID Library** resolves issuer DID via EBSI
3. **Credentials Library** creates VC with **SD-JWTs Library**
4. **Crypto Library** signs credential with BBS+
5. Result stored via **Core Wallet Library** (if custodial)

### **QR Code Verification Flow**
1. **Verifier App** scans QR → **Verifier API**
2. **Verifier API** → **Credentials Library** → **Crypto Library**
3. **DID Library** resolves issuer/holder DIDs
4. **SD-JWTs Library** processes selective disclosure
5. **Protocols Library** handles presentation request
6. Verification result returned to **Verifier App**

### **Wallet Operations Flow**
1. **Holder Wallet** → **Wallet API** → **Core Wallet Library**
2. **Auth Library** handles authentication
3. **Crypto Library** manages encryption/decryption
4. **Credentials Library** processes VC operations
5. **SD-JWTs Library** creates selective presentations

## 🚀 **Development Benefits**

### **✅ Clear Separation**
- **Frontend**: User experience and interfaces
- **Libraries**: Reusable business logic
- **Services**: Focused API endpoints

### **✅ Reusability**
- Libraries can be used across multiple services
- Consistent implementations across the platform
- Easy to test and maintain

### **✅ Scalability**
- Services can be scaled independently
- Libraries provide consistent performance
- Clear dependency management

### **✅ Maintainability**
- Single responsibility principle
- Clear interfaces and contracts
- Comprehensive testing at each layer

This architecture provides a **solid foundation** for building a comprehensive, scalable, and maintainable Verifiable Credentials platform! 🎉
