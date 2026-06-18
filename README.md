# ClinicWave - Software Baseline

ClinicWave is a full-stack clinic management application used as the software
baseline for the SES 25/26 security project. It includes a React frontend,
Spring Boot backend, PostgreSQL database, Keycloak identity provider, and an
Nginx + ModSecurity WAF in front of the API.

## Mirror Repository

This repository was mirrored to GitHub for CI and security workflow validation:

- <https://github.com/alof2004/softwate-baseline-ses25_102-mirror>

## Stack

- Frontend: React 19, Vite 7, React Router, Keycloak JS.
- Backend: Java 21, Spring Boot 4, Spring Security, OAuth2 Resource Server,
  Spring Data JPA, Bean Validation, Springdoc OpenAPI.
- Database: PostgreSQL 16.
- Identity provider: Keycloak 26 with an imported `clinic` realm.
- WAF: Nginx reverse proxy with ModSecurity and OWASP CRS.
- Security tooling: pre-commit, Gitleaks, Semgrep, ESLint security rules,
  Trivy, CodeQL, SonarQube, OWASP ZAP.

## Repository Layout

```text
.
├── .github/
│   ├── config/security/        # SAST and scanner configuration
│   ├── scripts/                # CI report renderers and PR comment helpers
│   └── workflows/              # SAST, nightly SAST, and PR DAST workflows
├── docs/                       # Project documents and abuse stories
├── keycloak/                   # Clinic realm export and Keycloak notes
├── src/backend/                # Spring Boot API
├── src/frontend/               # React frontend
├── waf/                        # Nginx + ModSecurity + CRS configuration
├── docker-compose.yml
└── .env.example
```

## Quick Start

Run everything from the repository root.

```bash
cp .env.example .env
docker compose up --build
```

Open:

- Frontend: `http://localhost:5173`
- WAF HTTP entry point: `http://localhost:8080`
- WAF HTTPS/API entry point: `https://localhost:8443`
- Backend OpenAPI through WAF: `https://localhost:8443/v3/api-docs`
- Keycloak: `http://localhost:8180`

The backend and database are intentionally not exposed on the host by default.
API traffic should go through the WAF. The commented `BACKEND_PORT` and `DB_PORT`
settings are only for local debugging.

Stop the stack:

```bash
docker compose down
```

Remove the database volume as well:

```bash
docker compose down -v
```

## Environment

Copy `.env.example` to `.env` before running Docker Compose. Important defaults:

- PostgreSQL: `clinic_db`, `clinic_user`, `clinic_pass`.
- Frontend port: `5173`.
- WAF ports: HTTP `8080`, HTTPS `8443`.
- Keycloak port: `8180`.
- Frontend API base URL: `https://localhost:8443/api`.
- Keycloak issuer: `http://localhost:8180/realms/clinic`.
- Backend JWKS URL inside Docker:
  `http://keycloak:8080/realms/clinic/protocol/openid-connect/certs`.

TLS certificates for the local WAF live under `certs/` and are mounted into the
WAF container. Browsers and `curl` may require accepting or ignoring the local
self-signed certificate.

## Login and Roles

The `clinic` realm is imported automatically from `keycloak/realm-export.json`.
See [keycloak/README.md](./keycloak/README.md) for OIDC endpoints and PKCE flow
details.

Sample users:

| Username | Password | Role |
| --- | --- | --- |
| `admin.user` | `Admin1234!` | `ADMIN` |
| `doctor.user` | `Doctor1234!` | `DOCTOR` |
| `receptionist.user` | `Receptionist1234!` | `RECEPTIONIST` |

Role permissions are configured in `src/backend/src/main/resources/application.yml`:

| Role | Patients | Appointments | Audit Logs |
| --- | --- | --- | --- |
| `ADMIN` | Read, create, update, delete | Read, create, update, delete | Read |
| `DOCTOR` | Read, update | Read, update | No access |
| `RECEPTIONIST` | Read, create | Read, create, update, delete | No access |

## Application Features

- Patient CRUD.
- Appointment CRUD and appointment filtering by patient name, date, status, and
  specialty.
