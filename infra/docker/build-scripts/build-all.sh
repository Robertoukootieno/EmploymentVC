#!/bin/bash

# Build all microservices
# Usage: ./build-all.sh [registry] [tag]

set -e

REGISTRY=${1:-ghcr.io/robertoukootieno/employmentvc}
TAG=${2:-latest}
BUILDKIT=${BUILDKIT:-1}

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

print_step() {
    echo -e "${BLUE}==>${NC} $1"
}

print_success() {
    echo -e "${GREEN}✅${NC} $1"
}

print_error() {
    echo -e "${RED}❌${NC} $1"
}

# Services to build
SERVICES=(
    "api-gateway"
    "auth-service"
    "did-registry"
    "schema-registry"
    "issuer-api"
    "verifier-api"
    "custodial-wallet"
    "noncustodial-gateway"
    "notification-service"
    "workflow-service"
)

print_step "Building all microservices..."
echo "Registry: $REGISTRY"
echo "Tag: $TAG"
echo "BuildKit: $BUILDKIT"
echo ""

FAILED_BUILDS=()
SUCCESSFUL_BUILDS=()

for service in "${SERVICES[@]}"; do
    print_step "Building $service..."
    
    if DOCKER_BUILDKIT=$BUILDKIT docker build \
        -f "backend-services/$service/Dockerfile" \
        -t "$REGISTRY/$service:$TAG" \
        . 2>&1 | tail -20; then
        print_success "$service built successfully"
        SUCCESSFUL_BUILDS+=("$service")
    else
        print_error "Failed to build $service"
        FAILED_BUILDS+=("$service")
    fi
    echo ""
done

# Summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
print_step "Build Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

if [ ${#SUCCESSFUL_BUILDS[@]} -gt 0 ]; then
    print_success "Successful builds: ${#SUCCESSFUL_BUILDS[@]}"
    for service in "${SUCCESSFUL_BUILDS[@]}"; do
        echo "  ✓ $service"
    done
    echo ""
fi

if [ ${#FAILED_BUILDS[@]} -gt 0 ]; then
    print_error "Failed builds: ${#FAILED_BUILDS[@]}"
    for service in "${FAILED_BUILDS[@]}"; do
        echo "  ✗ $service"
    done
    echo ""
    exit 1
fi

print_success "All services built successfully!"
echo ""
echo "Next steps:"
echo "  • Tag images: docker tag $REGISTRY/<service>:$TAG <new-registry>/<service>:$TAG"
echo "  • Push images: docker push $REGISTRY/<service>:$TAG"
echo "  • Run services: docker-compose up -d"
