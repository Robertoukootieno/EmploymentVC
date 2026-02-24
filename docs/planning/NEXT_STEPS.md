# Provenly Employment VC Platform - Next Steps & Roadmap

**Date**: January 21, 2026  
**Current Status**: Early Development (15% Complete)

---

## 🎯 **Immediate Priorities (Next 2-4 Weeks)**

### **Phase 1: Core Backend Services** 🔴 **CRITICAL**

The mobile app is 80% complete but **cannot function without backend APIs**. Priority is implementing the backend services.

#### **1.1 Implement Wallet API** ⭐ **HIGHEST PRIORITY**
**Why**: Mobile wallet app needs these endpoints to work

**Tasks**:
- [ ] Create `WalletController` with REST endpoints
- [ ] Implement `WalletService` for business logic
- [ ] Create `Wallet` entity and repository
- [ ] Implement custodial wallet creation
- [ ] Implement non-custodial wallet registration
- [ ] Add wallet listing and retrieval endpoints

**Endpoints to Implement**:
```java
POST   /api/v1/wallets/custodial          // Create custodial wallet
POST   /api/v1/wallets/non-custodial      // Register non-custodial wallet
GET    /api/v1/wallets                    // List user wallets
GET    /api/v1/wallets/{id}              // Get wallet details
DELETE /api/v1/wallets/{id}              // Delete wallet
```

**Estimated Time**: 3-5 days

---

#### **1.2 Implement Issuer API** ⭐ **HIGH PRIORITY**
**Why**: Core functionality for credential issuance

**Tasks**:
- [ ] Create `IssuerController` with REST endpoints
- [ ] Implement `CredentialService` for VC creation
- [ ] Integrate with Walt.id for VC signing
- [ ] Create `Credential` entity and repository
- [ ] Implement credential templates
- [ ] Add revocation functionality

**Endpoints to Implement**:
```java
POST /api/v1/credentials/issue           // Issue credential
POST /api/v1/credentials/batch-issue     // Bulk issuance
GET  /api/v1/credentials/templates       // Get templates
POST /api/v1/credentials/revoke          // Revoke credential
GET  /api/v1/credentials/{id}           // Get credential
```

**Estimated Time**: 5-7 days

---

#### **1.3 Implement Verifier API** ⭐ **HIGH PRIORITY**
**Why**: Core functionality for credential verification

**Tasks**:
- [ ] Create `VerifierController` with REST endpoints
- [ ] Implement `VerificationService` for VC/VP validation
- [ ] Integrate with Walt.id for signature verification
- [ ] Implement selective disclosure verification
- [ ] Add QR code generation for verification requests

**Endpoints to Implement**:
```java
POST /api/v1/verify/credential           // Verify credential
POST /api/v1/verify/presentation         // Verify presentation
POST /api/v1/verify/selective-disclosure // Selective disclosure
GET  /api/v1/verify/requirements        // Get verification requirements
```

**Estimated Time**: 4-6 days

---

#### **1.4 Complete Auth Service** ⭐ **HIGH PRIORITY**
**Why**: All APIs need authentication

**Tasks**:
- [ ] Implement `AuthController` with login/logout endpoints
- [ ] Configure Keycloak integration
- [ ] Implement JWT token generation and validation
- [ ] Add Web3 wallet signature authentication
- [ ] Implement refresh token mechanism
- [ ] Create `User` entity and repository

**Endpoints to Implement**:
```java
POST /api/v1/auth/login                  // Traditional login
POST /api/v1/auth/web3-login            // Web3 wallet login
POST /api/v1/auth/refresh               // Refresh token
POST /api/v1/auth/logout                // Logout
GET  /api/v1/auth/me                    // Get current user
```

**Estimated Time**: 4-6 days

---

#### **1.5 Implement API Gateway** 🟡 **MEDIUM PRIORITY**
**Why**: Central entry point for all APIs

**Tasks**:
- [ ] Configure Spring Cloud Gateway
- [ ] Set up routing to backend services
- [ ] Implement rate limiting
- [ ] Add CORS configuration
- [ ] Configure authentication filters
- [ ] Add request/response logging

**Estimated Time**: 2-3 days

---

### **Phase 2: Backend Libraries** 🟡 **MEDIUM PRIORITY**

