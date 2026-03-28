# DAST Implementation Plan

## Executive Summary

This document outlines the strategy for implementing Dynamic Application Security Testing (DAST) for the clinic management application. DAST will complement the existing SAST pipeline by testing the running application for security vulnerabilities that only manifest at runtime.

**Key Decision: OWASP ZAP** has been selected as the primary DAST scanner due to its:
- Free, open-source nature with active community support
- Excellent automation capabilities via ZAP Automation Framework
- Docker-based deployment for consistent CI/CD integration
- Native SARIF output for GitHub Code Scanning integration
- Strong API testing capabilities suitable for REST APIs

## Workflow Separation

When DAST is integrated into GitHub Actions, the workflow split must remain explicit:

- **PR DAST** belongs in the **software** workflow path
- **Nightly DAST** belongs in the **security** workflow path

This separation should be preserved in Phase 2 and Phase 4 so PR validation and nightly security monitoring remain distinct concerns.

## Application Architecture Analysis

### Technology Stack
- **Frontend**: React 19.2.0 SPA with React Router (Vite build)
- **Backend**: Spring Boot 4.0.2 REST API (Java 21)
- **Database**: PostgreSQL
- **Deployment**: Docker Compose with 3 containers

### API Endpoints Identified
```
/api/patients
  - GET /api/patients (list all)
  - GET /api/patients/{id} (get by ID)
  - POST /api/patients (create)
  - PUT /api/patients/{id} (update)
  - DELETE /api/patients/{id} (delete)

/api/appointments
  - GET /api/appointments (list with optional filters)
  - GET /api/appointments/{id} (get by ID)
  - POST /api/appointments?patientId={id} (create)
  - PUT /api/appointments/{id} (update)
  - DELETE /api/appointments/{id} (delete)
```

### Frontend Routes
- `/` → redirects to `/patients`
- `/patients` - Patient management page
- `/appointments` - Appointment management page
- `/categories` → redirects to `/appointments`

### Security Characteristics
- **No authentication/authorization layer** - API is publicly accessible
- This simplifies DAST configuration (no session management needed)
- Focus areas: injection attacks, XSS, API abuse, data validation

## DAST Implementation Strategy

### 1. Environment Setup

#### Docker Compose Integration

Create a dedicated DAST environment by extending the existing `docker-compose.yml`:

**File: `docker-compose.dast.yml`**
```yaml
version: "3.8"

services:
  # Include existing services from main docker-compose.yml
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-clinic_db}
      POSTGRES_USER: ${POSTGRES_USER:-clinic_user}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-clinic_pass}
    ports:
      - "${DB_PORT:-5432}:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U clinic_user -d clinic_db"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build:
      context: ./src/backend
      dockerfile: Dockerfile
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-clinic_db}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-clinic_user}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-clinic_pass}
      SPRING_JPA_HIBERNATE_DDL_AUTO: ${SPRING_JPA_HIBERNATE_DDL_AUTO:-update}
    depends_on:
      postgres:
        condition: service_healthy
    ports:
      - "${BACKEND_PORT:-8080}:8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/patients"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s

  frontend:
    build:
      context: ./src/frontend
      dockerfile: Dockerfile
    environment:
      VITE_API_BASE_URL: ${VITE_API_BASE_URL:-/api}
      VITE_PROXY_TARGET: ${VITE_PROXY_TARGET:-http://backend:8080}
    depends_on:
      backend:
        condition: service_healthy
    ports:
      - "${FRONTEND_PORT:-5173}:80"
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:80"]
      interval: 10s
      timeout: 5s
      retries: 5

  # DAST Scanner - OWASP ZAP
  zap:
    image: ghcr.io/zaproxy/zaproxy:stable
    container_name: zap-scanner
    depends_on:
      frontend:
        condition: service_healthy
      backend:
        condition: service_healthy
    volumes:
      - ./dast/zap-config:/zap/wrk:rw
      - ./dast/reports:/zap/reports:rw
    command: zap.sh -cmd -addonupdate -addoninstall pscanrulesBeta -addoninstall ascanrulesBeta
    networks:
      - default
    user: zap

volumes:
  postgres_data:

networks:
  default:
    driver: bridge
```

#### Directory Structure for DAST

