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
npm run lint   # ESLint
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

This repository has strict security scanning integrated at three lifecycle stages. **Any security-related changes must pass all gates.**

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
- **Gitleaks**: Any secrets detected
- **Semgrep**: ERROR severity findings (OWASP Top 10, Java, JavaScript rulesets)

### PR/Push Workflow (CI)

Automatically runs on PRs and pushes to `develop` branch. Blocks on:
- Gitleaks violations
- Semgrep ERROR severity findings
- npm audit high/critical vulnerabilities in production dependencies
- OWASP Dependency-Check findings with CVSS ≥ 7

### Backend Security Profile

Deep security scan (SpotBugs + FindSecBugs + OWASP Dependency-Check):
```bash
cd src/backend
javac -version  # Confirm JDK 21 compiler
./mvnw -Psecurity-sast -DskipTests verify
```

Dependency audit only (used in PR workflow):
```bash
cd src/backend
./mvnw -Psecurity-sast -DskipTests -Dsecurity.sast.skipSpotbugs=true verify
```

**Important**: Backend scans require a real JDK 21 compiler (not just runtime). Ensure `JAVA_HOME` and `PATH` point to JDK 21.

### Security Configuration Files

- `.pre-commit-config.yaml` - Local hook definitions
- `.gitleaks.toml` - Gitleaks configuration with project-specific allowlists
- `.semgrep.yml` - Custom Semgrep rules
- `.semgrepignore` - Exclusions for generated/third-party code
- `.github/workflows/sast.yml` - PR/push SAST workflow
- `.github/workflows/sast-nightly.yml` - Nightly deep scan
- `src/backend/pom.xml` - Maven `security-sast` profile configuration

### Handling Security Findings

All findings must be triaged:
- **True positive**: Fix the issue
- **False positive**: Suppress narrowly with scanner-specific configuration and document why
- **Accepted risk**: Document the decision

Never globally weaken thresholds to make pipelines green. See `docs/sast-strategy.md` for detailed rationale and policy.

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

Main branch: **develop** (PRs target this branch)

GitHub Actions SAST checks run on all PRs and pushes to `develop`.

## Key Files

- `docker-compose.yml` - Multi-container orchestration
- `src/backend/pom.xml` - Maven build configuration with security profile
- `src/frontend/package.json` - NPM dependencies and scripts
- `src/frontend/vite.config.js` - Vite configuration with API proxy
- `src/backend/src/main/resources/application.properties` - Spring Boot configuration
- `docs/sast-strategy.md` - Comprehensive SAST approach documentation
