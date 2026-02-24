#!/bin/bash

# Setup Docker Registry Authentication (GitHub Container Registry)
# Usage: ./registry-setup.sh [username] [token]

set -e

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

USERNAME=${1:-}
TOKEN=${2:-}

if [ -z "$USERNAME" ] || [ -z "$TOKEN" ]; then
    print_step "GitHub Container Registry Authentication Setup"
    echo ""
    echo "Prerequisites:"
    echo "  1. GitHub Personal Access Token with 'read:packages' and 'write:packages'"
    echo "     Link: https://github.com/settings/tokens"
    echo "  2. Username (GitHub username)"
    echo ""
    
    read -p "Enter GitHub username: " USERNAME
    read -sp "Enter Personal Access Token: " TOKEN
    echo ""
fi

if [ -z "$USERNAME" ] || [ -z "$TOKEN" ]; then
    print_error "Username and token are required"
    exit 1
fi

print_step "Logging into GitHub Container Registry..."

if echo "$TOKEN" | docker login ghcr.io -u "$USERNAME" --password-stdin; then
    print_success "Successfully logged into ghcr.io"
    echo ""
    
    print_step "Verification"
    echo ""
    
    # Check config
    if grep -q "ghcr.io" ~/.docker/config.json; then
        print_success "Credentials saved in ~/.docker/config.json"
    else
        print_warning "Could not verify credentials"
    fi
    
    echo ""
    print_step "Next steps:"
    echo "  1. Test push: docker tag test-image ghcr.io/$USERNAME/test-image:latest"
    echo "  2. Push:      docker push ghcr.io/$USERNAME/test-image:latest"
    echo "  3. View at:   https://github.com/$USERNAME?tab=packages"
    echo ""
    print_warning "Keep your token secret! Never commit it."
else
    print_error "Failed to authenticate with GitHub Container Registry"
    exit 1
fi