```
project-root/
├── dast/
│   ├── zap-config/
│   │   ├── automation-plan.yaml      # ZAP Automation Framework configuration
│   │   ├── context.xml               # ZAP context (scope, exclusions)
│   │   └── policies/                 # Custom scan policies
│   ├── reports/                      # Generated DAST reports
│   ├── scripts/
│   │   ├── run-dast-scan.sh         # Local DAST execution script
│   │   └── wait-for-app.sh          # Health check script
│   └── baseline/                     # Baseline scan results for comparison
├── .github/workflows/
│   └── dast-nightly.yml             # CI/CD workflow for DAST
└── docs/
    └── dast-implementation-plan.md   # This document
```

### 2. ZAP Automation Framework Configuration

**File: `dast/zap-config/automation-plan.yaml`**

This configuration defines a comprehensive DAST scan using ZAP's Automation Framework:

```yaml
---
env:
  contexts:
    - name: "Clinic Management App"
      urls:
        - "http://frontend:80"
        - "http://backend:8080"
      includePaths:
        - "http://frontend:80/.*"
        - "http://backend:8080/api/.*"
      excludePaths:
        - "http://frontend:80/assets/.*"
        - "http://backend:8080/actuator/.*"
  parameters:
    failOnError: true
    failOnWarning: false
    progressToStdout: true

jobs:
  - type: passiveScan-config
    parameters:
      maxAlertsPerRule: 10
      scanOnlyInScope: true
      enableTags: false

  # Spider: Traditional crawler for static content and API discovery
  - type: spider
    parameters:
      context: "Clinic Management App"
      url: "http://frontend:80"
      maxDuration: 5
      maxDepth: 10
      maxChildren: 20
      acceptCookies: true

  # AJAX Spider: JavaScript-aware crawler for React SPA
  - type: spiderAjax
    parameters:
      context: "Clinic Management App"
      url: "http://frontend:80"
      maxDuration: 10
      maxCrawlDepth: 10
      numberOfBrowsers: 2
      browserId: "firefox-headless"
      clickDefaultElems: true
      clickElemsOnce: true

  # API Schema Import (optional - can be added if OpenAPI spec exists)
  # - type: openapi
  #   parameters:
  #     apiFile: /zap/wrk/openapi.yaml
  #     targetUrl: http://backend:8080
  #     context: "Clinic Management App"

  # Passive Scan Wait - ensure all passive rules run before active scan
  - type: passiveScan-wait
    parameters:
      maxDuration: 5

  # Active Scan: Attack testing
  - type: activeScan
    parameters:
      context: "Clinic Management App"
      policy: "API-and-Web-Service"
      maxRuleDurationInMins: 5
      maxScanDurationInMins: 30
      addQueryParam: false
      defaultPolicy: "Default Policy"
      delayInMs: 0
      handleAntiCSRFTokens: true
      injectPluginIdInHeader: false
      scanHeadersAllRequests: true
      threadPerHost: 2
    policyDefinition:
      defaultStrength: "MEDIUM"
      defaultThreshold: "MEDIUM"
      rules:
        # High priority OWASP Top 10 rules
        - id: 40012  # Cross Site Scripting (Reflected)
          strength: "HIGH"
          threshold: "LOW"
        - id: 40014  # Cross Site Scripting (Persistent)
          strength: "HIGH"
          threshold: "LOW"
        - id: 40018  # SQL Injection
          strength: "HIGH"
          threshold: "LOW"
        - id: 40019  # SQL Injection - PostgreSQL
          strength: "HIGH"
          threshold: "LOW"
        - id: 90019  # Server Side Code Injection
          strength: "HIGH"
          threshold: "MEDIUM"
        - id: 7     # Remote OS Command Injection
          strength: "HIGH"
          threshold: "LOW"
        - id: 42    # Path Traversal
          strength: "MEDIUM"
          threshold: "MEDIUM"
        - id: 6     # Path Traversal
          strength: "MEDIUM"
          threshold: "MEDIUM"
        - id: 41    # Source Code Disclosure
          strength: "MEDIUM"
          threshold: "MEDIUM"
        - id: 43    # External Redirect
          strength: "MEDIUM"
          threshold: "MEDIUM"
        - id: 10202 # Absence of Anti-CSRF Tokens
          strength: "LOW"
          threshold: "MEDIUM"
        # API-specific tests
        - id: 90034 # Cloud Metadata Attack
          strength: "MEDIUM"
          threshold: "MEDIUM"
        - id: 90035 # Server Side Template Injection
          strength: "HIGH"
          threshold: "MEDIUM"

  # Report Generation
  - type: report
    parameters:
      template: "traditional-html"
      reportDir: /zap/reports
      reportFile: "zap-report.html"
      reportTitle: "DAST Scan - Clinic Management App"
      reportDescription: "Comprehensive security scan of the clinic management application"
      displayReport: false

  - type: report
    parameters:
      template: "sarif-json"
      reportDir: /zap/reports
      reportFile: "zap-report.sarif"
      reportTitle: "DAST Scan - Clinic Management App"
      reportDescription: "SARIF format for GitHub Code Scanning"
      displayReport: false

  - type: report
    parameters:
      template: "traditional-json"
      reportDir: /zap/reports
      reportFile: "zap-report.json"
      reportTitle: "DAST Scan - Clinic Management App"
      displayReport: false
```

