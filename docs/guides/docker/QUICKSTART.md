# 🚀 Quick Start: Docker Build System

## New Team Member? Start Here!

### Prerequisites
- Docker 20.10+ installed
- Docker Buildx plugin (included in Docker Desktop, or install separately)
- GitHub account with repository access

---

## 1️⃣ First Time Setup (5 minutes)

```bash
# Clone the repository (if you haven't)
git clone https://github.com/Robertoukootieno/EmploymentVC.git
cd EmploymentVC

# Setup Docker Buildx for multi-architecture builds
./infra/docker/setup-buildx.sh

# Setup GitHub Container Registry authentication
./infra/docker/registry-setup.sh
# You'll need: Your GitHub username and a Personal Access Token
# Create token at: https://github.com/settings/tokens
# Required scopes: read:packages, write:packages
```

**That's it! You're ready to build.**

---

## 2️⃣ Daily Development

### Build Single Service (Most Common)
```bash
# Build the service you're working on
./infra/docker/build-scripts/build-service.sh api-gateway

# Run it locally to test
docker run -p 8080:8080 ghcr.io/robertoukootieno/employmentvc/api-gateway:latest
```

### Build All Services
```bash
# Build everything (takes longer, but comprehensive)
./infra/docker/build-scripts/build-all-multiarch.sh
```

### Push to Registry
```bash
# Build and push your changes
./infra/docker/build-scripts/build-service.sh api-gateway ghcr.io/robertoukootieno/employmentvc dev-yourname --push
```

---

## 3️⃣ Common Tasks

### Working on a Feature
```bash
# 1. Make your code changes in backend-services/api-gateway/

# 2. Build the image
./infra/docker/build-scripts/build-service.sh api-gateway

# 3. Test locally
docker run -p 8080:8080 ghcr.io/robertoukootieno/employmentvc/api-gateway:latest

# 4. If good, push with feature tag
./infra/docker/build-scripts/build-service.sh api-gateway ghcr.io/robertoukootieno/employmentvc feature-xyz --push
```

### Testing Multi-Architecture
```bash
# Build for ARM64 (Apple Silicon, cloud ARM instances)
./infra/docker/build-scripts/build-service.sh api-gateway ghcr.io/robertoukootieno/employmentvc latest --platform linux/arm64

# Build for both AMD64 and ARM64
./infra/docker/build-scripts/build-service.sh api-gateway ghcr.io/robertoukootieno/employmentvc latest --platform linux/amd64,linux/arm64 --push
```

### Check Build Logs
```bash
# View recent builds
docker buildx ls

# Check builder status
docker buildx inspect provenly-builder

# View available images
docker images | grep employmentvc
```

---

## 4️⃣ GitHub Actions (Automated Builds)

### Automatic Builds
Every time you push to `main` or `develop`:
- ✅ All tests run automatically
- ✅ All services build automatically
- ✅ Images push to GitHub Container Registry
- ✅ Multi-architecture images created (AMD64 + ARM64)

**You don't need to do anything!**

### Manual Builds (When You Need Control)
1. Go to: https://github.com/Robertoukootieno/EmploymentVC/actions
2. Click **"Build with Scripts"** workflow
3. Click **"Run workflow"** button (top right)
4. Fill in:
   - **Service:** (empty for all, or specific like `api-gateway`)
   - **Tag:** (e.g., `hotfix-1.2.3` or `latest`)
   - **Push:** (check to push to registry)
5. Click **"Run workflow"**

---

## 5️⃣ Available Services

| Service | Command |
|---------|---------|
| API Gateway | `./infra/docker/build-scripts/build-service.sh api-gateway` |
| Auth Service | `./infra/docker/build-scripts/build-service.sh auth-service` |
| DID Registry | `./infra/docker/build-scripts/build-service.sh did-registry` |
| Schema Registry | `./infra/docker/build-scripts/build-service.sh schema-registry` |
| Issuer API | `./infra/docker/build-scripts/build-service.sh issuer-api` |
| Verifier API | `./infra/docker/build-scripts/build-service.sh verifier-api` |
| Wallet API | `./infra/docker/build-scripts/build-service.sh wallet-api` |
| Custodial Wallet | `./infra/docker/build-scripts/build-service.sh custodial-wallet` |
| Non-Custodial Gateway | `./infra/docker/build-scripts/build-service.sh noncustodial-gateway` |
| Notification Service | `./infra/docker/build-scripts/build-service.sh notification-service` |
| Workflow Service | `./infra/docker/build-scripts/build-service.sh workflow-service` |

---

## 6️⃣ Troubleshooting

### "buildx: command not found"
```bash
# Check Docker version (need 20.10+)
docker version

# On Linux, install buildx manually
mkdir -p ~/.docker/cli-plugins
wget -O ~/.docker/cli-plugins/docker-buildx \
  https://github.com/docker/buildx/releases/latest/download/buildx-*-linux-amd64
chmod +x ~/.docker/cli-plugins/docker-buildx
```

### "permission denied" when running scripts
```bash
# Make scripts executable
chmod +x infra/docker/*.sh
chmod +x infra/docker/build-scripts/*.sh
```

### "authentication required" when pushing
```bash
# Re-login to registry
./infra/docker/registry-setup.sh

# Or manual login
docker login ghcr.io
```

### Build is slow
```bash
# First build is always slow (downloading base images)
# Subsequent builds are faster due to caching

# Speed tip: Build only what you need
./infra/docker/build-scripts/build-service.sh <your-service>
# Instead of building all services
```

### "multiple platforms not supported"
```bash
# Install QEMU
docker run --privileged --rm tonistiigi/binfmt --install all

# Re-setup buildx
./infra/docker/setup-buildx.sh
```

---

## 🆘 Need Help?

1. **Comprehensive Guide:** See [BUILD_REFERENCE.md](BUILD_REFERENCE.md)
2. **Docker Config:** See [README.md](README.md)
3. **Ask Team:** Slack channel #docker-builds
4. **GitHub Issues:** Report build system issues

---

## 💡 Pro Tips

✅ **Use tags wisely:** Dev builds use your name (e.g., `dev-john`), features use descriptive names (e.g., `feature-oauth`)

✅ **Test before push:** Always test images locally before pushing to registry

✅ **Build only what changed:** Building a single service is faster than building all

✅ **Check Actions tab:** Monitor automated builds on GitHub Actions tab

✅ **Clean up old images:** Run `docker system prune -a` periodically to free disk space

---

## 📚 Full Documentation

- **Quick Reference:** You're reading it! 🎉
- **Comprehensive Build Guide:** [BUILD_REFERENCE.md](BUILD_REFERENCE.md)
- **Docker Configuration:** [README.md](README.md)
- **System Enhancement Details:** [ENHANCEMENT_SUMMARY.md](ENHANCEMENT_SUMMARY.md)

---

**Happy Building! 🐳**
