#!/bin/bash

# Script to perform final cleanup of project structure

set -e

echo "🧹 Performing final cleanup of project structure..."

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

# Check what's in the old backend directory
print_step "Checking old backend directory contents..."
if [ -d "backend" ]; then
    echo "Contents of backend/:"
    ls -la backend/ || true
    echo ""
fi

# Move any useful files from old backend directory to correct locations
print_step "Moving files from old backend directory..."

# Check if there are any services in backend/ that need to be moved
if [ -d "backend/api-gateway" ]; then
    if [ ! -d "backend-services/api-gateway" ]; then
        print_warning "Moving api-gateway from backend/ to backend-services/"
        mv backend/api-gateway backend-services/
    else
        print_warning "api-gateway already exists in backend-services/, removing duplicate"
        rm -rf backend/api-gateway
    fi
fi

if [ -d "backend/auth-service" ]; then
    if [ ! -d "backend-services/auth-service" ]; then
        print_warning "Moving auth-service from backend/ to backend-services/"
        mv backend/auth-service backend-services/
    else
        print_warning "auth-service already exists in backend-services/, removing duplicate"
        rm -rf backend/auth-service
    fi
fi

if [ -d "backend/did-registry" ]; then
    if [ ! -d "backend-services/did-registry" ]; then
        print_warning "Moving did-registry from backend/ to backend-services/"
        mv backend/did-registry backend-services/
    else
        print_warning "did-registry already exists in backend-services/, removing duplicate"
        rm -rf backend/did-registry
    fi
fi

if [ -d "backend/credential-schema-registry" ]; then
    if [ ! -d "backend-services/schema-registry" ]; then
        print_warning "Moving credential-schema-registry to backend-services/schema-registry"
        mv backend/credential-schema-registry backend-services/schema-registry
    else
        print_warning "schema-registry already exists in backend-services/, removing duplicate"
        rm -rf backend/credential-schema-registry
    fi
fi

# Remove any other files/directories in backend/
if [ -d "backend" ]; then
    print_step "Checking for remaining files in backend/..."
    remaining_files=$(find backend -type f 2>/dev/null | wc -l)
    remaining_dirs=$(find backend -type d -not -path backend 2>/dev/null | wc -l)
    
    if [ "$remaining_files" -gt 0 ] || [ "$remaining_dirs" -gt 0 ]; then
        print_warning "Found $remaining_files files and $remaining_dirs directories in backend/"
        echo "Contents:"
        find backend -type f -o -type d | head -20
        echo ""
        
        # Create backup before deletion
        mkdir -p backup/old-backend-cleanup
        cp -r backend/* backup/old-backend-cleanup/ 2>/dev/null || true
        print_success "Created backup in backup/old-backend-cleanup/"
    fi
    
    # Remove the old backend directory
    rm -rf backend
    print_success "Removed old backend/ directory"
else
    print_success "No backend/ directory found"
fi

# Remove old frontend directory if it exists and is empty
if [ -d "frontend" ]; then
    if [ -z "$(ls -A frontend)" ]; then
        print_step "Removing empty frontend/ directory..."
        rmdir frontend
        print_success "Removed empty frontend/ directory"
    else
        print_warning "frontend/ directory is not empty, keeping it"
        echo "Contents:"
        ls -la frontend/
    fi
fi

# Verify final structure
print_step "Verifying final project structure..."
echo ""
echo "📁 Final Project Structure:"
echo "├── employmentVC-Applications/  # User-facing applications"
echo "│   ├── issuer-app/"
echo "│   ├── verifier-app/"
echo "│   └── holder-wallet/"
echo "├── backend-libraries/          # Core functionality libraries"
echo "│   ├── auth-lib/"
echo "│   ├── credentials-lib/"
echo "│   ├── crypto-lib/"
echo "│   ├── protocols-lib/"
echo "│   ├── sdjwts-lib/"
echo "│   ├── core-wallet-lib/"
echo "│   ├── utils-lib/"
echo "│   ├── did-lib/"
echo "│   ├── library-commons/"
echo "│   └── openid4vc-lib/"
echo "├── backend-services/           # API services"
echo "│   ├── issuer-api/"
echo "│   ├── verifier-api/"
echo "│   ├── wallet-api/"
echo "│   ├── web3-login-service/"
echo "│   ├── e2e-test-service/"
echo "│   ├── auth-service/"
echo "│   ├── did-registry/"
echo "│   ├── schema-registry/"
echo "│   └── api-gateway/"
echo "├── infra/                      # Infrastructure configs"
echo "├── k8s/                        # Kubernetes manifests"
echo "├── shared/                     # Shared resources"
echo "├── docs/                       # Documentation"
echo "├── scripts/                    # Utility scripts"
echo "├── build.gradle               # Root build file"
echo "├── settings.gradle            # Multi-project setup"
echo "└── docker-compose.yml         # Infrastructure services"
echo ""

# Check actual structure matches expected
print_step "Checking actual directory structure..."
actual_structure_issues=0

# Check required directories exist
required_dirs=(
    "employmentVC-Applications"
    "backend-libraries"
    "backend-services"
    "infra"
    "k8s"
    "shared"
    "docs"
    "scripts"
)

for dir in "${required_dirs[@]}"; do
    if [ ! -d "$dir" ]; then
        print_error "Missing required directory: $dir"
        actual_structure_issues=$((actual_structure_issues + 1))
    else
        print_success "Found required directory: $dir"
    fi
done

# Check for unwanted directories
unwanted_dirs=(
    "backend"
    "provenly-services"
)

for dir in "${unwanted_dirs[@]}"; do
    if [ -d "$dir" ]; then
        print_error "Found unwanted directory: $dir"
        actual_structure_issues=$((actual_structure_issues + 1))
    else
        print_success "Confirmed removal of: $dir"
    fi
done

# Summary
echo ""
if [ $actual_structure_issues -eq 0 ]; then
    print_success "✅ Project structure is clean and correct!"
    echo ""
    echo "🎯 Structure Summary:"
    echo "✅ Frontend Apps: $(find employmentVC-Applications -maxdepth 2 -type d | wc -l) directories"
    echo "✅ Backend Libraries: $(find backend-libraries -maxdepth 1 -type d | tail -n +2 | wc -l) libraries"
    echo "✅ Backend Services: $(find backend-services -maxdepth 1 -type d | tail -n +2 | wc -l) services"
    echo "✅ No duplicate or unwanted directories"
else
    print_error "❌ Found $actual_structure_issues structure issues"
    echo "Please review and fix the issues above."
fi

echo ""
print_success "Final cleanup completed! 🎉"
