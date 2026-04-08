# Docker Build System Quick Reference

## 🚀 Local Development Builds

### First Time Setup
```bash
# Setup Docker Buildx for multi-architecture builds
cd /home/robert/EmploymentVC
./infra/docker/setup-buildx.sh
```

### Build Commands

#### Single Service
```bash
# Build for local use (amd64 only)
./infra/docker/build-scripts/build-service.sh api-gateway

# Build for specific registry and tag
./infra/docker/build-scripts/build-service.sh api-gateway ghcr.io/robertoukootieno/employmentvc v1.0.0

# Build and push to registry
./infra/docker/build-scripts/build-service.sh api-gateway ghcr.io/robertoukootieno/employmentvc latest --push

# Build for specific platform
./infra/docker/build-scripts/build-service.sh api-gateway ghcr.io/robertoukootieno/employmentvc latest --platform linux/arm64
```

#### All Services
```bash
# Build all services (multi-arch: amd64 + arm64)
./infra/docker/build-scripts/build-all-multiarch.sh

# Build and push all services
./infra/docker/build-scripts/build-all-multiarch.sh ghcr.io/robertoukootieno/employmentvc latest --push

# Build with custom registry and tag
./infra/docker/build-scripts/build-all-multiarch.sh myregistry.com/myorg v2.0.0 --push
```

---

## 🔐 Registry Authentication

### GitHub Container Registry (GHCR)
```bash
# Interactive setup
./infra/docker/registry-setup.sh

# Direct login
docker login ghcr.io -u YOUR_USERNAME -p YOUR_GITHUB_TOKEN

# Using environment variables
echo $GITHUB_TOKEN | docker login ghcr.io -u $GITHUB_USER --password-stdin
```

### Verify Authentication
```bash
cat ~/.docker/config.json | grep ghcr.io
```

---

## 📋 Available Services

| Service | Port | Description |
|---------|------|-------------|
| api-gateway | 8080 | Main API entry point |
| auth-service | 8081 | Authentication service |
| did-registry | 8082 | DID management |
| schema-registry | 8086 | Credential schemas |
| issuer-api | 8085 | Credential issuance |
| verifier-api | 8086 | Credential verification |
| wallet-api | 8084 | Wallet operations |
| custodial-wallet | 8087 | Custodial wallet service |
| noncustodial-gateway | 8088 | Non-custodial gateway |
| notification-service | 8090 | Notifications |
| workflow-service | 8089 | Workflow orchestration |

---

## 🔧 GitHub Actions Workflows

### 1. Main CI/CD Pipeline
**File:** `.github/workflows/ci-cd.yml`

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests
- Tags matching `v*` pattern

**What it does:**
1. Security scanning (Trivy)
2. Tests all backend libraries
3. Tests all backend services
4. Tests frontend applications
5. Builds and pushes Docker images (multi-arch)
6. Deploys to dev/prod environments

**Automatic:** Runs on every push/PR

---

### 2. Manual Build Workflow
**File:** `.github/workflows/build-with-scripts.yml`

**Triggers:**
- Manual (workflow_dispatch)
- Push to build-scripts directory

**Usage:**
1. Go to **Actions** tab in GitHub
2. Select **"Build with Scripts"**
3. Click **"Run workflow"**
4. Configure:
   - **Service:** Leave empty for all, or specify (e.g., `api-gateway`)
   - **Tag:** Docker tag (default: `latest`)
   - **Push:** Whether to push to registry (default: `true`)

**When to use:**
- Testing builds for specific services
- Creating hotfix images
- Building with custom tags
- Debugging build issues

---

## 🎯 Common Workflows

### Scenario 1: Developing a Single Service
```bash
# Edit code in backend-services/api-gateway/

# Build locally
./infra/docker/build-scripts/build-service.sh api-gateway

# Test the image
docker run -p 8080:8080 ghcr.io/robertoukootieno/employmentvc/api-gateway:latest

# When ready, push
./infra/docker/build-scripts/build-service.sh api-gateway ghcr.io/robertoukootieno/employmentvc dev-test --push
```

