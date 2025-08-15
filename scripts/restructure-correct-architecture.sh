#!/bin/bash

# Script to restructure project with correct architecture

set -e

echo "🏗️ Restructuring Provenly Employment VC Platform with correct architecture..."

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_step() {
    echo -e "${BLUE}==>${NC} $1"
}

print_success() {
    echo -e "${GREEN}✅${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠️${NC} $1"
}

# Create correct directory structure
print_step "Creating correct project structure..."

# Remove old incorrect structure
rm -rf backend/application-service

# Frontend Applications (User-facing)
mkdir -p frontend-apps/{issuer-app,verifier-app,holder-wallet}/{web,mobile}

# Backend Libraries (Core functionality)
mkdir -p backend-libraries/{auth-lib,credentials-lib,crypto-lib,protocols-lib,sdjwts-lib,core-wallet-lib,utils-lib,did-lib,library-commons,openid4vc-lib}

# Backend Services (APIs)
mkdir -p backend-services/{issuer-api,verifier-api,wallet-api,web3-login-service,e2e-test-service,auth-service,did-registry,schema-registry,api-gateway}

# Infrastructure and shared resources
mkdir -p {infra,shared,docs,scripts,k8s}

print_success "Directory structure created"

# Create root build configuration
print_step "Creating root build configuration..."

# Update settings.gradle for correct structure
cat > settings.gradle << 'EOF'
rootProject.name = 'provenly-employment-vc'

// Backend Libraries
include 'auth-lib'
include 'credentials-lib'
include 'crypto-lib'
include 'protocols-lib'
include 'sdjwts-lib'
include 'core-wallet-lib'
include 'utils-lib'
include 'did-lib'
include 'library-commons'
include 'openid4vc-lib'

// Backend Services
include 'issuer-api'
include 'verifier-api'
include 'wallet-api'
include 'web3-login-service'
include 'e2e-test-service'
include 'auth-service'
include 'did-registry'
include 'schema-registry'
include 'api-gateway'

// Set project directories
project(':auth-lib').projectDir = file('backend-libraries/auth-lib')
project(':credentials-lib').projectDir = file('backend-libraries/credentials-lib')
project(':crypto-lib').projectDir = file('backend-libraries/crypto-lib')
project(':protocols-lib').projectDir = file('backend-libraries/protocols-lib')
project(':sdjwts-lib').projectDir = file('backend-libraries/sdjwts-lib')
project(':core-wallet-lib').projectDir = file('backend-libraries/core-wallet-lib')
project(':utils-lib').projectDir = file('backend-libraries/utils-lib')
project(':did-lib').projectDir = file('backend-libraries/did-lib')
project(':library-commons').projectDir = file('backend-libraries/library-commons')
project(':openid4vc-lib').projectDir = file('backend-libraries/openid4vc-lib')

project(':issuer-api').projectDir = file('backend-services/issuer-api')
project(':verifier-api').projectDir = file('backend-services/verifier-api')
project(':wallet-api').projectDir = file('backend-services/wallet-api')
project(':web3-login-service').projectDir = file('backend-services/web3-login-service')
project(':e2e-test-service').projectDir = file('backend-services/e2e-test-service')
project(':auth-service').projectDir = file('backend-services/auth-service')
project(':did-registry').projectDir = file('backend-services/did-registry')
project(':schema-registry').projectDir = file('backend-services/schema-registry')
project(':api-gateway').projectDir = file('backend-services/api-gateway')
EOF

# Update root build.gradle
cat > build.gradle << 'EOF'
plugins {
    id 'java-library'
    id 'org.springframework.boot' version '3.2.0' apply false
    id 'io.spring.dependency-management' version '1.1.4' apply false
    id 'com.github.spotbugs' version '5.2.1' apply false
    id 'checkstyle' apply false
}

allprojects {
    group = 'io.provenly'
    version = '1.0.0'
    
    repositories {
        mavenCentral()
        maven { url 'https://repo.spring.io/milestone' }
        maven { url 'https://repo.spring.io/snapshot' }
        maven { url 'https://jitpack.io' }
    }
}

// Configure backend libraries
configure(subprojects.findAll { it.path.startsWith(':auth-lib') || 
                                it.path.startsWith(':credentials-lib') ||
                                it.path.startsWith(':crypto-lib') ||
                                it.path.startsWith(':protocols-lib') ||
                                it.path.startsWith(':sdjwts-lib') ||
                                it.path.startsWith(':core-wallet-lib') ||
                                it.path.startsWith(':utils-lib') ||
                                it.path.startsWith(':did-lib') ||
                                it.path.startsWith(':library-commons') ||
                                it.path.startsWith(':openid4vc-lib') }) {
    apply plugin: 'java-library'
    apply plugin: 'checkstyle'
    apply plugin: 'com.github.spotbugs'

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    dependencies {
        // Common library dependencies
        api 'org.slf4j:slf4j-api:2.0.9'
        api 'com.fasterxml.jackson.core:jackson-databind:2.16.0'
        api 'org.apache.commons:commons-lang3:3.13.0'
        
        compileOnly 'org.projectlombok:lombok:1.18.30'
        annotationProcessor 'org.projectlombok:lombok:1.18.30'
        
        testImplementation 'org.junit.jupiter:junit-jupiter:5.10.1'
        testImplementation 'org.mockito:mockito-core:5.7.0'
        testImplementation 'org.assertj:assertj-core:3.24.2'
    }

    tasks.named('test') {
        useJUnitPlatform()
    }
}

