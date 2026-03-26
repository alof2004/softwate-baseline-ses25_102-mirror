# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Full-stack clinic management application with React frontend, Spring Boot backend, and PostgreSQL database. The application manages patients and appointments with a focus on security through comprehensive SAST tooling integrated into the development lifecycle.

## Development Stack

- **Backend**: Spring Boot 4.0.2, Java 21, PostgreSQL, JPA/Hibernate, Lombok
- **Frontend**: React 19, Vite 7, React Router, React Icons
- **Infrastructure**: Docker Compose, PostgreSQL 16

## Quick Start Commands

### Full Stack (Docker Compose)

```bash
# From repository root
cp .env.example .env
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Database: localhost:5432

### Local Development

**Start database only:**
```bash
docker compose up -d db
```

**Backend (requires Java 21 JDK):**
```bash
cd src/backend
./mvnw spring-boot:run
```

**Backend tests:**
```bash
cd src/backend
./mvnw test
```

**Frontend (requires Node.js 20+):**
```bash
cd src/frontend
npm install
npm run dev    # Development server
npm run build  # Production build
npm run lint   # ESLint with security rules
```

## Architecture

### Backend Structure

Standard Spring Boot layered architecture in package `org.pt.ua.deti.clinicProject`:

- `controllers/` - REST controllers exposing `/api/patients` and `/api/appointments` endpoints
- `services/` - Business logic layer
- `repositories/` - JPA repositories for data access
- `models/` - JPA entities (Patient, Appointment)
- `config/` - Application configuration including DataSeeder for initial data

REST API follows standard patterns:
- GET `/api/patients` - List all
- GET `/api/patients/{id}` - Get by ID
- POST `/api/patients` - Create
- PUT `/api/patients/{id}` - Update
- DELETE `/api/patients/{id}` - Delete

Same pattern applies to `/api/appointments`.

### Frontend Structure

React SPA with client-side routing:

- `src/pages/` - Page components (PatientsPage, AppointmentsPage)
- `src/components/` - Reusable UI components (modals, forms, panels)
- `src/layouts/` - Layout wrapper components (SidebarLayout)
- `src/consumers/` - API client layer
  - `httpConsumer.js` - Base HTTP client with fetch wrapper
  - `patientConsumer.js` - Patient API operations
  - `appointmentConsumer.js` - Appointment API operations
- `src/utils/` - Utility functions
- `src/constants/` - Constants and configuration

The frontend uses Vite's proxy to forward `/api/*` requests to the backend (configured in `vite.config.js`).

### Database

PostgreSQL with JPA entities using Hibernate DDL auto-generation. Configuration via environment variables in `.env` file.

## SAST Security Tooling

This repository implements a multi-layered SAST strategy optimized for fast PR feedback and comprehensive nightly scanning. **All security gates must pass before merging.**

### Architecture: Fast PR + Deep Nightly

**PR Checks (< 5 minutes):**
- Fast, high-signal tools for rapid developer feedback
- Blocks critical issues without slowing development

**Nightly Scans (20-40 minutes):**
- Comprehensive deep analysis tools
- Catches issues requiring more analysis time

### Pre-commit Hooks (Local)

Install once:
```bash
python3 -m pip install --user pre-commit
pre-commit install
```

Run manually:
```bash
pre-commit run --all-files
```

Blocks on:
- **Gitleaks**: Any secrets detected in staged changes
- **Semgrep**: ERROR severity findings (fast security patterns)

**Note**: Pre-commit hooks are bypassable, so PR checks are the real enforcement gate.

### PR/Push Workflow (CI)

Automatically runs on PRs and pushes to `develop`. Blocks on:

| Tool | Purpose | Blocks On |
| --- | --- | --- |
| **Gitleaks** | Secrets in commits | Any finding |
| **Semgrep** | Fast source code patterns | ERROR severity |
| **SonarQube** | Code quality & security (Java + JS) | Quality gate failure |
| **ESLint Security** | Frontend security anti-patterns | Linting errors |
| **npm audit** | Frontend dependency CVEs | HIGH/CRITICAL (prod deps) |
| **Trivy** | Dependencies, secrets, misconfigs | HIGH/CRITICAL |
| **Checkov** | Dockerfile, GitHub Actions, secrets | Policy violations |

**Note**: SonarQube runs as an ephemeral Docker service container in GitHub Actions - no external setup required!

### Nightly Workflow (Comprehensive)

Runs at 2 AM daily + manual dispatch. Blocks on:

| Tool | Purpose | Blocks On |
| --- | --- | --- |
| **CodeQL** | Deep semantic analysis (Java + JS) | Security findings |
| **Semgrep** | Full rulesets (OWASP Top 10, Java, JS) | ERROR severity |
| **Trivy** | Comprehensive dependency/IaC scan | MEDIUM+ for deps |
| **SpotBugs + FindSecBugs** | Deep Java bytecode analysis | Any bug |
| **OWASP Dependency-Check** | Java CVE database | CVSS ≥ 7 |
| **npm audit** | Frontend dependency CVEs | HIGH/CRITICAL |

**Note**: Gitleaks is intentionally excluded from nightly scans as it already runs on every PR/push.

### Local Security Commands

**Backend deep scan:**
```bash
cd src/backend
javac -version  # Confirm JDK 21 compiler
./mvnw -Psecurity-sast -DskipTests verify
```

**Backend dependency audit only:**
```bash
cd src/backend
./mvnw -Psecurity-sast -DskipTests -Dsecurity.sast.skipSpotbugs=true verify
```

**Frontend security checks:**
```bash
cd src/frontend
npm install
npm run lint  # ESLint with security rules
npm audit --omit=dev --audit-level=high
```

**Trivy scan:**
```bash
# Filesystem scan
docker run --rm -v "$PWD:/repo" -w /repo \
  aquasec/trivy:latest fs --config trivy.yaml .

# Backend dependency scan
docker run --rm -v "$PWD:/repo" -w /repo \
  aquasec/trivy:latest fs --scanners vuln src/backend
```

**Checkov IaC scan:**
```bash
docker run --rm -v "$PWD:/repo" -w /repo \
  bridgecrew/checkov:latest --config-file .checkov.yaml
```

**Important**: Backend scans require a real JDK 21 compiler (not just runtime). Ensure `JAVA_HOME` and `PATH` point to JDK 21.

### Security Configuration Files

- `.pre-commit-config.yaml` - Local hook definitions
- `.gitleaks.toml` - Gitleaks config with allowlists
- `.semgrep.yml` - Custom Semgrep rules
- `.semgrepignore` - Semgrep exclusions
- `sonar-project.properties` - SonarQube project configuration
- `trivy.yaml` - Trivy configuration
- `.trivyignore` - Trivy suppression list
- `.checkov.yaml` - Checkov policy configuration
- `src/frontend/eslint.config.js` - ESLint with security plugin
- `src/backend/pom.xml` - Maven `security-sast` profile
- `.github/workflows/sast.yml` - PR/push workflow
- `.github/workflows/sast-nightly.yml` - Nightly comprehensive scan

### Handling Security Findings

All findings must be triaged:
1. **True positive**: Fix the issue
2. **False positive**: Suppress narrowly with tool-specific config and document why
3. **Accepted risk**: Document the decision

**Suppression guidelines:**
- Use tool-specific suppression files (`.trivyignore`, `.semgrepignore`, Checkov skip annotations)
- Keep suppressions narrow (specific CVE/check + justification)
- Never globally weaken thresholds to make pipelines green

See `docs/sast-strategy.md` for comprehensive documentation on tool selection, blocking thresholds, and triage policy.

## Testing

Backend tests use JUnit with Spring Boot Test. H2 in-memory database for test isolation.

```bash
cd src/backend
./mvnw test
```

## Environment Variables

Root `.env` file (copy from `.env.example`) defines:
- Database credentials (`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`)
- Port mappings (`DB_PORT`, `BACKEND_PORT`, `FRONTEND_PORT`)
- Hibernate DDL mode (`SPRING_JPA_HIBERNATE_DDL_AUTO`)
- Vite API configuration (`VITE_API_BASE_URL`, `VITE_PROXY_TARGET`)

Backend `application.properties` uses environment variable defaults with fallback values for local development.

## Git Workflow

**Branch Structure:**
- **`master`** - Protected production branch
- **`develop`** - Active development branch
- Feature branches → `develop` → `master`

**Pull Request Flow:**
- Development happens on `develop` branch (or feature branches)
- PRs are created FROM `develop` TO `master` for releases
- GitHub Actions SAST checks run on all PRs targeting `master`

### Branch Protection

The `master` branch should be protected with rules requiring:
- All SAST checks to pass (7 required status checks)
- At least 1 PR approval
- Conversation resolution before merging
- No direct pushes to master

**Setup Instructions**: See `docs/branch-protection.md` for complete configuration guide.

## Key Files

- `docker-compose.yml` - Multi-container orchestration
- `src/backend/pom.xml` - Maven build configuration with security profile
- `src/frontend/package.json` - NPM dependencies and scripts
- `src/frontend/vite.config.js` - Vite configuration with API proxy
- `src/backend/src/main/resources/application.properties` - Spring Boot configuration
- `sonar-project.properties` - SonarQube project configuration
- `docs/sast-strategy.md` - Comprehensive SAST approach documentation
- `docs/sonarqube-setup.md` - SonarQube server setup guide
- `docs/branch-protection.md` - Branch protection rules setup guide
