#!/bin/bash

# Script to clean up project structure and remove duplicate services

set -e

echo "🧹 Cleaning up Provenly Employment VC Platform project structure..."

# Colors for output
RED='\033[0;31m'
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

print_error() {
    echo -e "${RED}❌${NC} $1"
}

# Backup old services before deletion (just in case)
print_step "Creating backup of old Node.js services..."
if [ -d "provenly-services" ]; then
    mkdir -p backup/old-nodejs-services
    cp -r provenly-services/* backup/old-nodejs-services/ 2>/dev/null || true
    print_success "Backup created in backup/old-nodejs-services/"
fi

# Remove old Node.js services directory
print_step "Removing old Node.js services directory..."
if [ -d "provenly-services" ]; then
    rm -rf provenly-services
    print_success "Removed provenly-services/ directory"
else
    print_warning "provenly-services/ directory not found"
fi

# Clean up old environment files that are Node.js specific
print_step "Cleaning up old environment files..."
OLD_ENV_FILES=(
    ".env.development"
    ".env.production" 
    ".env.provenly"
)

for file in "${OLD_ENV_FILES[@]}"; do
    if [ -f "$file" ]; then
        mv "$file" "backup/$file.old" 2>/dev/null || rm -f "$file"
        print_success "Cleaned up $file"
    fi
done

# Remove old Docker Compose files for Node.js services
print_step "Cleaning up old Docker Compose configurations..."
if [ -f "docker-compose.services.yml" ]; then
    mv docker-compose.services.yml backup/docker-compose.services.yml.old 2>/dev/null || rm -f docker-compose.services.yml
    print_success "Cleaned up docker-compose.services.yml"
fi

# Clean up old Kubernetes manifests for individual Node.js services
print_step "Cleaning up old Kubernetes manifests..."
OLD_K8S_FILES=(
    "k8s/orchestration-service.yaml"
    "k8s/provenly-holder-wallet.yaml"
    "k8s/provenly-issuer-service.yaml"
    "k8s/provenly-verifier-service.yaml"
)

for file in "${OLD_K8S_FILES[@]}"; do
    if [ -f "$file" ]; then
        mv "$file" "backup/$(basename $file).old" 2>/dev/null || rm -f "$file"
        print_success "Cleaned up $file"
    fi
done

# Update .gitignore to reflect new structure
print_step "Updating .gitignore for Java/Node.js structure..."
cat > .gitignore << 'EOF'
# Java/Gradle
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar
!**/src/main/**/build/
!**/src/test/**/build/
*.jar
*.war
*.nar
*.ear
*.zip
*.tar.gz
*.rar
hs_err_pid*

# IDE
.idea/
.vscode/
*.swp
*.swo
*~

# OS
.DS_Store
.DS_Store?
._*
.Spotlight-V100
.Trashes
ehthumbs.db
Thumbs.db

# Node.js (Frontend)
frontend/node_modules/
frontend/.next/
frontend/out/
frontend/dist/
frontend/.env*.local
frontend/npm-debug.log*
frontend/yarn-debug.log*
frontend/yarn-error.log*

# Environment variables
.env
.env.local
.env.development.local
.env.test.local
.env.production.local

# Logs
logs/
*.log

# Database
*.db
*.sqlite

# Docker
.docker/

# Backup directory
backup/

# Temporary files
tmp/
temp/
EOF

print_success "Updated .gitignore"

# Create new environment files for Java backend
print_step "Creating new environment files for Java backend..."

# Development environment
cat > .env.development << 'EOF'
# Development Environment Configuration for Java Backend

# Spring Profiles
SPRING_PROFILES_ACTIVE=development

# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/provenly_dev
DATABASE_USERNAME=provenly_dev
DATABASE_PASSWORD=dev_password

# Redis Configuration
REDIS_URL=redis://localhost:6379

# JWT Configuration
JWT_ISSUER_URI=http://localhost:8080/auth/realms/provenly
JWT_JWK_SET_URI=http://localhost:8080/auth/realms/provenly/protocol/openid-connect/certs

# Hyperledger Besu Configuration
BESU_RPC_URL=http://localhost:8545
BESU_WS_URL=ws://localhost:8546
BESU_NETWORK_ID=1337
BESU_CHAIN_ID=1337
BESU_PRIVATE_KEY=0x8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63

# EBSI Configuration (Pilot Environment)
EBSI_API_BASE_URL=https://api-pilot.ebsi.eu
EBSI_DID_REGISTRY_URL=https://api-pilot.ebsi.eu/did-registry/v4
EBSI_TRUSTED_ISSUERS_REGISTRY_URL=https://api-pilot.ebsi.eu/trusted-issuers-registry/v4
EBSI_TRUSTED_SCHEMAS_REGISTRY_URL=https://api-pilot.ebsi.eu/trusted-schemas-registry/v2
EBSI_CLIENT_ID=dev_ebsi_client_id
EBSI_CLIENT_SECRET=dev_ebsi_client_secret
EBSI_PRIVATE_KEY=dev_ebsi_private_key

