#!/bin/bash

# Script to restructure project for Java/Gradle backend and Node.js frontend

set -e

echo "🔄 Restructuring Provenly Employment VC Platform for Java/Gradle Backend..."

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

# Create new directory structure
print_step "Creating new project structure..."

# Backend services structure
mkdir -p backend/{api-gateway,auth-service,did-registry,credential-schema-registry,application-service}

# Frontend structure
mkdir -p frontend/{src,public,components,pages,hooks,utils,types,styles}

# Shared resources
mkdir -p shared/{schemas,contracts,docs}

# Infrastructure updates
mkdir -p infra/{java,docker,k8s}

print_success "Directory structure created"

# Create root build files
print_step "Creating root build configuration..."

# Root settings.gradle
cat > settings.gradle << 'EOF'
rootProject.name = 'provenly-employment-vc'

include 'api-gateway'
include 'auth-service'
include 'did-registry'
include 'credential-schema-registry'
include 'application-service'

project(':api-gateway').projectDir = file('backend/api-gateway')
project(':auth-service').projectDir = file('backend/auth-service')
project(':did-registry').projectDir = file('backend/did-registry')
project(':credential-schema-registry').projectDir = file('backend/credential-schema-registry')
project(':application-service').projectDir = file('backend/application-service')
EOF

# Root build.gradle
cat > build.gradle << 'EOF'
plugins {
    id 'java'
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
    }
}

subprojects {
    apply plugin: 'java'
    apply plugin: 'org.springframework.boot'
    apply plugin: 'io.spring.dependency-management'
    apply plugin: 'checkstyle'
    apply plugin: 'com.github.spotbugs'

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    configurations {
        compileOnly {
            extendsFrom annotationProcessor
        }
    }

    dependencies {
        // Spring Boot Starters
        implementation 'org.springframework.boot:spring-boot-starter'
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
        
        // JSON Processing
        implementation 'com.fasterxml.jackson.core:jackson-databind'
        implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'
        
        // Validation
        implementation 'org.springframework.boot:spring-boot-starter-validation'
        
        // Lombok
        compileOnly 'org.projectlombok:lombok'
        annotationProcessor 'org.projectlombok:lombok'
        
        // Testing
        testImplementation 'org.springframework.boot:spring-boot-starter-test'
        testImplementation 'org.springframework.security:spring-security-test'
        testImplementation 'org.testcontainers:junit-jupiter'
        testImplementation 'org.testcontainers:postgresql'
        testImplementation 'org.testcontainers:testcontainers'
    }

    tasks.named('test') {
        useJUnitPlatform()
    }

    checkstyle {
        toolVersion = '10.12.4'
        configFile = file("${rootProject.projectDir}/config/checkstyle/checkstyle.xml")
    }

    spotbugs {
        toolVersion = '4.8.2'
    }
}
EOF

# Gradle wrapper properties
mkdir -p gradle/wrapper
cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

print_success "Root build configuration created"

# Create frontend package.json
print_step "Creating frontend configuration..."

