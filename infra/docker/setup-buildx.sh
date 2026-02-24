#!/bin/bash

# Setup Docker Buildx for multi-architecture builds
# Usage: ./setup-buildx.sh [builder-name]

set -e

BUILDER_NAME=${1:-provenly-builder}

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

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker first."
    exit 1
fi

print_step "Setting up Docker Buildx for multi-architecture builds..."
echo ""

# Check if buildx is available
if ! docker buildx version > /dev/null 2>&1; then
    print_error "Docker Buildx is not available. Please upgrade Docker."
    exit 1
fi

print_success "Docker Buildx is available"
echo ""

# Check if builder already exists
if docker buildx inspect "$BUILDER_NAME" > /dev/null 2>&1; then
    print_warning "Builder '$BUILDER_NAME' already exists"
    read -p "Do you want to remove and recreate it? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_step "Removing existing builder..."
        docker buildx rm "$BUILDER_NAME"
        print_success "Removed existing builder"
    else
        print_step "Using existing builder"
        docker buildx use "$BUILDER_NAME"
        docker buildx inspect --bootstrap
        exit 0
    fi
fi

# Create new builder
print_step "Creating new builder: $BUILDER_NAME"
docker buildx create \
    --name "$BUILDER_NAME" \
    --driver docker-container \
    --bootstrap \
    --use \
    --config "$(dirname "$0")/buildx.toml"

print_success "Builder created successfully"
echo ""

# Inspect the builder
print_step "Builder configuration:"
docker buildx inspect --bootstrap

echo ""
print_success "Docker Buildx is now configured!"
echo ""

print_step "Supported platforms:"
docker buildx inspect | grep Platforms

echo ""
print_step "Quick commands:"
echo "  • List builders:     docker buildx ls"
echo "  • Use this builder:  docker buildx use $BUILDER_NAME"
echo "  • Build multi-arch:  docker buildx build --platform linux/amd64,linux/arm64 -t myimage:latest ."
echo "  • Remove builder:    docker buildx rm $BUILDER_NAME"
echo ""

print_step "Testing builder..."
if docker buildx build --platform linux/amd64,linux/arm64 --help > /dev/null 2>&1; then
    print_success "Builder is ready for multi-architecture builds!"
else
    print_warning "Builder created but may need additional setup"
fi
