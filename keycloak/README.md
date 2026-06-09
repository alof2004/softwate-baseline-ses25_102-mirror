# Keycloak — Clinic Realm

Keycloak runs at `http://localhost:8180` (port configurable via `KEYCLOAK_PORT` in `.env`).

**Admin console:** `http://localhost:8180/admin`
Credentials: `KEYCLOAK_ADMIN_USER` / `KEYCLOAK_ADMIN_PASSWORD` from `.env` (default: `admin` / `admin`).

The `clinic` realm is imported automatically at container startup from `keycloak/realm-export.json` via the `--import-realm` flag. No manual UI setup is needed.

---

## OIDC Endpoints (realm: `clinic`)

| Purpose | URL |
|---|---|
| Discovery document | `http://localhost:8180/realms/clinic/.well-known/openid-configuration` |
| Authorization endpoint | `http://localhost:8180/realms/clinic/protocol/openid-connect/auth` |
| Token endpoint | `http://localhost:8180/realms/clinic/protocol/openid-connect/token` |
| Userinfo endpoint | `http://localhost:8180/realms/clinic/protocol/openid-connect/userinfo` |
| End session (logout) | `http://localhost:8180/realms/clinic/protocol/openid-connect/logout` |
| JWKS (public keys) | `http://localhost:8180/realms/clinic/protocol/openid-connect/certs` |

---

## Authorization Code + PKCE Flow

This is a browser-based flow. The frontend (`clinic-frontend` client) is a **public client** — it never handles a client secret.

### Step-by-step

1. **Frontend generates a PKCE pair**
   - `code_verifier`: random 43–128 character URL-safe base64 string
   - `code_challenge`: `BASE64URL(SHA-256(code_verifier))`

2. **Frontend redirects the browser** to the authorization endpoint with:
   ```
   GET /realms/clinic/protocol/openid-connect/auth
     ?client_id=clinic-frontend
     &response_type=code
     &redirect_uri=http://localhost:5173/<callback>
     &scope=openid profile email
     &code_challenge=<computed>
     &code_challenge_method=S256
     &state=<random-nonce>
   ```

3. **User authenticates** at the Keycloak login page.

4. **Keycloak redirects back** to `redirect_uri` with `?code=<auth_code>&state=<state>`.

5. **Frontend verifies `state`** matches what it sent (CSRF protection), then **POSTs to the token endpoint**:
   ```
   POST /realms/clinic/protocol/openid-connect/token
   Content-Type: application/x-www-form-urlencoded

   grant_type=authorization_code
   &client_id=clinic-frontend
   &code=<auth_code>
   &redirect_uri=http://localhost:5173/<callback>
   &code_verifier=<original-verifier>
   ```

6. **Keycloak responds** with:
   ```json
   {
     "access_token": "<JWT>",
     "id_token": "<JWT>",
     "refresh_token": "<opaque>",
     "expires_in": 300,
     "token_type": "Bearer"
   }
   ```

7. **Frontend stores** the access token (memory or `sessionStorage` — never `localStorage`).

8. **Frontend attaches** the token to every API request:
   ```
   Authorization: Bearer <access_token>
   ```

---

## Clients

| Client ID | Type | Purpose |
|---|---|---|
| `clinic-frontend` | Public (PKCE) | React SPA login flow |
| `clinic-backend` | Bearer-only | Spring Boot token validation |

PKCE is enforced on `clinic-frontend` (`code_challenge_method=S256`). Requests without a valid `code_challenge` are rejected.

---

## Roles

Realm roles (appear in access token under `realm_access.roles[]`):

| Role | Description |
|---|---|
| `ADMIN` | Full administrative access |
| `DOCTOR` | Can view/update patients and appointments |
| `RECEPTIONIST` | Can manage appointments and view patients |

---

## Sample Users

| Username | Password | Role |
|---|---|---|
| `admin.user` | `Admin1234!` | `ADMIN` |
| `doctor.user` | `Doctor1234!` | `DOCTOR` |
| `receptionist.user` | `Receptionist1234!` | `RECEPTIONIST` |

---

## For Tomás (Spring Boot JWT Validation)

```
Issuer URI (from host):    http://localhost:8180/realms/clinic
Issuer URI (from Docker):  http://keycloak:8080/realms/clinic
JWKS URI:                  http://localhost:8180/realms/clinic/protocol/openid-connect/certs
```

Use `spring.security.oauth2.resourceserver.jwt.issuer-uri` pointing to the internal Docker issuer URI when configuring `application.properties`. The `clinic-backend` client is `bearerOnly=true` — no login flow, token validation only.

Roles are under `realm_access.roles[]` in the decoded access token.

---

## Resetting the Realm

Keycloak **skips import** if the `clinic` realm already exists. To force a clean re-import after modifying `realm-export.json`:

```bash
docker compose rm -sf keycloak
docker compose up -d keycloak
```

> H2 storage is ephemeral — any changes made via the admin UI are lost when the container is removed. All permanent configuration must live in `realm-export.json`.