#### **2.1 Implement Core Libraries**
**Priority Order**:
1. [ ] `library-commons` - Base classes, exceptions, DTOs
2. [ ] `crypto-lib` - Signing, encryption, key management
3. [ ] `credentials-lib` - VC/VP creation and validation
4. [ ] `did-lib` - DID operations and resolution
5. [ ] `auth-lib` - Authentication utilities
6. [ ] `core-wallet-lib` - Wallet operations
7. [ ] `utils-lib` - Common utilities
8. [ ] `sdjwts-lib` - Selective Disclosure JWTs
9. [ ] `protocols-lib` - DIDComm and protocols
10. [ ] `openid4vc-lib` - OpenID4VC implementation

**Estimated Time**: 3-4 weeks (can be done in parallel with services)

---

### **Phase 3: Complete Mobile Holder Wallet** 🟢 **IN PROGRESS**

#### **3.1 Initialize Native Projects**
**Tasks**:
- [ ] Run `npx react-native init` or use existing template
- [ ] Configure iOS project (Xcode)
- [ ] Configure Android project (Android Studio)
- [ ] Set up code signing
- [ ] Configure app icons and splash screens

**Estimated Time**: 1-2 days

---

#### **3.2 Complete UI Screens**
**Tasks**:
- [ ] Onboarding screens
- [ ] Login/Registration screens
- [ ] Wallet dashboard
- [ ] Credential list view
- [ ] Credential detail view
- [ ] QR code scanner
- [ ] Settings screen
- [ ] Profile screen

**Estimated Time**: 1-2 weeks

---

#### **3.3 Testing & QA**
**Tasks**:
- [ ] Unit tests for services
- [ ] Integration tests for API calls
- [ ] UI component tests
- [ ] End-to-end tests
- [ ] Security testing
- [ ] Performance testing

**Estimated Time**: 1 week

---

### **Phase 4: Web Applications** 🔴 **NOT STARTED**

#### **4.1 Holder Wallet Web**
- [ ] Set up Next.js project
- [ ] Implement wallet UI
- [ ] Web3 wallet integration
- [ ] Responsive design

**Estimated Time**: 2-3 weeks

---

#### **4.2 Issuer App (Web & Mobile)**
- [ ] Set up Next.js project (web)
- [ ] Set up React Native project (mobile)
- [ ] Implement credential issuance UI
- [ ] Bulk issuance features
- [ ] Analytics dashboard

**Estimated Time**: 3-4 weeks

---

#### **4.3 Verifier App (Web & Mobile)**
- [ ] Set up Next.js project (web)
- [ ] Set up React Native project (mobile)
- [ ] QR code scanning
- [ ] Verification result display
- [ ] Selective disclosure UI

**Estimated Time**: 2-3 weeks

---

## 🚀 **Recommended Development Sequence**

### **Sprint 1 (Week 1-2): Foundation**
1. Complete Auth Service
2. Implement library-commons
3. Implement crypto-lib
4. Set up development environment

### **Sprint 2 (Week 3-4): Core APIs**
1. Implement Wallet API
2. Implement credentials-lib
3. Implement did-lib
4. Initialize mobile native projects

### **Sprint 3 (Week 5-6): Issuance & Verification**
1. Implement Issuer API
2. Implement Verifier API
3. Complete mobile wallet UI
4. Integration testing

### **Sprint 4 (Week 7-8): Integration & Testing**
1. Implement API Gateway
2. Complete remaining libraries
3. End-to-end testing
4. Security audit

### **Sprint 5 (Week 9-12): Web Applications**
1. Holder Wallet Web
2. Issuer App Web
3. Verifier App Web
4. Documentation

---

## 📋 **Prerequisites & Setup**

### **Before Starting Development**:
1. [ ] Set up Azure App Registration (for Entra ID integration)
2. [ ] Configure Keycloak realm and clients
3. [ ] Set up Walt.id services
4. [ ] Configure PostgreSQL databases
5. [ ] Set up development environment variables
6. [ ] Start infrastructure services (`COMPOSE_PROJECT_NAME=employmentvc docker compose up -d`)

---

*See PROJECT_STATUS_REPORT.md for current status details.*

