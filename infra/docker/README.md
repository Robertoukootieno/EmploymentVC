# Docker Configuration & Infrastructure

This directory contains Docker daemon configurations and compose files for different environments.

## 📖 Quick Start

**See [BUILD_REFERENCE.md](docs/guides/docker/BUILD_REFERENCE.md) for comprehensive build guide and common workflows.**

```bash
# Setup Buildx (first time only)
./infra/docker/setup-buildx.sh

# Build all services
./infra/docker/build-scripts/build-all-multiarch.sh

# Build single service
./infra/docker/build-scripts/build-service.sh api-gateway
```

## Contents

### **Configuration Files**

- **`daemon.json`** - Production Docker daemon configuration
  - Optimized for security and performance
  - Log rotation enabled
  - BuildKit enabled
  - Storage driver: overlay2

- **`daemon.dev.json`** - Development Docker daemon configuration
  - Debug mode enabled
  - Relaxed logging
  - Experimental features enabled

- **`buildx.toml`** - Buildx configuration for multi-architecture builds
  - Multi-platform support (amd64, arm64)
  - Build parallelism settings
  - Registry mirror configuration

- **`.dockerignore`** - Global patterns for Docker build context
  - Excludes unnecessary files from images
  - Reduces build context size
  - Applied to all service builds

### **Compose Files**

- **`docker-compose.prod.yml`** - Production services configuration
  - Health checks for all services
  - Resource limits and reservations
  - Proper restart policies
  - Environment variable driven

### **Build Scripts**

- **`setup-buildx.sh`** - Setup Docker Buildx for multi-arch builds
- **`registry-setup.sh`** - Configure registry authentication
- **`build-scripts/build-service.sh`** - Build single service
- **`build-scripts/build-all.sh`** - Build all services (single arch)
- **`build-scripts/build-all-multiarch.sh`** - Build all services (multi-arch)

## Setup Instructions

### **Using Daemon Configuration**

#### **Linux**

1. Copy daemon configuration:
   ```bash
   sudo cp daemon.json /etc/docker/daemon.json
   # OR for development:
   sudo cp daemon.dev.json /etc/docker/daemon.json
   ```

2. Reload Docker daemon:
   ```bash
   sudo systemctl restart docker
   ```

3. Verify configuration:
   ```bash
   docker info | grep -i "log driver"
   docker info | grep -i "storage driver"
   ```

#### **Docker Desktop (macOS/Windows)**

1. Open Docker Desktop Preferences
2. Go to **Docker Engine** tab
3. Update the JSON with contents from `daemon.json`
4. Click **Apply & Restart**

### **Production Deployment**

1. Set environment variables:
   ```bash
  cp .env.example .env
  # then edit .env with secure production values
   ```

2. Start services:
   ```bash
  COMPOSE_PROJECT_NAME=employmentvc-prod docker compose -f docker-compose.prod.yml up -d
   ```

3. Verify health:
   ```bash
  COMPOSE_PROJECT_NAME=employmentvc-prod docker compose -f docker-compose.prod.yml ps
  COMPOSE_PROJECT_NAME=employmentvc-prod docker compose -f docker-compose.prod.yml logs -f
   ```

## Best Practices

### **Security**
- ✅ Use explicit daemon configurations
- ✅ Enable log rotation to prevent disk full
- ✅ Set resource limits for containers
- ✅ Use health checks for critical services
- ✅ Never commit secrets to version control

### **Performance**
- ✅ Use overlay2 storage driver (modern, performant)
- ✅ Enable BuildKit for faster builds
- ✅ Use `.dockerignore` to reduce build context
- ✅ Implement proper health checks with reasonable intervals

### **Monitoring**
- ✅ Monitor Docker metrics via prometheus endpoint (127.0.0.1:9323)
- ✅ Review logs regularly
- ✅ Set up alerts for container restarts

## Registry Configuration

For Production GHCR authentication, configure credentials:

```bash
# Login to GitHub Container Registry
docker login ghcr.io -u <username> -p <personal-access-token>

# Verify login
cat ~/.docker/config.json
```

## Building Services

See `build-scripts/` directory for automated build commands.

### Quick Build Commands

