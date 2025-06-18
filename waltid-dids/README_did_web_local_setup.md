
# 🛠️ Local Development Setup: `did:web` for Issuer Onboarding using `mkcert` and `http-server`

This guide helps you onboard a new issuer locally by setting up a `did:web`-compliant DID document, served securely over HTTPS using `mkcert` and a static HTTP server. This environment is ideal for development and integration testing with identity frameworks like [walt.id](https://walt.id/).

## ✅ Prerequisites

Make sure the following are installed:

- Ubuntu (or equivalent Linux system)
- `mkcert` (for generating trusted local TLS certificates)
- Node.js + `pnpm` or `npm`
- `http-server` (static file server)
- `waltid-cli` (for creating and resolving DIDs)

## 🪪 Onboarding a New Issuer — Operation Summary

| Step | Operation | Description |
|------|-----------|-------------|
| 1 | Create fake local domain | Add entry to `/etc/hosts` to alias a local hostname (e.g. `robskytec.local`) |
| 2 | Install `mkcert` | Set up local certificate authority and TLS generation tool |
| 3 | Generate TLS certificate | Create HTTPS certs for your local domain |
| 4 | Create DID Document | Use `waltid-cli` to generate a `did:web` DID |
| 5 | Set up `.well-known` directory | Copy DID JSON to the correct location as per `did:web` spec |
| 6 | Serve the DID document | Use `http-server` with HTTPS certs to serve your DID document |
| 7 | Verify locally | Confirm document is served correctly using `curl` |
| 8 | Resolve the DID | Use `waltid-cli` to resolve the DID via HTTPS |

## 🧾 Step-by-Step Guide

### 🧩 Step 1: Create a Local Domain

Edit the system hosts file:

```bash
sudo nano /etc/hosts
```

Add the following line:

```bash
127.0.0.1 robskytec.local
```

### 🛡️ Step 2: Install `mkcert`

```bash
sudo apt update
sudo apt install libnss3-tools wget -y
wget https://github.com/FiloSottile/mkcert/releases/latest/download/mkcert-v1.4.4-linux-amd64
chmod +x mkcert-v1.4.4-linux-amd64
sudo mv mkcert-v1.4.4-linux-amd64 /usr/local/bin/mkcert
mkcert -install
```

### 🔐 Step 3: Generate TLS Certificates

```bash
mkdir -p ~/certs
cd ~/certs
mkcert robskytec.local
```

### 🧾 Step 4: Create `did:web` Document

```bash
./waltid-cli did create --method web --did-web-domain robskytec.local
mkdir -p ~/waltid-dids/.well-known
cp ~/.waltid/dids/web/robskytec.local/did.json ~/waltid-dids/.well-known/did.json
```

### 🌐 Step 5: Install `http-server`

#### Option A: Using `npm`

```bash
sudo apt install nodejs npm -y
sudo npm install -g http-server
```

#### Option B: Using `pnpm`

```bash
corepack enable
corepack prepare pnpm@latest --activate
pnpm setup
exec $SHELL
pnpm add -g http-server
```

### 🔌 Step 6: Serve DID Document Over HTTPS

```bash
http-server ~/waltid-dids -S \
-C ~/certs/robskytec.local.pem \
-K ~/certs/robskytec.local-key.pem \
-a robskytec.local \
-p 443
```

### 🔍 Step 7: Verify with `curl`

```bash
curl -v https://robskytec.local/.well-known/did.json --insecure
```

### 🔁 Step 8: Resolve DID with `waltid-cli`

```bash
./waltid-cli did resolve did:web:robskytec.local
```

## 🗂️ Folder Structure

```
~/
├── certs/
│   ├── robskytec.local.pem
│   └── robskytec.local-key.pem
├── waltid-dids/
│   └── .well-known/
│       └── did.json
```

## 🔎 Notes

- The `/.well-known/did.json` path is **required** by the `did:web` specification.
- Use `--insecure` only for local dev/testing environments.
- `mkcert` certificates are **not valid for production** — they are trusted only locally.