### Scenario 2: Release New Version
```bash
# After merging to main, create a tag
git tag -a v1.2.0 -m "Release version 1.2.0"
git push origin v1.2.0

# GitHub Actions will automatically:
# 1. Run all tests
# 2. Build all images with tags: v1.2.0, v1.2, latest
# 3. Push to ghcr.io
# 4. Deploy to production (if configured)
```

### Scenario 3: Hotfix for Production
```bash
# Option A: Using GitHub Actions (Recommended)
# 1. Go to Actions → "Build with Scripts"
# 2. Set service: "api-gateway"
# 3. Set tag: "hotfix-1.2.1"
# 4. Enable push
# 5. Run workflow

# Option B: Local build and push
./infra/docker/build-scripts/build-service.sh \
  api-gateway \
  ghcr.io/robertoukootieno/employmentvc \
  hotfix-1.2.1 \
  --push
```

### Scenario 4: Building for ARM (Apple Silicon, Raspberry Pi)
```bash
# Build single service for ARM64
./infra/docker/build-scripts/build-service.sh \
  api-gateway \
  ghcr.io/robertoukootieno/employmentvc \
  latest \
  --platform linux/arm64

# Build all services for both AMD64 and ARM64
./infra/docker/build-scripts/build-all-multiarch.sh \
  ghcr.io/robertoukootieno/employmentvc \
  latest \
  --push
```

---

## 🐛 Troubleshooting

### Build Fails: "buildx not found"
```bash
# Verify docker version
docker version

# Check buildx availability
docker buildx version

# Reinstall buildx if needed (Linux)
mkdir -p ~/.docker/cli-plugins
wget -O ~/.docker/cli-plugins/docker-buildx \
  https://github.com/docker/buildx/releases/latest/download/buildx-*-linux-amd64
chmod +x ~/.docker/cli-plugins/docker-buildx
```

### Build Fails: "multiple platforms not supported"
```bash
# Ensure QEMU is installed
docker run --privileged --rm tonistiigi/binfmt --install all

# Re-setup buildx
./infra/docker/setup-buildx.sh
```

### Authentication Fails
```bash
# Clear existing credentials
docker logout ghcr.io

# Login again
./infra/docker/registry-setup.sh

# Verify
docker pull ghcr.io/robertoukootieno/employmentvc/api-gateway:latest
```

### Build is Very Slow
```bash
# Use registry caching
docker buildx build \
  --cache-from type=registry,ref=ghcr.io/robertoukootieno/employmentvc/api-gateway:buildcache \
  --cache-to type=registry,ref=ghcr.io/robertoukootieno/employmentvc/api-gateway:buildcache,mode=max \
  ...

# Or use local cache
docker buildx build \
  --cache-from type=local,src=/tmp/.buildx-cache \
  --cache-to type=local,dest=/tmp/.buildx-cache \
  ...
```

### Image Too Large
```bash
# Check image size
docker images | grep employmentvc

# Inspect layers
docker history ghcr.io/robertoukootieno/employmentvc/api-gateway:latest

# Optimize Dockerfile:
# - Use multi-stage builds (already implemented)
# - Add more patterns to .dockerignore
# - Remove unnecessary dependencies
```

---

## 📚 Additional Resources

- [Docker Buildx Documentation](https://docs.docker.com/buildx/working-with-buildx/)
- [Multi-platform Images](https://docs.docker.com/build/building/multi-platform/)
- [GitHub Container Registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)

---

## 💡 Pro Tips

1. **Use Build Cache:** Always use `--cache-from` and `--cache-to` for faster builds
2. **Parallel Builds:** Build multiple services in parallel locally using `xargs`:
   ```bash
   echo "api-gateway auth-service did-registry" | xargs -n1 -P3 ./infra/docker/build-scripts/build-service.sh
   ```
3. **Test Before Push:** Always test images locally before pushing to registry
4. **Tag Consistently:** Use semantic versioning (v1.2.3) for releases
5. **Monitor Registry:** Check [GitHub Packages](https://github.com/Robertoukootieno?tab=packages) for image sizes and usage