- Admin-only audit log listing at `/api/audit-logs`.
- Seed data on first startup: 120 patients and 520 appointments.
- OpenAPI documentation at `/v3/api-docs` and Swagger UI through the backend.

## Backend Security

- Stateless Bearer-token authentication with Keycloak JWT validation.
- JWT issuer validation and JWKS-based signature verification.
- Realm roles mapped from `realm_access.roles[]` into Spring Security roles.
- Method-level authorization with `@PreAuthorize` and an explicit permission
  matrix loaded from YAML.
- CORS restricted to configured origins.
- CSRF, form login, and HTTP Basic disabled for the stateless API.
- Security headers for API responses.
- Bean Validation DTOs for request input.
- Per-IP backend rate limit: 100 API requests per minute.
- Audit logging for create, update, and delete actions.

## WAF

The WAF is the public API entry point in the Compose deployment.

It provides:

- HTTPS termination with TLS 1.2/1.3.
- Reverse proxying to the internal backend.
- OWASP CRS inspection through ModSecurity.
- Request body limits.
- API rate limiting at Nginx: 30 requests per second per IP with burst 60.
- Forwarded client IP headers for backend rate limiting and logs.
- Security headers for non-API responses.
- ModSecurity audit logging at `/var/log/modsec_audit.log`.

Project-specific CRS tuning lives in:

- `waf/crs-setup.d/01-clinic-exclusions.conf`
- `waf/crs-setup.d/02-clinic-rule-exclusions.conf`

## Local Development

Start only the database:

```bash
docker compose up -d db
```

Run the backend:

```bash
cd src/backend
./mvnw spring-boot:run
```

Run the frontend:

```bash
cd src/frontend
npm install
npm run dev
```

Useful local URLs:

- Backend direct URL if exposed for debugging: `http://localhost:8080`
- Frontend dev server: `http://localhost:5173`
- Keycloak admin console: `http://localhost:8180/admin`

## Tests and Quality Checks

Backend tests:

```bash
cd src/backend
./mvnw test
```

Frontend lint:

```bash
cd src/frontend
npm ci
npm run lint
```

Frontend production build:

```bash
cd src/frontend
npm ci
npm run build
```

Backend security profile with SpotBugs + FindSecBugs:

```bash
cd src/backend
./mvnw -Psecurity-sast verify
```

## Local SAST Hooks

The canonical pre-commit configuration is
`.github/config/security/pre-commit.yaml`. A compatibility copy also exists at
`.pre-commit-config.yaml`.

Install and enable the hooks:

```bash
python3 -m pip install --user pre-commit
pre-commit install --config .github/config/security/pre-commit.yaml
```

Run all hooks manually:

```bash
pre-commit run --all-files --config .github/config/security/pre-commit.yaml
```

The hooks run Gitleaks and Semgrep for fast local feedback before code reaches
CI.

## CI Security Workflows

GitHub Actions workflows live under `.github/workflows/`.

- `sast.yml`: PR SAST gate for `master`. It runs Gitleaks, Semgrep, ESLint
  security rules, Trivy filesystem/dependency scans, and SonarQube. Reports are
  uploaded as artifacts and PR summaries are generated where applicable.
- `sast-nightly.yml`: scheduled daily at 02:00 UTC and manually runnable. It
  runs CodeQL for Java and JavaScript/TypeScript, comprehensive Semgrep, and
  Trivy image scans for backend and frontend containers.
- `dast-pr.yml`: PR DAST gate for `master` and `develop`. It runs OWASP ZAP
  direct scans and WAF scans, uploads reports, comments summaries on PRs, and
  fails the gate when High severity findings are present.

Security scanner configuration lives in `.github/config/security/`.

## Project Documentation

- Abuse stories: [docs/abuse_stories/ABUSE_STORIES.md](./docs/abuse_stories/ABUSE_STORIES.md)
- Project specification PDFs: `docs/SES_Group_project.pdf` and
  `docs/SES_Group_projectV2.pdf`
- Keycloak details: [keycloak/README.md](./keycloak/README.md)

## Authors

- Afonso Ferreira, `113480`
- Tomás Brás, `112665`