### 3. Scan Execution Script

**File: `dast/scripts/run-dast-scan.sh`**

```bash
#!/bin/bash
set -e

echo "Starting DAST scan for Clinic Management Application..."

# Configuration
COMPOSE_FILE="docker-compose.dast.yml"
REPORTS_DIR="./dast/reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Clean previous reports
echo "Cleaning previous reports..."
rm -rf ${REPORTS_DIR}/*
mkdir -p ${REPORTS_DIR}

# Start application stack
echo "Starting application stack..."
docker-compose -f ${COMPOSE_FILE} up -d postgres backend frontend

# Wait for services to be healthy
echo "Waiting for services to be ready..."
timeout 120 bash -c '
  until docker-compose -f docker-compose.dast.yml ps | grep -E "frontend.*healthy"; do
    echo "Waiting for frontend to be healthy..."
    sleep 5
  done
'

# Seed database with test data (if seeder exists)
echo "Seeding database with test data..."
docker-compose -f ${COMPOSE_FILE} exec -T backend curl -f http://localhost:8080/api/patients || true

# Run ZAP scan
echo "Starting ZAP scan..."
docker run --rm \
  --network "$(basename $(pwd))_default" \
  -v "$(pwd)/dast/zap-config:/zap/wrk:ro" \
  -v "$(pwd)/dast/reports:/zap/reports:rw" \
  ghcr.io/zaproxy/zaproxy:stable \
  zap.sh -cmd \
  -autorun /zap/wrk/automation-plan.yaml \
  -config api.disablekey=true

echo "DAST scan completed!"

# Generate summary
echo ""
echo "=== SCAN SUMMARY ==="
if [ -f "${REPORTS_DIR}/zap-report.json" ]; then
  python3 - <<EOF
import json
with open("${REPORTS_DIR}/zap-report.json", "r") as f:
    report = json.load(f)

alerts = report.get("site", [{}])[0].get("alerts", [])
risk_levels = {"High": 0, "Medium": 0, "Low": 0, "Informational": 0}

for alert in alerts:
    risk = alert.get("risk", "Informational")
    risk_levels[risk] = risk_levels.get(risk, 0) + 1

print(f"High Risk: {risk_levels['High']}")
print(f"Medium Risk: {risk_levels['Medium']}")
print(f"Low Risk: {risk_levels['Low']}")
print(f"Informational: {risk_levels['Informational']}")
print(f"Total Alerts: {sum(risk_levels.values())}")
EOF
fi

echo ""
echo "Reports available in: ${REPORTS_DIR}"
echo "  - zap-report.html (detailed HTML report)"
echo "  - zap-report.sarif (GitHub Code Scanning format)"
echo "  - zap-report.json (machine-readable format)"

# Cleanup
echo ""
read -p "Stop application stack? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
  docker-compose -f ${COMPOSE_FILE} down
fi
```

### 4. CI/CD Integration - GitHub Actions

**File: `.github/workflows/dast-nightly.yml`**

