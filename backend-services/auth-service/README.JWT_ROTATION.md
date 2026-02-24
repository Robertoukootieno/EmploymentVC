# JWT Rotation & Refresh Token Security

## Overview
This service implements secure JWT rotation and refresh token management to prevent token hijacking and replay attacks. The approach uses short-lived access tokens and one-time-use refresh tokens, with in-memory or distributed storage for refresh token validity.

## Endpoints
- `POST /api/auth/login` — Issues access and refresh tokens on successful authentication.
- `POST /api/auth/refresh` — Rotates refresh token, issues new access and refresh tokens. Old refresh token is revoked.
- `POST /api/auth/logout` — Revokes the provided refresh token.

## Flow
1. **Login:**
   - User authenticates, receives `accessToken` (short-lived) and `refreshToken` (long-lived, one-time-use).
   - Both tokens are JWTs. The refresh token contains a unique `jti` (token ID).
   - The refresh token ID is stored in the server-side store (in-memory or distributed).
2. **Access:**
   - User presents `accessToken` for API calls. If expired, uses `refreshToken` to obtain new tokens.
3. **Refresh:**
   - User calls `/refresh` with the refresh token.
   - The server checks:
     - Token signature and expiration.
     - Token ID is present and valid in the store.
   - If valid, the old token is revoked, a new token ID is generated, and new tokens are issued.
   - If invalid or expired, the request is rejected.
4. **Logout:**
   - User calls `/logout` with the refresh token.
   - The server revokes the refresh token, preventing further use.

## Security Benefits
- **Prevents replay:** Each refresh token is one-time-use. Replay attempts are rejected.
- **Immediate revocation:** Logout or compromise revokes the token instantly.
- **Short-lived access tokens:** Limits window for token abuse.

## Implementation Notes
- For production, use a distributed store (e.g., Redis) for refresh tokens.
- The in-memory store is for demonstration and single-instance deployments only.
- All endpoints return standard JSON responses with new tokens or error messages.

## Example Request/Response
### Login
```
POST /api/auth/login
{
  "email": "user@example.com",
  "password": "password"
}
Response:
{
  "accessToken": "...",
  "refreshToken": "...",
  "expiresIn": 900,
  "tokenType": "Bearer",
  "user": { ... }
}
```

### Refresh
```
POST /api/auth/refresh
{
  "refreshToken": "..."
}
Response:
{
  "accessToken": "...",
  "refreshToken": "..."
}
```

### Logout
```
POST /api/auth/logout
{
  "refreshToken": "..."
}
Response:
"Logged out"
```

## References
- [OWASP JWT Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_Cheat_Sheet_for_Java.html)
- [OAuth 2.0 BCP: Token Replay Prevention](https://datatracker.ietf.org/doc/html/rfc6819#section-5.2.2.3)