cat > frontend/package.json << 'EOF'
{
  "name": "@provenly/frontend",
  "version": "1.0.0",
  "description": "Provenly Employment VC Platform - Frontend Application",
  "private": true,
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start",
    "lint": "next lint",
    "test": "jest",
    "test:watch": "jest --watch",
    "test:coverage": "jest --coverage",
    "type-check": "tsc --noEmit"
  },
  "dependencies": {
    "next": "^14.0.4",
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "typescript": "^5.3.3",
    "@types/react": "^18.2.45",
    "@types/react-dom": "^18.2.18",
    "@types/node": "^20.10.4",
    "tailwindcss": "^3.3.6",
    "autoprefixer": "^10.4.16",
    "postcss": "^8.4.32",
    "ethers": "^6.8.1",
    "wagmi": "^1.4.12",
    "@rainbow-me/rainbowkit": "^1.3.5",
    "viem": "^1.19.11",
    "axios": "^1.6.2",
    "react-query": "^3.39.3",
    "zustand": "^4.4.7",
    "react-hook-form": "^7.48.2",
    "@hookform/resolvers": "^3.3.2",
    "zod": "^3.22.4",
    "lucide-react": "^0.294.0",
    "clsx": "^2.0.0",
    "tailwind-merge": "^2.1.0"
  },
  "devDependencies": {
    "eslint": "^8.55.0",
    "eslint-config-next": "^14.0.4",
    "@typescript-eslint/eslint-plugin": "^6.13.2",
    "@typescript-eslint/parser": "^6.13.2",
    "prettier": "^3.1.1",
    "prettier-plugin-tailwindcss": "^0.5.7",
    "jest": "^29.7.0",
    "jest-environment-jsdom": "^29.7.0",
    "@testing-library/react": "^14.1.2",
    "@testing-library/jest-dom": "^6.1.5",
    "@testing-library/user-event": "^14.5.1"
  },
  "engines": {
    "node": ">=18.0.0",
    "npm": ">=8.0.0"
  }
}
EOF

# Create frontend Dockerfile
cat > frontend/Dockerfile << 'EOF'
# Multi-stage build for Next.js application
FROM node:18-alpine AS base

# Install dependencies only when needed
FROM base AS deps
RUN apk add --no-cache libc6-compat
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci --only=production

# Rebuild the source code only when needed
FROM base AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .

ENV NEXT_TELEMETRY_DISABLED 1

RUN npm run build

# Production image, copy all the files and run next
FROM base AS runner
WORKDIR /app

ENV NODE_ENV production
ENV NEXT_TELEMETRY_DISABLED 1

RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

COPY --from=builder /app/public ./public

# Set the correct permission for prerender cache
RUN mkdir .next
RUN chown nextjs:nodejs .next

# Automatically leverage output traces to reduce image size
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3000

ENV PORT 3000
ENV HOSTNAME "0.0.0.0"

CMD ["node", "server.js"]
EOF

print_success "Frontend configuration created"

# Create shared resources
print_step "Creating shared resources..."

# Shared schemas directory
mkdir -p shared/schemas/credentials
mkdir -p shared/schemas/contexts

# Create employment credential schema
cat > shared/schemas/credentials/employment-credential.json << 'EOF'
{
  "@context": [
    "https://www.w3.org/2018/credentials/v1",
    "https://provenly.io/contexts/employment/v1"
  ],
  "type": "object",
  "properties": {
    "@context": {
      "type": "array",
      "items": {
        "type": "string"
      }
    },
    "type": {
      "type": "array",
      "items": {
        "type": "string"
      },
      "contains": {
        "const": "EmploymentCredential"
      }
    },
    "credentialSubject": {
      "type": "object",
      "properties": {
        "id": {
          "type": "string",
          "format": "uri"
        },
        "employeeId": {
          "type": "string"
        },
        "position": {
          "type": "string"
        },
        "department": {
          "type": "string"
        },
        "startDate": {
          "type": "string",
          "format": "date"
        },
        "endDate": {
          "type": "string",
          "format": "date"
        },
        "salary": {
          "type": "number",
          "minimum": 0
        },
        "employer": {
          "type": "object",
          "properties": {
            "name": {
              "type": "string"
            },
            "did": {
              "type": "string"
            }
          },
          "required": ["name", "did"]
        }
      },
      "required": ["id", "employeeId", "position", "startDate", "employer"]
    }
  },
  "required": ["@context", "type", "credentialSubject"]
}
EOF

print_success "Shared resources created"

print_step "Creating documentation updates..."

# Update main README
cat > README.md << 'EOF'
# Provenly Employment VC Platform

A comprehensive Verifiable Credentials platform for employment verification built with **Java/Spring Boot backend** and **Next.js frontend**, featuring Hyperledger Besu, EBSI APIs for DIDs, walt.id VCs, selective disclosure, and JSON-LD.

## 🏗️ Architecture