```yaml
name: DAST Nightly

on:
  schedule:
    - cron: "0 3 * * *"
  workflow_dispatch:

permissions:
  actions: read
  contents: read
  security-events: write

concurrency:
  group: dast-nightly
  cancel-in-progress: false

jobs:
  zap:
    name: OWASP ZAP
    runs-on: ubuntu-latest
    timeout-minutes: 45

    steps:
      - name: Check out repository
        uses: actions/checkout@v4

      - name: Make DAST scripts executable
        run: chmod +x dast/scripts/*.sh

      - name: Run nightly DAST scan
        env:
          COMPOSE_PROJECT_NAME: clinic-dast-nightly-${{ github.run_id }}
        run: ./dast/scripts/run-dast-scan.sh

      - name: Upload DAST reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: dast-nightly-reports
          path: |
            dast/reports/zap-report.html
            dast/reports/zap-report.json
            dast/reports/zap-report.sarif
          if-no-files-found: warn

      - name: Publish DAST findings to Security tab
        if: always() && hashFiles('dast/reports/zap-report.sarif') != ''
        continue-on-error: true
        uses: github/codeql-action/upload-sarif@v4
        with:
          sarif_file: dast/reports/zap-report.sarif
          category: dast-zap-nightly

      - name: Summarize DAST findings
        if: always() && hashFiles('dast/reports/zap-report.json') != ''
        run: |
          python3 - <<'PY'
          import json
          import os
          from pathlib import Path

          report = json.loads(Path("dast/reports/zap-report.json").read_text())
          summary_path = Path(os.environ["GITHUB_STEP_SUMMARY"])

          alerts = []
          for site in report.get("site", []):
              alerts.extend(site.get("alerts", []))

          counts = {"High": 0, "Medium": 0, "Low": 0, "Informational": 0}
          for alert in alerts:
              risk = alert.get("risk", "Informational")
              counts[risk] = counts.get(risk, 0) + 1

          lines = [
              "## DAST Scan Summary",
              "",
              "| Severity | Count |",
              "| --- | ---: |",
              f"| High | {counts.get('High', 0)} |",
              f"| Medium | {counts.get('Medium', 0)} |",
              f"| Low | {counts.get('Low', 0)} |",
              f"| Informational | {counts.get('Informational', 0)} |",
              f"| Total | {sum(counts.values())} |",
          ]

          summary_path.write_text("\n".join(lines) + "\n")
          PY
```

### 5. Optional: PR-Based DAST (Baseline Comparison)

For PR-based DAST, use a baseline comparison approach to only report NEW vulnerabilities introduced in the PR:

**File: `.github/workflows/dast-pr.yml`**

```yaml
name: DAST PR Check

on:
  pull_request:
    branches: [master, develop]
    paths:
      - 'src/**'
      - 'docker-compose*.yml'

permissions:
  contents: read
  pull-requests: write
  security-events: write

jobs:
  dast-baseline:
    name: DAST Baseline Scan
    runs-on: ubuntu-latest
    # Only run if PR changes application code
    if: contains(github.event.pull_request.labels.*.name, 'run-dast') || github.event.pull_request.draft == false

    steps:
      - name: Check out repository
        uses: actions/checkout@v4

      - name: Create reports directory
        run: mkdir -p dast/reports

      - name: Start application stack
        run: docker-compose -f docker-compose.dast.yml up -d

      - name: Wait for application
        run: |
          timeout 180 bash -c '
            until docker-compose -f docker-compose.dast.yml ps | grep -E "frontend.*healthy"; do
              sleep 10
            done
          '

      # Run ZAP baseline scan (faster, passive + limited active)
      - name: ZAP Baseline Scan
        uses: zaproxy/action-baseline@v0.13.0
        with:
          target: 'http://localhost:5173'
          rules_file_name: 'dast/zap-config/baseline-rules.tsv'
          cmd_options: '-a -j'
          fail_action: false
          allow_issue_writing: false

      - name: Upload baseline report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: dast-baseline-report
          path: |
            report_html.html
            report_json.json

      - name: Cleanup
        if: always()
        run: docker-compose -f docker-compose.dast.yml down -v
```

## Security Testing Focus Areas

Based on the application architecture, DAST should focus on:

### 1. Injection Vulnerabilities
- **SQL Injection**: Test all API parameters (patient name, date filters, IDs)
- **NoSQL Injection**: Though using PostgreSQL, test for ORM injection patterns
- **Command Injection**: Unlikely but test if any system commands are exposed

### 2. Cross-Site Scripting (XSS)
- **Reflected XSS**: Test URL parameters and API query strings
- **Stored XSS**: Test patient names, appointment details stored in database
- **DOM-based XSS**: React components rendering user data

### 3. API Security Issues
- **Mass Assignment**: Test if API accepts extra fields in POST/PUT
- **IDOR (Insecure Direct Object References)**: Test accessing other patients' data
- **HTTP Method Tampering**: Test unauthorized methods on endpoints
- **Lack of Rate Limiting**: Test for brute force susceptibility

### 4. Security Misconfigurations
- **CORS Policy**: Verify CORS headers are appropriate
- **Verbose Error Messages**: Check for stack traces in responses
- **Missing Security Headers**: Test for CSP, X-Frame-Options, etc.
- **Directory Listing**: Verify no sensitive directories exposed

### 5. Data Exposure
- **Sensitive Data in URLs**: Patient data in query strings
- **PII Leakage**: Patient information exposure
- **Excessive Data Exposure**: API returning more data than needed

