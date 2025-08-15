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
| **Application Service** | **8084** | **🌟 Core VC Operations** |
| | | **• Issuer Component** |
| | | **• Verifier Component** |
| | | **• Custodial Wallet** |
| | | **• Non-Custodial Wallet** |

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
