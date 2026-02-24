#!/bin/bash

##############################################################################
# Phase 2 Security Deployment Script
# Deploys ModSecurity + nginx WAF, CrowdSec IDS/IPS, and Failed Login Throttling
#
# Usage: ./deploy-phase2-security.sh [option]
# Options: modsecurity, crowdsec, throttling, all, status, stop, logs
##############################################################################

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

SECURITY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$(dirname "$SECURITY_DIR")"
PROJECT_ROOT="$(dirname "$INFRA_DIR")"
DOCKER_COMPOSE_FILE="$SECURITY_DIR/docker-compose.security.yml"
COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-employmentvc-security}"
COMPOSE_CMD="docker compose -p $COMPOSE_PROJECT_NAME -f $DOCKER_COMPOSE_FILE"

print_header() {
    echo -e "${BLUE}===================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}===================================================${NC}"
}

print_success() { echo -e "${GREEN}✓ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠ $1${NC}"; }
print_error() { echo -e "${RED}✗ $1${NC}"; }

check_docker() {
    if ! command -v docker >/dev/null 2>&1; then
        print_error "Docker is not installed"
        exit 1
    fi
    if ! docker ps >/dev/null 2>&1; then
        print_error "Docker daemon is not running"
        exit 1
    fi
    print_success "Docker is available"
}

check_certificates() {
    print_header "Checking SSL Certificates"

    if [ ! -f "$SECURITY_DIR/certs/cert.pem" ] || [ ! -f "$SECURITY_DIR/certs/key.pem" ]; then
        print_warning "SSL certificates not found, generating self-signed certificates..."
        mkdir -p "$SECURITY_DIR/certs"

        openssl req -x509 -newkey rsa:4096 \
            -keyout "$SECURITY_DIR/certs/key.pem" \
            -out "$SECURITY_DIR/certs/cert.pem" \
            -days 365 -nodes \
            -subj "/CN=localhost/O=EmploymentVC/C=US" >/dev/null 2>&1

        cp "$SECURITY_DIR/certs/cert.pem" "$SECURITY_DIR/certs/chain.pem"
        print_success "Created self-signed certs in $SECURITY_DIR/certs"
    else
        print_success "SSL certificates found"
    fi
}

create_log_directories() {
    print_header "Creating Log Directories"

    local dirs=(
        "/var/log/nginx"
        "/var/log/modsecurity"
        "/var/log/employmentvc/api-gateway"
        "/var/log/employmentvc/auth-service"
        "/var/log/employmentvc/did-registry"
    )

    for dir in "${dirs[@]}"; do
        if [ ! -d "$dir" ]; then
            sudo mkdir -p "$dir"
            sudo chmod 755 "$dir"
            print_success "Created $dir"
        else
            print_success "Exists $dir"
        fi
    done
}

create_env_file() {
    local env_file="$SECURITY_DIR/.env"
    if [ ! -f "$env_file" ]; then
        cat > "$env_file" << 'EOF'
# CrowdSec Configuration
CROWDSEC_AGENT_PASSWORD=temporaryPassword123!ChangeMe
CROWDSEC_BOUNCER_API_KEY=to_be_generated_by_crowdsec
CROWDSEC_API_KEY=to_be_generated_by_crowdsec

# Environment
TZ=UTC
EOF
        print_success "Created $env_file"
    fi
}

deploy_modsecurity() {
    print_header "Deploying ModSecurity WAF"
    check_docker
    check_certificates
    create_log_directories

    $COMPOSE_CMD up -d nginx-modsecurity
    sleep 5

    if [ -n "$($COMPOSE_CMD ps -q nginx-modsecurity)" ]; then
        print_success "ModSecurity WAF is running"
        echo "  HTTPS: https://localhost"
        echo "  Metrics: http://localhost:8080/nginx_status"
        echo "  Logs: $COMPOSE_CMD logs -f nginx-modsecurity"
    else
        print_error "Failed to start nginx-modsecurity"
        $COMPOSE_CMD logs nginx-modsecurity
        exit 1
    fi
}