## Findings Triage and Remediation Workflow

### 1. Automated Triage
```python
# Example: Filter false positives based on application knowledge
FALSE_POSITIVES = {
    "X-Content-Type-Options Header Missing": "Acceptable for dev environment",
    "Incomplete or No Cache-control Header Set": "Not applicable for API responses",
}

def is_false_positive(alert):
    return alert.get("name") in FALSE_POSITIVES
```

### 2. Severity Classification
- **Critical/High**: Block PR, immediate fix required
- **Medium**: Create GitHub issue, fix in next sprint
- **Low/Info**: Document, address opportunistically

### 3. Remediation Process
1. DAST scan runs → Findings uploaded to GitHub Security tab
2. Developer reviews findings in Security → Code Scanning alerts
3. For valid findings:
   - Create issue with details
   - Link to OWASP guidance
   - Assign to developer
4. Developer fixes → Commits reference issue
5. Re-run DAST to verify fix

## Comparison: SAST vs DAST

| Aspect | SAST (Current) | DAST (Planned) |
|--------|----------------|----------------|
| **Timing** | Pre-deployment (PR) | Post-deployment (Nightly) |
| **Access** | Source code required | Black-box testing |
| **Coverage** | All code paths | Only accessible endpoints |
| **False Positives** | Higher (hypothetical issues) | Lower (real runtime issues) |
| **Performance Impact** | Build time increase | Runtime environment required |
| **Authentication** | N/A | Requires session handling |
| **Tools** | SonarQube, Semgrep, CodeQL | OWASP ZAP |
| **Vulnerability Types** | Code-level (hardcoded secrets, patterns) | Runtime (injection, XSS, config) |

**Complementary Approach**: SAST catches issues in code before deployment, DAST validates security of the running application.

## Timeline and Milestones

### Phase 1: Setup and Configuration (Week 1)
- [ ] Create `docker-compose.dast.yml`
- [ ] Create DAST directory structure
- [ ] Write ZAP automation plan YAML
- [ ] Test local DAST execution with `run-dast-scan.sh`
- [ ] Verify reports are generated correctly

### Phase 2: CI/CD Integration (Week 1-2)
- [x] Create `.github/workflows/dast-nightly.yml`
- [x] Configure GitHub permissions for SARIF upload
- [ ] Test workflow execution on develop branch
- [ ] Verify SARIF findings appear in Security tab
- [ ] Fine-tune scan policies based on initial results

### Phase 3: Baseline and Optimization (Week 2)
- [ ] Run initial baseline scan
- [ ] Triage findings (identify false positives)
- [ ] Create exclusion rules for known false positives
- [ ] Optimize scan duration (currently 30 min max)
- [ ] Document common findings and remediation patterns

### Phase 4: Optional PR Integration (Week 3)
- [ ] Implement baseline comparison approach
- [ ] Create `.github/workflows/dast-pr.yml`
- [ ] Test on sample PR
- [ ] Evaluate whether PR-based DAST adds value vs nightly only

### Phase 5: Documentation and Training (Week 3)
- [ ] Update project README with DAST information
- [ ] Create developer guide for interpreting DAST findings
- [ ] Document triage process
- [ ] Present DAST implementation to team

## Metrics and KPIs

Track the following metrics to measure DAST effectiveness:

1. **Scan Coverage**
   - Number of URLs discovered by spider
   - API endpoint coverage (should be 10: 5 patient + 5 appointment endpoints)
   - React route coverage (should be 2: /patients, /appointments)

2. **Vulnerability Detection**
   - Total vulnerabilities by severity (High/Medium/Low/Info)
   - Trend over time (should decrease as fixes are applied)
   - Mean time to remediation (MTTR)

3. **Operational Metrics**
   - Scan duration (target: < 30 minutes)
   - False positive rate (target: < 20%)
   - Scan success rate (target: > 95%)

4. **Integration Metrics**
   - SARIF upload success rate
   - Developer engagement (issues created/resolved)
   - Remediation rate (% of findings fixed within 30 days)

## Risk Assessment and Limitations

### Limitations of DAST
1. **No Source Code Visibility**: Cannot detect code-level issues like hardcoded secrets
2. **Coverage Dependent on Crawling**: May miss endpoints not discoverable through UI
3. **Runtime Environment Required**: More complex setup than SAST
4. **Longer Execution Time**: Active scans can take 30+ minutes
5. **No Business Logic Testing**: Cannot detect complex authorization flaws