# Walt.id Configuration
WALTID_CORE_API_URL=http://localhost:7000
WALTID_SIGNATORY_API_URL=http://localhost:7001
WALTID_CUSTODIAN_API_URL=http://localhost:7002
WALTID_AUDITOR_API_URL=http://localhost:7003
WALTID_API_KEY=dev_waltid_api_key

# Selective Disclosure Configuration
SD_ISSUER_KEY=dev_sd_issuer_key
SD_HOLDER_BINDING_REQUIRED=true
SD_DEFAULT_SUITE=BbsBlsSignature2020

# Wallet Configuration
WALLET_BACKUP_ENABLED=false
WALLET_BACKUP_LOCATION=file:///tmp/wallet-backups
MAX_CREDENTIALS_PER_WALLET=1000
CUSTODIAL_AUTO_BACKUP=true
METADATA_RETENTION_DAYS=365

# Logging Configuration
LOG_LEVEL=DEBUG
SECURITY_LOG_LEVEL=DEBUG
WEB_LOG_LEVEL=DEBUG
SQL_LOG_LEVEL=INFO
SQL_BIND_LOG_LEVEL=WARN

# Development Tools
DEBUG_MODE=true
ENABLE_SWAGGER=true
ENABLE_H2_CONSOLE=false
EOF

# Production environment template
cat > .env.production.template << 'EOF'
# Production Environment Configuration Template
# Copy this file to .env.production and update with actual values

# Spring Profiles
SPRING_PROFILES_ACTIVE=production

# Database Configuration
DATABASE_URL=jdbc:postgresql://postgres-service:5432/provenly_prod
DATABASE_USERNAME=provenly_user
DATABASE_PASSWORD=CHANGE_ME_IN_PRODUCTION

# Redis Configuration
REDIS_URL=redis://redis-service:6379

# JWT Configuration
JWT_ISSUER_URI=https://auth.provenly.io/auth/realms/provenly
JWT_JWK_SET_URI=https://auth.provenly.io/auth/realms/provenly/protocol/openid-connect/certs

# Hyperledger Besu Configuration
BESU_RPC_URL=http://besu-service:8545
BESU_WS_URL=ws://besu-service:8546
BESU_NETWORK_ID=1337
BESU_CHAIN_ID=1337
BESU_PRIVATE_KEY=CHANGE_ME_IN_PRODUCTION

# EBSI Configuration (Production)
EBSI_API_BASE_URL=https://api.ebsi.eu
EBSI_DID_REGISTRY_URL=https://api.ebsi.eu/did-registry/v4
EBSI_TRUSTED_ISSUERS_REGISTRY_URL=https://api.ebsi.eu/trusted-issuers-registry/v4
EBSI_TRUSTED_SCHEMAS_REGISTRY_URL=https://api.ebsi.eu/trusted-schemas-registry/v2
EBSI_CLIENT_ID=CHANGE_ME_IN_PRODUCTION
EBSI_CLIENT_SECRET=CHANGE_ME_IN_PRODUCTION
EBSI_PRIVATE_KEY=CHANGE_ME_IN_PRODUCTION

# Walt.id Configuration
WALTID_CORE_API_URL=http://waltid-core-service:7000
WALTID_SIGNATORY_API_URL=http://waltid-signatory-service:7001
WALTID_CUSTODIAN_API_URL=http://waltid-custodian-service:7002
WALTID_AUDITOR_API_URL=http://waltid-auditor-service:7003
WALTID_API_KEY=CHANGE_ME_IN_PRODUCTION

# Selective Disclosure Configuration
SD_ISSUER_KEY=CHANGE_ME_IN_PRODUCTION
SD_HOLDER_BINDING_REQUIRED=true
SD_DEFAULT_SUITE=BbsBlsSignature2020

# Wallet Configuration
WALLET_BACKUP_ENABLED=true
WALLET_BACKUP_LOCATION=s3://provenly-prod-backups/wallets
MAX_CREDENTIALS_PER_WALLET=1000
CUSTODIAL_AUTO_BACKUP=true
METADATA_RETENTION_DAYS=365

# Logging Configuration
LOG_LEVEL=INFO
SECURITY_LOG_LEVEL=WARN
WEB_LOG_LEVEL=WARN
SQL_LOG_LEVEL=WARN
SQL_BIND_LOG_LEVEL=WARN

# Production Settings
DEBUG_MODE=false
ENABLE_SWAGGER=false
ENABLE_H2_CONSOLE=false
EOF

print_success "Created new environment files"

