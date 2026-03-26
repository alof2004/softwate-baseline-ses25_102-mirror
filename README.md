# Clinic Project

Full-stack clinic management application with:

- `src/frontend`: React + Vite frontend
- `src/backend`: Spring Boot backend
- PostgreSQL database

## Quick Start (Docker Compose)

Run everything from the repository root.

1. Create your environment file:

```bash
cp .env.example .env
```

2. Start the full stack:

```bash
docker compose up --build
```

3. Open the app:

- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080`

## Stop the Project

```bash
docker compose down
```

To also remove the database volume:

```bash
docker compose down -v
```

## Local Development (Without Full Docker)

You can run services separately.

### 1. Start the database

Use Docker (recommended):

```bash
docker compose up -d db
```

### 2. Run the backend

Requirements:

- Java 21

Start the backend:

```bash
cd src/backend
./mvnw spring-boot:run
```

Backend URL: `http://localhost:8080`

### 3. Run the frontend

Requirements:

- Node.js 20+
- npm

Start the frontend:

```bash
cd src/frontend
npm install
npm run dev
```

Frontend URL: `http://localhost:5173`

## Environment Variables

The root `.env.example` defines the values used by Docker Compose, including:

- Database credentials and ports
- Backend port
- Frontend port
- Vite API/proxy variables

Copy it to `.env` and adjust values if needed.

## SAST Tooling

This repository includes static security checks at three levels:

- Local `pre-commit` hooks with `Gitleaks` and `Semgrep` for fast developer feedback
- GitHub Actions PR checks with `CodeQL`, `Gitleaks`, and frontend dependency scanning for merge protection
- A nightly GitHub Actions workflow with `CodeQL` and deeper backend Java scans

### Local setup

Requirements:

- Python 3
- A real JDK 21 compiler for backend build-based scans
- Node.js 20+ and npm

Install and enable the local hooks:

```bash
python3 -m pip install --user pre-commit
pre-commit install
```

If your system blocks `pip install --user` with an externally-managed Python environment, create a virtual environment or use `pipx` instead.

Run the local SAST hooks against all tracked files:

```bash
pre-commit run --all-files
```

### Backend deep scan

The backend security profile adds SpotBugs with FindSecBugs.

```bash
cd src/backend
javac -version
# Confirm the compiler is JDK 21 before running the security profile.
./mvnw -Psecurity-sast -DskipTests verify
```

See [`docs/sast-strategy.md`](docs/sast-strategy.md) for the rationale, blocking thresholds, and triage policy.