```bash
# Build single service
docker build -f backend-services/api-gateway/Dockerfile -t employmentvc/api-gateway:latest .

# Build with BuildKit (faster, parallel)
DOCKER_BUILDKIT=1 docker build -f backend-services/api-gateway/Dockerfile -t employmentvc/api-gateway:latest .

# Build multi-arch (requires buildx)
docker buildx build --platform linux/amd64,linux/arm64 -f backend-services/api-gateway/Dockerfile -t employmentvc/api-gateway:latest .
```

### Using Build Scripts

#### Setup Buildx (First Time Only)
```bash
# Setup Docker Buildx with multi-architecture support
./infra/docker/setup-buildx.sh

# Or with custom builder name
./infra/docker/setup-buildx.sh my-builder
```

#### Build All Services (Multi-Architecture)
```bash
# Build all services for linux/amd64 and linux/arm64
./infra/docker/build-scripts/build-all-multiarch.sh

# Build and push to registry
./infra/docker/build-scripts/build-all-multiarch.sh ghcr.io/yourorg/employmentvc latest --push
```

#### Build Single Service
```bash
# Build single service (loads locally)
./infra/docker/build-scripts/build-service.sh api-gateway

# Build and push to registry with custom tag
./infra/docker/build-scripts/build-service.sh api-gateway ghcr.io/yourorg/employmentvc v1.2.3 --push

# Build for specific platform
./infra/docker/build-scripts/build-service.sh api-gateway ghcr.io/yourorg/employmentvc latest --platform linux/arm64
```

### GitHub Actions Integration

The project includes two CI/CD workflows:

#### 1. Main CI/CD Pipeline (`.github/workflows/ci-cd.yml`)
- Runs on every push to `main` and `develop`
- Automated testing → Building → Pushing
- Uses Docker Buildx with inline configuration
- Registry caching for faster builds
- Supports semantic versioning tags

#### 2. Manual Build Workflow (`.github/workflows/build-with-scripts.yml`)
- Manually trigger builds via GitHub UI
- Uses the same build scripts as local development
- Options to:
  - Build all services or single service
  - Specify custom tags
  - Choose whether to push to registry
- Useful for hotfixes and testing

**Trigger Manual Build:**
1. Go to Actions tab on GitHub
2. Select "Build with Scripts"
3. Click "Run workflow"
4. Fill in parameters (service name, tag, push option)

**Benefits:**
- ✅ Consistent builds between local dev and CI/CD
- ✅ Multi-architecture support (amd64, arm64)
- ✅ Registry caching reduces build time
- ✅ Easy to debug - same scripts locally and in CI

## Troubleshooting

### Daemon Won't Start
```bash
# Check logs
journalctl -xu docker.service
# or
sudo /usr/local/bin/docker daemon --debug
```

### Build Context Too Large
- Ensure `.dockerignore` is properly configured
- Check for unnecessary files in build directory
- Use `docker build --progress=plain` to see context size

### Out of Disk Space
- Run cleanup: `docker system prune -a`
- Check log driver rotation is working
- Monitor volume usage: `docker system df`

## References

- [Docker Daemon Configuration Reference](https://docs.docker.com/config/daemon/)
- [Docker Compose Specification](https://docs.docker.com/compose/compose-file/)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)

## 🔒 Secrets Management

For production deployments, use Docker secrets or an external secrets manager (e.g., HashiCorp Vault, AWS Secrets Manager) for sensitive values. See the security documentation for integration examples.

- Never commit secrets to version control.
- Reference secrets in your compose files using the `secrets:` key or environment variables populated by your secrets manager.

---

## 🛡️ Image Vulnerability Scanning

Use the provided `image-scan.sh` script to scan all built images for vulnerabilities using Trivy:

```bash
./infra/docker/image-scan.sh
```

Install Trivy: https://aquasecurity.github.io/trivy/

---

## 🧹 Automated Cleanup

Use the provided `cleanup.sh` script to remove old images, containers, and volumes:

```bash
./infra/docker/cleanup.sh
```

---

## 📝 Local Development Setup

1. **Copy Example Files:**
   ```sh
   cp .env.example .env
   cp docker-compose.dev.example.yml docker-compose.dev.yml
   ```
2. **Edit `.env` and `docker-compose.dev.yml`** to match your local environment and secrets. These files are gitignored and safe for local changes.
3. **Start your stack:**
   ```sh
   docker compose -f docker-compose.dev.yml up -d
   ```

**Why use example files?**
- Prevents accidental commits of secrets or local overrides.
- Ensures every developer starts with a safe, documented config.
- Keeps sensitive and environment-specific data out of version control.