# Update Docker Compose to reflect new structure
print_step "Updating Docker Compose for Java backend..."
cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  # Database
  postgres:
    image: postgres:15-alpine
    container_name: provenly-postgres
    environment:
      POSTGRES_DB: provenly_dev
      POSTGRES_USER: provenly_dev
      POSTGRES_PASSWORD: dev_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./infra/postgres/init.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - provenly-network

  # Redis
  redis:
    image: redis:7-alpine
    container_name: provenly-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - provenly-network

  # Hyperledger Besu
  besu-node:
    image: hyperledger/besu:latest
    container_name: provenly-besu
    command: [
      "--network=dev",
      "--miner-enabled",
      "--miner-coinbase=0xfe3b557e8fb62b89f4916b721be55ceb828dbd73",
      "--rpc-http-enabled",
      "--rpc-http-host=0.0.0.0",
      "--rpc-http-port=8545",
      "--rpc-http-cors-origins=*",
      "--rpc-ws-enabled",
      "--rpc-ws-host=0.0.0.0",
      "--rpc-ws-port=8546",
      "--data-path=/var/lib/besu"
    ]
    ports:
      - "8545:8545"
      - "8546:8546"
      - "30303:30303"
    volumes:
      - besu_data:/var/lib/besu
    networks:
      - provenly-network

  # Keycloak
  keycloak:
    image: quay.io/keycloak/keycloak:23.0
    container_name: provenly-keycloak
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: dev_admin_password
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/provenly_dev
      KC_DB_USERNAME: provenly_dev
      KC_DB_PASSWORD: dev_password
    ports:
      - "8080:8080"
    depends_on:
      - postgres
    command: start-dev
    networks:
      - provenly-network

  # Walt.id Core
  waltid-core:
    image: waltid/waltid-ssikit:latest
    container_name: provenly-waltid-core
    ports:
      - "7000:7000"
    environment:
      WALTID_DATA_ROOT: /app/data
    volumes:
      - waltid_data:/app/data
    networks:
      - provenly-network

  # Walt.id Signatory
  waltid-signatory:
    image: waltid/waltid-signatory:latest
    container_name: provenly-waltid-signatory
    ports:
      - "7001:7001"
    networks:
      - provenly-network

  # Monitoring - Prometheus
  prometheus:
    image: prom/prometheus:latest
    container_name: provenly-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./infra/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    networks:
      - provenly-network

  # Monitoring - Grafana
  grafana:
    image: grafana/grafana:latest
    container_name: provenly-grafana
    ports:
      - "3001:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
    volumes:
      - grafana_data:/var/lib/grafana
    networks:
      - provenly-network

  # Tracing - Jaeger
  jaeger:
    image: jaegertracing/all-in-one:latest
    container_name: provenly-jaeger
    ports:
      - "16686:16686"
      - "14268:14268"
    environment:
      COLLECTOR_OTLP_ENABLED: true
    networks:
      - provenly-network

volumes:
  postgres_data:
  redis_data:
  besu_data:
  waltid_data:
  prometheus_data:
  grafana_data:

networks:
  provenly-network:
    driver: bridge
EOF

print_success "Updated Docker Compose configuration"

# Show final project structure
print_step "Final project structure:"
echo ""
echo "📁 Provenly Employment VC Platform (Java/Node.js)"
echo "├── backend/                    # Java/Spring Boot microservices"
echo "│   ├── api-gateway/           # Spring Cloud Gateway (Port 8080)"
echo "│   ├── auth-service/          # Authentication & Authorization (Port 8081)"
echo "│   ├── did-registry/          # DID Management with EBSI (Port 8082)"
echo "│   ├── schema-registry/       # Schema Management (Port 8086)"
echo "│   └── application-service/   # 🌟 CORE VC SERVICE (Port 8084)"
echo "│       ├── Issuer Component   # VC Issuance"
echo "│       ├── Verifier Component # VC Verification"
echo "│       ├── Custodial Wallet   # Platform-managed wallets"
echo "│       └── Non-Custodial Wallet # User-controlled wallets"
echo "├── frontend/                  # Next.js React application (Port 3000)"
echo "├── shared/                    # Shared schemas and resources"
echo "├── k8s/                      # Kubernetes deployment manifests"
echo "├── infra/                    # Infrastructure configurations"
echo "├── scripts/                  # Utility and setup scripts"
echo "├── docs/                     # Documentation"
echo "├── build.gradle             # Root Gradle build file"
echo "├── settings.gradle          # Multi-project configuration"
echo "├── docker-compose.yml       # Infrastructure services"
echo "├── .env.development         # Development environment"
echo "└── .env.production.template # Production template"
echo ""

print_success "Project structure cleanup completed!"
echo ""
echo "🎯 Key Changes Made:"
echo "✅ Removed duplicate Node.js services (provenly-services/)"
echo "✅ Consolidated to Java backend + Node.js frontend"
echo "✅ Updated environment files for Java configuration"
echo "✅ Cleaned up old Docker Compose and K8s manifests"
echo "✅ Updated .gitignore for new structure"
echo "✅ Created backup of old files in backup/ directory"
echo ""
echo "🚀 Next Steps:"
echo "1. Review the new structure: ls -la"
echo "2. Start infrastructure: docker-compose up -d"
echo "3. Build Java services: ./gradlew build"
echo "4. Run application service: ./gradlew :application-service:bootRun"
echo "5. Start frontend: cd frontend && npm install && npm run dev"
echo ""
print_success "Ready for Java/Node.js development! 🎉"