### Mitigation Strategies
1. **Combine with SAST**: Use both SAST and DAST for comprehensive coverage
2. **Manual API Endpoint Seeding**: Provide ZAP with API specification (OpenAPI)
3. **Regular Baseline Updates**: Update exclusions as application evolves
4. **Incremental Scanning**: Use PR baseline scans for quick feedback
5. **Manual Penetration Testing**: Supplement with annual manual security testing

## Cost-Benefit Analysis

### Costs
- **Infrastructure**: Minimal (Docker-based, runs on GitHub Actions)
- **Maintenance**: ~2-4 hours/month for triage and optimization
- **Execution Time**: ~30 minutes/day for nightly scans

### Benefits
- **Early Detection**: Find runtime vulnerabilities before production
- **Compliance**: Meet security testing requirements for regulated industries
- **Reduced Risk**: Decrease likelihood of security incidents
- **Confidence**: Validate that code-level fixes (SAST) work at runtime

**ROI**: High - Low cost with significant risk reduction

## Alternative Approaches Considered

### 1. Burp Suite Professional
- **Pros**: More advanced features, better false positive filtering
- **Cons**: Paid license ($449/year), less automation-friendly
- **Decision**: Rejected due to cost and OWASP ZAP meeting requirements

### 2. Nuclei
- **Pros**: Fast, template-based, great for known CVEs
- **Cons**: Less comprehensive for custom app testing, no crawling
- **Decision**: Could be added later for CVE detection

### 3. IAST (e.g., Contrast Security)
- **Pros**: Instrumentation provides better accuracy than DAST
- **Cons**: Requires code changes, vendor lock-in, high cost
- **Decision**: Out of scope for current project

### 4. Manual Penetration Testing Only
- **Pros**: Most thorough, finds business logic flaws
- **Cons**: Expensive, not continuous, doesn't scale
- **Decision**: DAST provides continuous testing; manual testing recommended annually

## References

1. **OWASP ZAP Documentation**: https://www.zaproxy.org/docs/
2. **ZAP Automation Framework**: https://www.zaproxy.org/docs/desktop/addons/automation-framework/
3. **OWASP Top 10 2021**: https://owasp.org/Top10/
4. **DAST Best Practices**: https://owasp.org/www-community/Vulnerability_Scanning_Tools
5. **GitHub Code Scanning SARIF**: https://docs.github.com/en/code-security/code-scanning/integrating-with-code-scanning/sarif-support-for-code-scanning
6. **Course Material**: `powerpoints/SES_04_DAST.pdf`
7. **Project Requirements**: `docs/SES_Group_project.pdf`

## Appendix A: Quick Start Guide

### Running DAST Locally

```bash
# 1. Ensure Docker is running
docker --version

# 2. Create required directories
mkdir -p dast/reports dast/zap-config

# 3. Copy the automation plan YAML to dast/zap-config/automation-plan.yaml

# 4. Make script executable
chmod +x dast/scripts/run-dast-scan.sh

# 5. Run the scan
./dast/scripts/run-dast-scan.sh

# 6. View the report
firefox dast/reports/zap-report.html
```

### Interpreting DAST Reports

**HTML Report Sections:**
1. **Summary**: High-level overview of findings by risk
2. **Alerts**: Detailed vulnerability descriptions
   - **Risk**: High/Medium/Low/Informational
   - **Confidence**: Reliability of the finding
   - **CWE**: Common Weakness Enumeration ID
   - **WASC**: Web Application Security Consortium classification
   - **Solution**: Remediation guidance
3. **Appendix**: Request/response details for reproduction

**Prioritization:**
1. Fix all "High Risk + High Confidence" first
2. Then "High Risk + Medium Confidence"
3. Review "Medium Risk" based on business impact
4. "Low/Info" are enhancement opportunities

## Appendix B: Common False Positives

| Alert | Reason | Action |
|-------|--------|--------|
| X-Content-Type-Options Missing | Development environment | Add to production config |
| Cookie without SameSite Attribute | No authentication cookies | Ignore or fix when auth added |
| CSP Missing | React app uses inline scripts | Configure CSP for Vite |
| Timestamp Disclosure | Not sensitive information | Acknowledge |

## Sign-Off

**Document Version**: 1.0
**Last Updated**: 2026-03-28
**Author**: Security Engineering Team
**Review Status**: Draft - Awaiting Implementation

---

**Next Steps**: Proceed with Phase 1 implementation and create the required files and configurations as outlined in this plan.