deploy_crowdsec() {
    print_header "Deploying CrowdSec IDS/IPS"
    check_docker
    create_env_file

    $COMPOSE_CMD up -d crowdsec crowdsec-bouncer crowdsec-ui
    sleep 15

    if [ -n "$($COMPOSE_CMD ps -q crowdsec)" ]; then
        print_success "CrowdSec services are running"

        local bouncer_key=""
        local attempts=0
        while [ -z "$bouncer_key" ] && [ $attempts -lt 5 ]; do
            bouncer_key=$($COMPOSE_CMD exec -T crowdsec cscli bouncers add nginx-bouncer -o json 2>/dev/null | jq -r '.api_key' 2>/dev/null || true)
            if [ -z "$bouncer_key" ] || [ "$bouncer_key" = "null" ]; then
                bouncer_key=""
                sleep 3
                attempts=$((attempts + 1))
            fi
        done

        if [ -n "$bouncer_key" ]; then
            sed -i "s/CROWDSEC_BOUNCER_API_KEY=.*/CROWDSEC_BOUNCER_API_KEY=${bouncer_key}/" "$SECURITY_DIR/.env"
            print_success "Updated bouncer API key in .env"
        else
            print_warning "Could not auto-generate bouncer key yet; run again after stack settles"
        fi

        echo "  Dashboard: http://localhost:3001"
        echo "  Decisions: $COMPOSE_CMD exec -T crowdsec cscli decisions list"
        echo "  Logs: $COMPOSE_CMD logs -f crowdsec"
    else
        print_error "Failed to start CrowdSec"
        $COMPOSE_CMD logs crowdsec
        exit 1
    fi
}

deploy_throttling() {
    print_header "Deploying Failed Login Throttling"

    local auth_build_file="$PROJECT_ROOT/backend-services/auth-service/build.gradle"
    if [ ! -f "$auth_build_file" ]; then
        print_error "Auth service build.gradle not found at $auth_build_file"
        exit 1
    fi

    print_warning "Building auth-service with failed login throttling..."
    cd "$PROJECT_ROOT/backend-services/auth-service"

    if ./gradlew clean build -x test >/tmp/auth-service-build.log 2>&1; then
        print_success "Auth service built"
    else
        print_error "Auth service build failed"
        tail -20 /tmp/auth-service-build.log
        exit 1
    fi

    if docker compose -f "$PROJECT_ROOT/docker-compose.yml" up -d auth-service >/tmp/auth-service-deploy.log 2>&1; then
        print_success "Auth service deployment command completed"
    else
        print_error "Failed to deploy auth-service"
        tail -20 /tmp/auth-service-deploy.log
        exit 1
    fi
}

check_status() {
    print_header "Phase 2 Security Stack Status"

    echo ""
    echo -e "${BLUE}Project:${NC} $COMPOSE_PROJECT_NAME"

    for svc in nginx-modsecurity crowdsec crowdsec-bouncer crowdsec-ui; do
        if [ -n "$($COMPOSE_CMD ps -q $svc)" ]; then
            print_success "$svc running"
        else
            print_warning "$svc not running"
        fi
    done

    echo ""
    echo -e "${BLUE}Compose ps:${NC}"
    $COMPOSE_CMD ps || true

    echo ""
    echo -e "${BLUE}Log Disk Usage:${NC}"
    du -sh /var/log/nginx /var/log/modsecurity /var/log/employmentvc/* 2>/dev/null || echo "  Logs not yet created"
}

stop_security_stack() {
    print_header "Stopping Phase 2 Security Stack"
    $COMPOSE_CMD down
    print_success "Security stack stopped"
}

view_logs() {
    print_header "Viewing Live Logs"
    echo "Press Ctrl+C to stop"
    $COMPOSE_CMD logs -f nginx-modsecurity crowdsec crowdsec-bouncer crowdsec-ui
}

main() {
    if [ $# -eq 0 ]; then
        print_header "Phase 2 Security Deployment Script"
        echo "Usage: $0 [OPTION]"
        echo ""
        echo "Options:"
        echo "  all          - Deploy all Phase 2 components"
        echo "  modsecurity  - Deploy only ModSecurity WAF"
        echo "  crowdsec     - Deploy only CrowdSec IDS/IPS"
        echo "  throttling   - Deploy only failed login throttling"
        echo "  status       - Check status of deployed components"
        echo "  stop         - Stop security stack"
        echo "  logs         - View security logs"
        exit 0
    fi

    case "$1" in
        all)
            deploy_modsecurity
            echo ""
            deploy_crowdsec
            echo ""
            deploy_throttling
            echo ""
            check_status
            ;;
        modsecurity)
            deploy_modsecurity
            ;;
        crowdsec)
            deploy_crowdsec
            ;;
        throttling)
            deploy_throttling
            ;;
        status)
            check_status
            ;;
        stop)
            stop_security_stack
            ;;
        logs)
            view_logs
            ;;
        *)
            print_error "Unknown option: $1"
            exit 1
            ;;
    esac
}

main "$@"
