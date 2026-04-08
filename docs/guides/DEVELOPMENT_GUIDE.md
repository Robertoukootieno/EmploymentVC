# Provenly Employment VC Platform - Development Guide

**Quick reference for developers starting work on this project**

---

## 🚀 **Quick Start**

### **1. Clone and Setup**
```bash
git clone https://github.com/Robertoukootieno/EmploymentVC.git
cd EmploymentVC
```

### **2. Start Infrastructure Services**
```bash
# Start all infrastructure (PostgreSQL, Redis, Besu, Keycloak, Walt.id, etc.)
COMPOSE_PROJECT_NAME=employmentvc docker compose up -d

# Verify services are running
COMPOSE_PROJECT_NAME=employmentvc docker compose ps
```

**Services Started**:
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- Hyperledger Besu: `localhost:8545`
- Keycloak: `localhost:8080/auth`
- Walt.id Core: `localhost:7000`
- Walt.id Signatory: `localhost:7001`
- Prometheus: `localhost:9090`
- Grafana: `localhost:3001`
- Jaeger: `localhost:16686`

---

## 🔧 **Backend Development (Java/Spring Boot)**

### **Prerequisites**:
- Java 17+
- Gradle 8.5+

### **Build All Services**:
```bash
./gradlew build
```

### **Run Specific Service**:
```bash
# Auth Service
./gradlew :auth-service:bootRun

# Wallet API (when implemented)
./gradlew :wallet-api:bootRun

# Issuer API (when implemented)
./gradlew :issuer-api:bootRun
```

### **Run Tests**:
```bash
# All tests
./gradlew test

# Specific service
./gradlew :auth-service:test
```

### **Code Quality Checks**:
```bash
# Checkstyle
./gradlew checkstyleMain

# SpotBugs
./gradlew spotbugsMain

# All checks
./gradlew check
```

### **Project Structure**:
```
backend-services/
├── auth-service/          # Port 8081
├── wallet-api/            # Port 8084 (to implement)
├── issuer-api/            # Port 8085 (to implement)
├── verifier-api/          # Port 8086 (to implement)
├── did-registry/          # Port 8082 (to implement)
├── schema-registry/       # Port 8086 (to implement)
└── api-gateway/           # Port 8080 (to implement)

backend-libraries/
├── library-commons/       # Base classes
├── crypto-lib/           # Cryptography
├── credentials-lib/      # VC/VP processing
├── did-lib/             # DID operations
└── [other libraries...]
```

---

## 📱 **Mobile Development (React Native)**

### **Prerequisites**:
- Node.js 18+
- React Native CLI
- Xcode (for iOS)
- Android Studio (for Android)

### **Setup Mobile Wallet**:
```bash
cd employmentVC-Applications/holder-wallet/mobile

# Install dependencies
npm install

# iOS setup
cd ios && pod install && cd ..

# Start Metro bundler
npm start
```

### **Run on Device/Simulator**:
```bash
# iOS
npm run ios

# Android
npm run android
```

### **Development Commands**:
```bash
# Type checking
npm run type-check

# Linting
npm run lint
npm run lint:fix

# Tests
npm test

# Clean project
npm run clean
```

### **Mobile App Structure**:
```
src/
├── navigation/          # App navigation
├── screens/            # UI screens
├── services/           # API clients and business logic
├── store/             # Redux state management
├── types/             # TypeScript types
└── polyfills.ts       # Crypto polyfills
```

---

## 🌐 **Web Development (Next.js)**

### **Prerequisites**:
- Node.js 18+
- npm or yarn

### **Setup (When Implemented)**:
```bash
cd employmentVC-Applications/holder-wallet/web

# Install dependencies
npm install

# Development server
npm run dev

# Build for production
npm run build

# Start production server
npm start
```

---

## 🔑 **Environment Variables**

### **Backend Services** (`.env` or `application.yml`):
```yaml
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/provenly_dev
spring.datasource.username=provenly_dev
spring.datasource.password=dev_password

# Redis
spring.redis.host=localhost
spring.redis.port=6379

# Keycloak
keycloak.auth-server-url=http://localhost:8080/auth
keycloak.realm=provenly
keycloak.resource=provenly-backend

# Walt.id
waltid.core-api-url=http://localhost:7000
waltid.signatory-api-url=http://localhost:7001

# EBSI
ebsi.api-base-url=https://api-pilot.ebsi.eu
ebsi.did-registry-url=https://api-pilot.ebsi.eu/did-registry/v4

# Blockchain
blockchain.rpc-url=http://localhost:8545
```

### **Mobile App** (`employmentVC-Applications/holder-wallet/mobile/.env`):
```bash
# API Configuration
API_BASE_URL=http://localhost:8080
API_TIMEOUT=30000

# Environment
NODE_ENV=development
```

---

## 🧪 **Testing**

### **Backend Tests**:
```bash
# Unit tests
./gradlew test

# Integration tests
./gradlew integrationTest

# With coverage
./gradlew test jacocoTestReport
```

### **Mobile Tests**:
```bash
cd employmentVC-Applications/holder-wallet/mobile

# Unit tests
npm test

# Watch mode
npm test -- --watch

# Coverage
npm test -- --coverage
```

---

## 🐛 **Debugging**

### **Backend**:
```bash
# Run with debug enabled
./gradlew :auth-service:bootRun --debug-jvm

# Connect debugger to port 5005
```

### **Mobile**:
```bash
# Enable React Native debugger
# Shake device or press Cmd+D (iOS) / Cmd+M (Android)
# Select "Debug"

# View logs
npx react-native log-ios
npx react-native log-android
```

---

## 📊 **Monitoring & Observability**

### **Access Monitoring Tools**:
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3001 (admin/admin)
- **Jaeger**: http://localhost:16686

### **Health Checks**:
```bash
# Auth Service
curl http://localhost:8081/actuator/health

# API Gateway (when running)
curl http://localhost:8080/actuator/health
```

---

## 🔒 **Security**

### **Secrets Management**:
- **Never commit secrets** to git
- Use environment variables
- Use Azure Key Vault for production
- Use `.env` files for local development (add to `.gitignore`)

### **Pre-commit Hooks** (Recommended):
```bash
# Install git-secrets
brew install git-secrets  # macOS
# or
apt-get install git-secrets  # Linux

# Set up hooks
git secrets --install
git secrets --register-aws
```

---

## 📚 **Additional Resources**

- **Architecture**: See `docs/CORRECT_ARCHITECTURE.md`
- **API Documentation**: See `docs/API.md`
- **Project Status**: See `PROJECT_STATUS_REPORT.md`
- **Next Steps**: See `NEXT_STEPS.md`
- **Deployment**: See `docs/DEPLOYMENT.md`

---

## 🆘 **Common Issues**

### **Issue**: Docker services won't start
**Solution**: 
```bash
COMPOSE_PROJECT_NAME=employmentvc docker compose down -v
COMPOSE_PROJECT_NAME=employmentvc docker compose up -d
```

### **Issue**: Gradle build fails
**Solution**:
```bash
./gradlew clean build --refresh-dependencies
```

### **Issue**: Mobile app won't build
**Solution**:
```bash
cd employmentVC-Applications/holder-wallet/mobile
npm run clean
rm -rf node_modules
npm install
cd ios && pod install && cd ..
```

---

**Happy Coding! 🚀**

