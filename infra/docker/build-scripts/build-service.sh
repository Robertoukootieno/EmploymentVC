#!/bin/bash

# Build single microservice with options
# Usage: ./build-service.sh <service-name> [registry] [tag] [--push] [--platform linux/amd64,linux/arm64]

set -e

SERVICE=$1
REGISTRY=${2:-ghcr.io/robertoukootieno/employmentvc}
TAG=${3:-latest}
PUSH=false
PLATFORM="linux/amd64"
BUILDKIT=${BUILDKIT:-1}

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

# Validate service name
if [ -z "$SERVICE" ]; then
    print_error "Service name is required"
    echo "Usage: $0 <service-name> [registry] [tag] [--push] [--platform]"
    echo ""
    echo "Available services:"
    echo "  • api-gateway"
    echo "  • auth-service"
    echo "  • did-registry"
    echo "  • schema-registry"
    echo "  • issuer-api"
    echo "  • verifier-api"
    echo "  • custodial-wallet"
    echo "  • noncustodial-gateway"
    echo "  • notification-service"
    echo "  • workflow-service"
    exit 1
fi

if [ ! -f "backend-services/$SERVICE/Dockerfile" ]; then
    print_error "Dockerfile not found for service: $SERVICE"
    exit 1
fi

# Parse additional arguments
for arg in "${@:4}"; do
    case "$arg" in
        --push)
            PUSH=true
            ;;
        --platform)
            # Next arg should be platform
            ;;
        linux/*)
            PLATFORM="$arg"
            ;;
    esac
done

print_step "Building service: $SERVICE"
echo "Registry: $REGISTRY"
echo "Tag: $TAG"
echo "Platform: $PLATFORM"
echo "Push: $PUSH"
echo ""

# Build command
BUILD_CMD="DOCKER_BUILDKIT=$BUILDKIT docker build \
    -f backend-services/$SERVICE/Dockerfile \
    -t $REGISTRY/$SERVICE:$TAG"

# Add platform if not default
if [ "$PLATFORM" != "linux/amd64" ]; then
    if [[ $PLATFORM == *","* ]]; then
        # Multi-platform requires buildx
        BUILD_CMD="docker buildx build \
            -f backend-services/$SERVICE/Dockerfile \
            --platform $PLATFORM \
            -t $REGISTRY/$SERVICE:$TAG"
        if [ "$PUSH" = true ]; then
            BUILD_CMD="$BUILD_CMD --push"
        else
            BUILD_CMD="$BUILD_CMD --load"
        fi
    fi
fi

BUILD_CMD="$BUILD_CMD ."

print_step "Executing build..."
echo "Command: $BUILD_CMD"
echo ""

if eval "$BUILD_CMD"; then
    print_success "Build completed successfully!"
    echo ""
    echo "Built image: $REGISTRY/$SERVICE:$TAG"
    
    if [ "$PUSH" = true ]; then
        print_success "Image pushed to registry"
    else
        print_warning "Image built locally. To push: docker push $REGISTRY/$SERVICE:$TAG"
    fi
    
    print_step "Quick commands:"
    echo "  • View image:    docker images | grep $SERVICE"
    echo "  • Push image:    docker push $REGISTRY/$SERVICE:$TAG"
    echo "  • Run container: docker run -it $REGISTRY/$SERVICE:$TAG"
    echo "  • Tag for other: docker tag $REGISTRY/$SERVICE:$TAG other-registry/$SERVICE:$TAG"
else
    print_error "Build failed"
    exit 1
fi
