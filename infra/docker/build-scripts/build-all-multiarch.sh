#!/bin/bash

# Build all microservices for multiple architectures and push to registry
# Usage: ./build-all-multiarch.sh [registry] [tag] [--push]

set -e

REGISTRY=${1:-ghcr.io/robertoukootieno/employmentvc}
TAG=${2:-latest}
PUSH=false
PLATFORMS="linux/amd64,linux/arm64"
BUILDER_NAME="provenly-builder"

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
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

print_warning() {
    echo -e "${YELLOW}⚠️${NC} $1"
}

# Check for --push flag
for arg in "$@"; do
    if [ "$arg" = "--push" ]; then
        PUSH=true
    fi
done

# Validate buildx is available
if ! docker buildx version > /dev/null 2>&1; then
    print_error "Docker Buildx is not available"
    exit 1
fi

# Check if builder exists, create if not
if ! docker buildx inspect "$BUILDER_NAME" > /dev/null 2>&1; then
    print_warning "Builder '$BUILDER_NAME' not found. Creating..."
    if [ -f "$(dirname "$0")/../setup-buildx.sh" ]; then
        bash "$(dirname "$0")/../setup-buildx.sh" "$BUILDER_NAME"
    else
        print_error "setup-buildx.sh not found. Please run it first."
        exit 1
    fi
fi

# Use the builder
docker buildx use "$BUILDER_NAME"

print_step "Building all microservices for multiple architectures..."
echo "Registry: $REGISTRY"
echo "Tag: $TAG"
echo "Platforms: $PLATFORMS"
echo "Push: $PUSH"
echo ""

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

FAILED_BUILDS=()
SUCCESSFUL_BUILDS=()

for service in "${SERVICES[@]}"; do
    print_step "Building $service for $PLATFORMS..."
    
    BUILD_CMD="docker buildx build \
        --platform $PLATFORMS \
        -f backend-services/$service/Dockerfile \
        -t $REGISTRY/$service:$TAG \
        --cache-from type=registry,ref=$REGISTRY/$service:buildcache \
        --cache-to type=registry,ref=$REGISTRY/$service:buildcache,mode=max"
    
    if [ "$PUSH" = true ]; then
        BUILD_CMD="$BUILD_CMD --push"
    else
        BUILD_CMD="$BUILD_CMD --load"
        print_warning "Not pushing (use --push flag to push to registry)"
    fi
    
    BUILD_CMD="$BUILD_CMD ."
    
    if eval "$BUILD_CMD"; then
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

print_success "All services built successfully for multiple architectures!"

if [ "$PUSH" = true ]; then
    print_success "Images pushed to $REGISTRY"
else
    print_warning "Images built locally. To push: ./build-all-multiarch.sh $REGISTRY $TAG --push"
fi