### Backend (Java/Spring Boot)
- **API Gateway** - Central routing, authentication, and rate limiting
- **Authentication Service** - Multi-method auth (Traditional, Web3, DID)
- **DID Registry** - EBSI integration for DID management
- **Credential Schema Registry** - JSON-LD schema management
- **Application Service** - Core VC operations (Issuer, Verifier, Wallets)

### Frontend (Next.js/React)
- **Web Application** - User interface for credential management
- **Web3 Integration** - Wallet connectivity and blockchain interactions
- **Responsive Design** - Mobile-first approach with Tailwind CSS

### Infrastructure
- **Hyperledger Besu** - Blockchain network
- **PostgreSQL** - Primary database
- **Redis** - Caching and sessions
- **Keycloak** - Identity management

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- Docker & Docker Compose
- Gradle 8.5+

### Development Setup

1. **Clone and setup**
   ```bash
   git clone <repository-url>
   cd EmploymentVC
   ```

2. **Start infrastructure**
   ```bash
   docker-compose up -d postgres redis besu-node keycloak
   ```

3. **Build and run backend services**
   ```bash
   ./gradlew build
   ./gradlew bootRun --parallel
   ```

4. **Start frontend**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

5. **Access the platform**
   - Frontend: http://localhost:3000
   - API Gateway: http://localhost:8080
   - API Documentation: http://localhost:8080/swagger-ui.html

## 📊 Service Ports

| Service | Port | Description |
|---------|------|-------------|
| Frontend | 3000 | Next.js web application |
| API Gateway | 8080 | Main API entry point |
| Auth Service | 8081 | Authentication service |
| DID Registry | 8082 | DID management |
| Schema Registry | 8083 | Credential schemas |
| Application Service | 8084 | Core VC operations |

## 🔧 Development

### Backend Development
```bash
# Run specific service
./gradlew :application-service:bootRun

# Run tests
./gradlew test

# Code quality checks
./gradlew checkstyleMain spotbugsMain
```

### Frontend Development
```bash
cd frontend

# Development server
npm run dev

# Build for production
npm run build

# Run tests
npm test
```

## 📚 Documentation

- [API Documentation](docs/API.md)
- [Architecture Guide](docs/ARCHITECTURE.md)
- [Deployment Guide](docs/DEPLOYMENT.md)
- [Web3 Integration](docs/WEB3_INTEGRATION.md)

## 🔐 Features

- **Multi-Method Authentication** - Traditional, Web3 wallets, DID-based
- **EBSI Integration** - European Blockchain Services Infrastructure
- **Selective Disclosure** - Privacy-preserving credential sharing
- **Custodial & Non-Custodial Wallets** - Flexible credential storage
- **JSON-LD Support** - Semantic interoperability
- **Enterprise Security** - Production-ready security features

## 🚀 Deployment

### Docker Compose (Development)
```bash
docker-compose up -d
```

### Kubernetes (Production)
```bash
kubectl apply -f k8s/
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
EOF

print_success "Documentation updated"

echo ""
echo "🎉 Project restructuring completed!"
echo ""
echo "📁 New Structure:"
echo "├── backend/                    # Java/Spring Boot services"
echo "│   ├── api-gateway/"
echo "│   ├── auth-service/"
echo "│   ├── did-registry/"
echo "│   ├── credential-schema-registry/"
echo "│   └── application-service/"
echo "├── frontend/                   # Next.js application"
echo "├── shared/                     # Shared schemas and resources"
echo "├── infra/                      # Infrastructure configurations"
echo "├── build.gradle               # Root build configuration"
echo "└── settings.gradle            # Multi-project setup"
echo ""
echo "🔧 Next Steps:"
echo "1. Run this script: ./scripts/restructure-project.sh"
echo "2. Create Java service implementations"
echo "3. Implement frontend application"
echo "4. Update Docker configurations"
echo "5. Test the complete stack"
echo ""
echo "🚀 Ready for Java/Node.js development!"
