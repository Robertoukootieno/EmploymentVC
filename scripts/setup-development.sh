#!/bin/bash

# Complete development environment setup script

set -e

echo "🚀 Setting up Provenly Employment VC Platform for development..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

# Check prerequisites
print_step "Checking prerequisites..."

if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    print_error "Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

if ! command -v node &> /dev/null; then
    print_warning "Node.js is not installed. You'll need it for local development."
fi

print_success "Prerequisites check completed"

# Set up environment variables
print_step "Setting up environment variables..."

if [ ! -f ".env" ]; then
    cp .env.development .env
    print_success "Created .env file from .env.development"
else
    print_warning ".env file already exists, skipping..."
fi

# Create necessary directories
print_step "Creating necessary directories..."
mkdir -p logs data/postgres data/redis data/besu
print_success "Directories created"

# Start infrastructure services
print_step "Starting infrastructure services..."

echo "Starting PostgreSQL, Redis, and Hyperledger Besu..."
docker-compose up -d postgres redis besu-node

echo "Waiting for services to be ready..."
sleep 10

# Check if services are running
if docker-compose ps | grep -q "postgres.*Up"; then
    print_success "PostgreSQL is running"
else
    print_error "PostgreSQL failed to start"
fi

if docker-compose ps | grep -q "redis.*Up"; then
    print_success "Redis is running"
else
    print_error "Redis failed to start"
fi

if docker-compose ps | grep -q "besu-node.*Up"; then
    print_success "Hyperledger Besu is running"
else
    print_error "Hyperledger Besu failed to start"
fi

# Start walt.id services
print_step "Starting walt.id services..."
docker-compose up -d waltid-core waltid-signatory
sleep 5

if docker-compose ps | grep -q "waltid-core.*Up"; then
    print_success "Walt.id Core is running"
else
    print_warning "Walt.id Core may not be running properly"
fi

# Start Keycloak
print_step "Starting Keycloak..."
docker-compose up -d keycloak
sleep 10

if docker-compose ps | grep -q "keycloak.*Up"; then
    print_success "Keycloak is running"
else
    print_warning "Keycloak may not be running properly"
fi

# Start monitoring services
print_step "Starting monitoring services..."
docker-compose up -d prometheus grafana jaeger
sleep 5

print_success "Monitoring services started"

# Install dependencies for microservices
print_step "Installing dependencies for microservices..."
if [ -f "scripts/install-dependencies.sh" ]; then
    chmod +x scripts/install-dependencies.sh
    ./scripts/install-dependencies.sh
else
    print_warning "install-dependencies.sh script not found, skipping..."
fi

# Start microservices
print_step "Starting microservices..."
docker-compose -f docker-compose.services.yml up -d

sleep 10

print_step "Checking service health..."

# Check API Gateway
if curl -s http://localhost:3000/health > /dev/null; then
    print_success "API Gateway is healthy"
else
    print_warning "API Gateway may not be ready yet"
fi

# Print access information
echo ""
echo "🎉 Development environment setup completed!"
echo ""
echo "📋 Service Access Information:"
echo "================================"
echo "🌐 API Gateway:          http://localhost:3000"
echo "📚 API Documentation:    http://localhost:3000/api-docs"
echo "🔐 Keycloak Admin:       http://localhost:8080 (admin/dev_admin_password)"
echo "📊 Grafana:              http://localhost:3001 (admin/admin)"
echo "📈 Prometheus:           http://localhost:9090"
echo "🔍 Jaeger:               http://localhost:16686"
echo ""
echo "🔧 Development Commands:"
echo "========================"
echo "📦 Install dependencies: ./scripts/install-dependencies.sh"
echo "🧪 Run tests:           ./scripts/run-tests.sh"
echo "🔍 Check logs:          docker-compose logs -f [service-name]"
echo "🛑 Stop services:       docker-compose down"
echo "🗑️  Clean up:            docker-compose down -v --remove-orphans"
echo ""
echo "📖 Next Steps:"
echo "==============="
echo "1. Check that all services are running: docker-compose ps"
echo "2. View API documentation at http://localhost:3000/api-docs"
echo "3. Configure Keycloak realm and clients"
echo "4. Start developing your features!"
echo ""
print_success "Happy coding! 🚀"
