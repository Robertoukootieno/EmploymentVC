# Auth Service Testing Guide

## 📋 Table of Contents
1. [Prerequisites](#prerequisites)
2. [Setup Instructions](#setup-instructions)
3. [Testing Traditional Authentication](#testing-traditional-authentication)
4. [Testing Web3 Authentication](#testing-web3-authentication)
5. [Testing Token Refresh](#testing-token-refresh)
6. [Testing Logout](#testing-logout)
7. [Using Swagger UI](#using-swagger-ui)
8. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Services
- **PostgreSQL** (port 5432)
- **Redis** (port 6379)
- **Java 17+**
- **Gradle 8.5+**

### Optional Tools
- **Postman** or **cURL** for API testing
- **MetaMask** or similar Web3 wallet for Web3 authentication testing
- **DBeaver** or **pgAdmin** for database inspection

---

## Setup Instructions

### 1. Start Required Services

#### Using Docker Compose (Recommended)
```bash
# Start PostgreSQL and Redis
COMPOSE_PROJECT_NAME=employmentvc docker compose up -d postgres redis

# Verify services are running
COMPOSE_PROJECT_NAME=employmentvc docker compose ps
```

#### Manual Setup
```bash
# PostgreSQL
docker run -d \
  --name provenly-postgres \
  -e POSTGRES_DB=provenly_dev \
  -e POSTGRES_USER=provenly_dev \
  -e POSTGRES_PASSWORD=dev_password \
  -p 5432:5432 \
  postgres:15

# Redis
docker run -d \
  --name provenly-redis \
  -p 6379:6379 \
  redis:7-alpine
```

### 2. Configure Environment Variables

Create `.env` file in the auth-service directory:
```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/provenly_dev
DATABASE_USERNAME=provenly_dev
DATABASE_PASSWORD=dev_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=provenly-jwt-secret-key-change-this-in-production-minimum-256-bits
JWT_EXPIRATION_HOURS=24
JWT_REFRESH_EXPIRATION_DAYS=30

# Server
PORT=8081
```

### 3. Build and Run Auth Service

```bash
# Navigate to project root
cd /home/robert/EmploymentVC

# Build the project
./gradlew :backend-services:auth-service:build -x test

# Run the service
./gradlew :backend-services:auth-service:bootRun
```

The service will start on `http://localhost:8081`

### 4. Verify Service is Running

```bash
# Health check
curl http://localhost:8081/actuator/health

# Expected response:
# {"status":"UP"}
```

---

## Testing Traditional Authentication

### 1. Create a Test User (Manual Database Insert)

Since we don't have a registration endpoint yet, create a test user directly in the database:

```sql
-- Connect to PostgreSQL
psql -h localhost -U provenly_dev -d provenly_dev

-- Create test user with BCrypt hashed password "password123"
INSERT INTO users (id, email, name, password_hash, enabled, email_verified, created_at, updated_at, version)
VALUES (
  gen_random_uuid(),
  'test@example.com',
  'Test User',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- password123
  true,
  true,
  NOW(),
  NOW(),
  0
);

-- Add user role
INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_USER' FROM users WHERE email = 'test@example.com';
```

### 2. Test Login Endpoint

**Request:**
```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400,
    "tokenType": "Bearer",
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "email": "test@example.com",
      "name": "Test User",
      "roles": ["ROLE_USER"],
      "walletAddress": null,
      "did": null,
      "emailVerified": true,
      "enabled": true
    }
  },
  "timestamp": "2026-01-21T22:00:00Z"
}
```

### 3. Test Get Current User

**Request:**
```bash
# Save the access token from login response
ACCESS_TOKEN="eyJhbGciOiJIUzI1NiJ9..."

curl -X GET http://localhost:8081/api/v1/auth/me \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "test@example.com",
    "name": "Test User",
    "roles": ["ROLE_USER"],
    "walletAddress": null,
    "did": null,
    "emailVerified": true,
    "enabled": true
  }
}
```

---

## Testing Web3 Authentication

### 1. Generate Authentication Challenge

**Request:**
```bash
curl -X POST http://localhost:8081/api/v1/auth/web3/challenge \
  -H "Content-Type: application/json" \
  -d '{
    "walletAddress": "0x742d35Cc6634C0532925a3b844C9db96590b5b8c"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Challenge generated successfully",
  "data": {
    "challenge": "Sign this message to authenticate with Provenly:\n\nNonce: 123e4567-e89b-12d3-a456-426614174000\nIssued At: 2026-01-21T22:00:00Z",
    "nonce": "123e4567-e89b-12d3-a456-426614174000",
    "expiresAt": "2026-01-21T22:05:00Z"
  }
}
```

### 2. Sign the Challenge with MetaMask

Use MetaMask or Web3.js to sign the challenge message:

```javascript
// In browser console with MetaMask
const accounts = await ethereum.request({ method: 'eth_requestAccounts' });
const walletAddress = accounts[0];

const challenge = "Sign this message to authenticate with Provenly:\n\nNonce: 123e4567-e89b-12d3-a456-426614174000\nIssued At: 2026-01-21T22:00:00Z";

const signature = await ethereum.request({
  method: 'personal_sign',
  params: [challenge, walletAddress]
});

console.log('Signature:', signature);
```

### 3. Verify Signature and Authenticate

**Request:**
```bash
curl -X POST http://localhost:8081/api/v1/auth/web3/verify \
  -H "Content-Type: application/json" \
  -d '{
    "walletAddress": "0x742d35Cc6634C0532925a3b844C9db96590b5b8c",
    "signature": "0x1234567890abcdef...",
    "message": "Sign this message to authenticate with Provenly:\n\nNonce: 123e4567-e89b-12d3-a456-426614174000\nIssued At: 2026-01-21T22:00:00Z"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Web3 authentication successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400,
    "tokenType": "Bearer",
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "email": null,
      "name": "User 0x742d...5b8c",
      "roles": ["ROLE_USER"],
      "walletAddress": "0x742d35Cc6634C0532925a3b844C9db96590b5b8c",
      "did": null,
      "emailVerified": false,
      "enabled": true
    }
  }
}
```

---

## Testing Token Refresh

### Refresh Access Token

**Request:**
```bash
# Use the refresh token from login/Web3 auth response
REFRESH_TOKEN="eyJhbGciOiJIUzI1NiJ9..."

curl -X POST http://localhost:8081/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{
    \"refreshToken\": \"$REFRESH_TOKEN\"
  }"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400,
    "tokenType": "Bearer"
  }
}
```

---

## Testing Logout

### Logout and Revoke Tokens

**Request:**
```bash
ACCESS_TOKEN="eyJhbGciOiJIUzI1NiJ9..."

curl -X POST http://localhost:8081/api/v1/auth/logout \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Logout successful"
}
```

**Verify Token is Revoked:**
```bash
# Try to use the same access token
curl -X GET http://localhost:8081/api/v1/auth/me \
  -H "Authorization: Bearer $ACCESS_TOKEN"

# Expected: 401 Unauthorized
```

---

## Using Swagger UI

The Auth Service includes OpenAPI documentation accessible via Swagger UI.

### Access Swagger UI

1. Open browser: `http://localhost:8081/swagger-ui.html`
2. Explore available endpoints
3. Test endpoints directly from the UI

### Authorize Requests in Swagger

1. Click the **"Authorize"** button (lock icon)
2. Enter: `Bearer <your-access-token>`
3. Click **"Authorize"**
4. All subsequent requests will include the token

---

## Troubleshooting

### Common Issues

#### 1. Database Connection Error
```
Error: Connection refused: localhost:5432
```

**Solution:**
- Verify PostgreSQL is running: `docker ps | grep postgres`
- Check connection details in `application.yml`
- Test connection: `psql -h localhost -U provenly_dev -d provenly_dev`

#### 2. Redis Connection Error
```
Error: Unable to connect to Redis at localhost:6379
```

**Solution:**
- Verify Redis is running: `docker ps | grep redis`
- Test connection: `redis-cli ping` (should return "PONG")

#### 3. Invalid JWT Token
```
Error: JWT signature does not match
```

**Solution:**
- Ensure `JWT_SECRET` is consistent across restarts
- Check token hasn't expired (24 hours for access tokens)
- Verify token format: `Bearer <token>`

#### 4. Web3 Signature Verification Failed
```
Error: Invalid signature
```

**Solution:**
- Ensure the exact challenge message is signed (including newlines)
- Verify wallet address matches the one used to generate challenge
- Check challenge hasn't expired (5 minutes)
- Ensure nonce is still in Redis

#### 5. User Not Found
```
Error: User not found with email: test@example.com
```

**Solution:**
- Verify user exists in database: `SELECT * FROM users WHERE email = 'test@example.com';`
- Check user is enabled: `enabled = true`
- Create user if missing (see "Create a Test User" section)

### Checking Logs

```bash
# View application logs
./gradlew :backend-services:auth-service:bootRun

# Enable debug logging
# Add to application.yml:
logging:
  level:
    io.provenly: DEBUG
    org.springframework.security: DEBUG
```

### Database Inspection

```bash
# Connect to PostgreSQL
psql -h localhost -U provenly_dev -d provenly_dev

# Check users
SELECT id, email, name, wallet_address, enabled FROM users;

# Check refresh tokens
SELECT user_id, token, expires_at, revoked FROM refresh_tokens;

# Check user roles
SELECT u.email, ur.role
FROM users u
JOIN user_roles ur ON u.id = ur.user_id;
```

### Redis Inspection

```bash
# Connect to Redis
redis-cli

# Check stored challenges
KEYS web3:challenge:*

# Get challenge value
GET web3:challenge:0x742d35cc6634c0532925a3b844c9db96590b5b8c

# Check TTL
TTL web3:challenge:0x742d35cc6634c0532925a3b844c9db96590b5b8c
```

---

## Example Postman Collection

Import this JSON into Postman for quick testing:

```json
{
  "info": {
    "name": "Provenly Auth Service",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Traditional Login",
      "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"email\": \"test@example.com\",\n  \"password\": \"password123\"\n}"
        },
        "url": "http://localhost:8081/api/v1/auth/login"
      }
    },
    {
      "name": "Get Current User",
      "request": {
        "method": "GET",
        "header": [{"key": "Authorization", "value": "Bearer {{accessToken}}"}],
        "url": "http://localhost:8081/api/v1/auth/me"
      }
    },
    {
      "name": "Web3 Challenge",
      "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"walletAddress\": \"0x742d35Cc6634C0532925a3b844C9db96590b5b8c\"\n}"
        },
        "url": "http://localhost:8081/api/v1/auth/web3/challenge"
      }
    },
    {
      "name": "Refresh Token",
      "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"refreshToken\": \"{{refreshToken}}\"\n}"
        },
        "url": "http://localhost:8081/api/v1/auth/refresh"
      }
    },
    {
      "name": "Logout",
      "request": {
        "method": "POST",
        "header": [{"key": "Authorization", "value": "Bearer {{accessToken}}"}],
        "url": "http://localhost:8081/api/v1/auth/logout"
      }
    }
  ]
}
```

---

## Next Steps

After testing the Auth Service:

1. **Integration Testing**: Write automated tests for all endpoints
2. **Load Testing**: Test with multiple concurrent users
3. **Security Audit**: Review JWT configuration, password policies
4. **Keycloak Integration**: Add SSO support
5. **DID Authentication**: Implement DID-based login

---

**Testing Guide Complete!** 🎉

For issues or questions, check the logs and database state using the troubleshooting section above.