// Configure backend services
configure(subprojects.findAll { it.path.startsWith(':issuer-api') ||
                                it.path.startsWith(':verifier-api') ||
                                it.path.startsWith(':wallet-api') ||
                                it.path.startsWith(':web3-login-service') ||
                                it.path.startsWith(':e2e-test-service') ||
                                it.path.startsWith(':auth-service') ||
                                it.path.startsWith(':did-registry') ||
                                it.path.startsWith(':schema-registry') ||
                                it.path.startsWith(':api-gateway') }) {
    apply plugin: 'java'
    apply plugin: 'org.springframework.boot'
    apply plugin: 'io.spring.dependency-management'
    apply plugin: 'checkstyle'
    apply plugin: 'com.github.spotbugs'

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    dependencies {
        // Spring Boot dependencies
        implementation 'org.springframework.boot:spring-boot-starter-web'
        implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
        implementation 'org.springframework.boot:spring-boot-starter-data-redis'
        implementation 'org.springframework.boot:spring-boot-starter-security'
        implementation 'org.springframework.boot:spring-boot-starter-validation'
        implementation 'org.springframework.boot:spring-boot-starter-actuator'
        
        // Database
        runtimeOnly 'org.postgresql:postgresql'
        
        // Monitoring
        implementation 'io.micrometer:micrometer-registry-prometheus'
        
        // Internal libraries
        implementation project(':library-commons')
        implementation project(':utils-lib')
        
        // Lombok
        compileOnly 'org.projectlombok:lombok'
        annotationProcessor 'org.projectlombok:lombok'
        
        // Testing
        testImplementation 'org.springframework.boot:spring-boot-starter-test'
        testImplementation 'org.springframework.security:spring-security-test'
        testImplementation 'org.testcontainers:junit-jupiter'
        testImplementation 'org.testcontainers:postgresql'
    }

    tasks.named('test') {
        useJUnitPlatform()
    }
}
EOF

print_success "Build configuration updated"

# Create project structure documentation
print_step "Creating architecture documentation..."

cat > docs/CORRECT_ARCHITECTURE.md << 'EOF'
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
EOF

print_success "Architecture documentation created"

# Show final structure
print_step "Final correct project structure:"
echo ""
echo "📁 Provenly Employment VC Platform (Correct Architecture)"
echo "├── frontend-apps/              # User-facing applications"
echo "│   ├── issuer-app/            # Organization credential issuance"
echo "│   │   ├── web/               # React/Next.js web app"
echo "│   │   └── mobile/            # React Native mobile app"
echo "│   ├── verifier-app/          # Credential verification"
echo "│   │   ├── web/               # Web app with QR scanning"
echo "│   │   └── mobile/            # Mobile app with camera"
echo "│   └── holder-wallet/         # Individual credential management"
echo "│       ├── web/               # Web wallet interface"
echo "│       └── mobile/            # Mobile wallet (custodial/non-custodial)"
echo "├── backend-libraries/         # Core functionality libraries"
echo "│   ├── auth-lib/              # Authentication & authorization"
echo "│   ├── credentials-lib/       # VC/VP processing"
echo "│   ├── crypto-lib/            # Cryptographic operations"
echo "│   ├── protocols-lib/         # Communication protocols"
echo "│   ├── sdjwts-lib/           # Selective Disclosure JWTs"
echo "│   ├── core-wallet-lib/      # Wallet operations"
echo "│   ├── utils-lib/            # Common utilities"
echo "│   ├── did-lib/              # DID operations"
echo "│   ├── library-commons/      # Shared components"
echo "│   └── openid4vc-lib/       # OpenID4VC/VP"
echo "├── backend-services/         # API services"
echo "│   ├── issuer-api/           # Credential issuance API"
echo "│   ├── verifier-api/         # Credential verification API"
echo "│   ├── wallet-api/           # Wallet management API"
echo "│   ├── web3-login-service/   # Web3 authentication"
echo "│   ├── e2e-test-service/     # End-to-end testing"
echo "│   ├── auth-service/         # Authentication service"
echo "│   ├── did-registry/         # DID registry service"
echo "│   ├── schema-registry/      # Schema management"
echo "│   └── api-gateway/          # API gateway & routing"
echo "├── infra/                    # Infrastructure configs"
echo "├── k8s/                      # Kubernetes manifests"
echo "├── shared/                   # Shared resources"
echo "├── docs/                     # Documentation"
echo "└── scripts/                  # Utility scripts"
echo ""

print_success "Project restructured with correct architecture!"
echo ""
echo "🎯 Key Changes:"
echo "✅ Frontend Apps: Issuer, Verifier, Holder Wallet (Web + Mobile)"
echo "✅ Backend Libraries: Reusable core functionality"
echo "✅ Backend Services: Focused API services"
echo "✅ Clear separation of concerns and responsibilities"
echo ""
echo "🚀 Next Steps:"
echo "1. Run this script: ./scripts/restructure-correct-architecture.sh"
echo "2. Implement backend libraries first"
echo "3. Build API services using the libraries"
echo "4. Develop frontend applications"
echo "5. Set up E2E testing"
echo ""
print_success "Ready for correct architecture development! 🎉"
