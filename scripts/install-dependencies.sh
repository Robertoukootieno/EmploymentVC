#!/bin/bash

# Script to install dependencies for all microservices

set -e

echo "📦 Installing dependencies for all Provenly microservices..."

# Define services
SERVICES=(
    "api-gateway"
    "auth-service"
    "did-registry"
    "schema-registry"
    "provenly-issuer-service"
    "provenly-verifier-service"
    "provenly-holder-wallet"
    "orchestration-service"
)

# Function to install dependencies for a service
install_service_deps() {
    local service=$1
    
    echo "📦 Installing dependencies for $service..."
    
    if [ -f "provenly-services/$service/package.json" ]; then
        cd "provenly-services/$service"
        
        # Check if package-lock.json exists
        if [ -f "package-lock.json" ]; then
            npm ci
        else
            npm install
        fi
        
        cd ../..
        echo "✅ Dependencies installed for $service"
    else
        echo "⚠️  No package.json found for $service, skipping..."
    fi
}

# Install dependencies for all services
for service in "${SERVICES[@]}"; do
    install_service_deps "$service"
done

echo "🎉 All dependencies installed successfully!"
echo ""
echo "Next steps:"
echo "1. Configure environment variables"
echo "2. Start infrastructure services: docker-compose up -d postgres redis besu-node"
echo "3. Start microservices: docker-compose -f docker-compose.services.yml up -d"
