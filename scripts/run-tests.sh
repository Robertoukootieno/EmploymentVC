#!/bin/bash

# Script to run tests for all microservices

set -e

echo "🧪 Running tests for all Provenly microservices..."

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

# Track test results
FAILED_SERVICES=()
PASSED_SERVICES=()

# Function to run tests for a service
run_service_tests() {
    local service=$1
    
    echo "🧪 Running tests for $service..."
    
    if [ -f "provenly-services/$service/package.json" ]; then
        cd "provenly-services/$service"
        
        # Check if test script exists
        if npm run | grep -q "test"; then
            if npm test; then
                PASSED_SERVICES+=("$service")
                echo "✅ Tests passed for $service"
            else
                FAILED_SERVICES+=("$service")
                echo "❌ Tests failed for $service"
            fi
        else
            echo "⚠️  No test script found for $service, skipping..."
        fi
        
        cd ../..
    else
        echo "⚠️  No package.json found for $service, skipping..."
    fi
}

# Run tests for all services
for service in "${SERVICES[@]}"; do
    run_service_tests "$service"
    echo ""
done

# Print summary
echo "📊 Test Summary:"
echo "==============="

if [ ${#PASSED_SERVICES[@]} -gt 0 ]; then
    echo "✅ Passed (${#PASSED_SERVICES[@]}):"
    for service in "${PASSED_SERVICES[@]}"; do
        echo "  - $service"
    done
fi

if [ ${#FAILED_SERVICES[@]} -gt 0 ]; then
    echo "❌ Failed (${#FAILED_SERVICES[@]}):"
    for service in "${FAILED_SERVICES[@]}"; do
        echo "  - $service"
    done
    echo ""
    echo "❌ Some tests failed. Please check the output above."
    exit 1
else
    echo ""
    echo "🎉 All tests passed successfully!"
fi
