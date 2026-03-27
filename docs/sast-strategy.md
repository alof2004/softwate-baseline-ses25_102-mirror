# SAST Strategy

## What is SAST?

**Static Application Security Testing (SAST)** analyzes source code, bytecode, or binaries for security vulnerabilities without executing the application. SAST tools examine code patterns, data flows, and configurations to identify potential security issues early in the development lifecycle.

### Key Characteristics

- **White-box testing**: Requires access to source code
- **Early detection**: Finds issues during development, before deployment
- **Comprehensive coverage**: Scans entire codebase, including unused code paths
- **No runtime required**: Analyzes code statically without execution
- **Language-specific**: Different tools specialize in different languages

### What SAST Detects

- **Code vulnerabilities**: SQL injection, XSS, path traversal, command injection
- **Secrets exposure**: API keys, passwords, tokens in code/commits
- **Dependency vulnerabilities**: Known CVEs in third-party libraries
- **Misconfigurations**: Infrastructure-as-code (IaC) security issues
- **Coding anti-patterns**: Use of deprecated/dangerous functions

### SAST vs Other Security Testing

| Type | When | What It Finds | Limitations |
| --- | --- | --- | --- |
| **SAST** | Development (pre-runtime) | Code vulnerabilities, secrets, dependencies | Can't detect runtime issues, business logic flaws |
| **DAST** | Testing/staging (runtime) | Runtime vulnerabilities, authentication issues | No source code visibility, limited coverage |
| **IAST** | Runtime (with instrumentation) | Runtime + code context | Requires running application |
| **SCA** | Development/build | Dependency vulnerabilities, license issues | Only scans third-party code |

SAST is most effective when combined with other testing methods (DAST, penetration testing, code review).

## Purpose

This repository implements a multi-layered SAST approach integrated into different stages of the development lifecycle. The strategy balances speed and thoroughness by using fast, high-signal tools for rapid feedback during PRs, while running comprehensive deep scans nightly.

The design principles are:

- **Fast PR feedback**: Keep PR checks under 5 minutes to avoid blocking developers
- **High signal**: Block only on high-confidence, high-severity findings
- **Comprehensive coverage**: Run deep scans nightly to catch issues that require more analysis time
- **Defense in depth**: Layer multiple specialized tools rather than relying on a single scanner

## Tool Selection Rationale

### Why Multiple Tools

No single scanner excels at all security domains. This strategy uses specialized tools:

- **Gitleaks** - Fast, accurate secret detection in git history
- **Semgrep** - Fast pattern-based source code analysis with good language coverage
- **SonarQube** - Comprehensive code quality and security analysis for Java and JavaScript
- **CodeQL** - Deep semantic analysis (slower, more accurate)
- **Trivy** - Modern, fast vulnerability scanner for dependencies and IaC misconfigurations
- **ESLint Security** - Frontend-specific security patterns integrated into linting workflow
- **SpotBugs + FindSecBugs** - Deep Java bytecode analysis
- **npm audit** - Frontend dependency vulnerabilities

### Why Not Rely On One Scanner

Each tool has strengths and blind spots:
- CodeQL provides deep semantic analysis but takes 10-15 minutes
- Semgrep is fast but pattern-based (less context-aware than CodeQL)
- Trivy excels at dependency scanning and IaC misconfigurations but doesn't analyze source code
- Gitleaks specializes in secret detection but not code vulnerabilities
- SonarQube provides comprehensive quality metrics but may miss advanced security patterns

Layering specialized tools provides better coverage and reduces false negatives.

## Lifecycle Placement

### PR Checks (Fast - Target < 5 minutes)

**Goal**: High-signal gates that catch critical issues without slowing development

| Tool | Purpose | Blocking Threshold |
| --- | --- | --- |
| **Gitleaks** | Secrets in changed commits | Any finding |
| **Semgrep** | Fast source code patterns (`p/security-audit` + custom rules) | ERROR severity |
| **SonarQube** | Code quality & security (Java + JavaScript) | Quality gate failure |
| **ESLint Security** | Frontend security anti-patterns | Linting errors |
| **npm audit** | Frontend dependency CVEs | HIGH/CRITICAL in production deps |
| **Trivy** | Dependencies & IaC misconfigurations | HIGH/CRITICAL |

**Configuration Files:**
- `.github/workflows/sast.yml`
- `.semgrep.yml`
- `sonar-project.properties`
- `src/frontend/eslint.config.js`
- `trivy.yaml`

### Nightly Scans (Comprehensive - 20-40 minutes)

**Goal**: Deep security assessment without time pressure

| Tool | Purpose | Blocking Threshold |
| --- | --- | --- |
| **CodeQL** | Deep semantic analysis (Java + JavaScript) | Security findings |
| **Semgrep** | Full rulesets (OWASP Top 10, Java, JavaScript) | ERROR/WARNING severity |
| **SpotBugs + FindSecBugs** | Deep Java bytecode analysis | Any bug |

**Note:** Gitleaks, Trivy, and npm audit are intentionally excluded from nightly scans because they already run on every PR, providing immediate feedback without duplication. With proper branch protection, redundant nightly scans add no value.

**Configuration Files:**
- `.github/workflows/sast-nightly.yml`
- `src/backend/pom.xml` (security-sast profile)

### Pre-commit Hooks (Developer Local)

**Goal**: Fast local feedback before commit

| Tool | Purpose | Blocking |
| --- | --- | --- |
| **Gitleaks** | Staged secrets | Yes |
| **Semgrep** | Fast security patterns | ERROR severity |

**Installation:**
```bash
python3 -m pip install --user pre-commit
pre-commit install
```

**Run manually:**
```bash
pre-commit run --all-files
```

**Configuration:**
- `.pre-commit-config.yaml`

**Note:** Pre-commit hooks are optional/bypassable, so PR checks remain the enforcement gate.

## Blocking Thresholds

The strategy uses strict but narrow blocking criteria to avoid alert fatigue:

| Stage | What Blocks |
| --- | --- |
| **PR** | Gitleaks (any), Semgrep (ERROR), SonarQube (quality gate), ESLint (errors), npm audit (HIGH/CRITICAL prod deps), Trivy (HIGH/CRITICAL) |
| **Nightly** | CodeQL (security findings), Semgrep (ERROR/WARNING), SpotBugs (any) |
| **Pre-commit** | Gitleaks (any), Semgrep (ERROR) |

Lower-severity findings appear in reports but don't block builds. Redundant scans are eliminated to reduce noise and focus on high-signal findings.

## Repository Layout

### Configuration Files

- `.pre-commit-config.yaml` - Local hook definitions
- `.gitleaks.toml` - Gitleaks config with project allowlists
- `.semgrep.yml` - Custom Semgrep rules
- `.semgrepignore` - Semgrep exclusions
- `sonar-project.properties` - SonarQube project configuration
- `trivy.yaml` - Trivy configuration
- `.trivyignore` - Trivy suppression list
- `src/frontend/eslint.config.js` - ESLint with security plugin
- `src/backend/pom.xml` - Maven `security-sast` profile (SpotBugs + FindSecBugs)

### Workflow Files

- `.github/workflows/sast.yml` - PR checks
- `.github/workflows/sast-nightly.yml` - Nightly comprehensive scan

## Triage Policy

Every finding must be classified:

1. **True positive**: Real issue → Fix it
2. **False positive**: Scanner is wrong → Suppress narrowly with documentation
3. **Accepted risk**: Real but tolerated → Document the decision

### Suppression Guidelines

- Use tool-specific suppression mechanisms (`.trivyignore`, `.semgrepignore`)
- Keep suppressions narrow (specific CVE/check + justification comment)
- Never globally weaken thresholds to make pipelines green
- Document all suppressions with business/technical justification

## Environment Requirements

### Local Development

**Pre-commit hooks:**
- Python 3
- Git

**Backend deep scans:**
- JDK 21 (compiler, not just runtime)
- Maven

**Frontend:**
- Node.js 20+
- npm

### CI/CD

All tools run in GitHub Actions with:
- Ubuntu latest
- Pre-installed Docker for Gitleaks/Trivy containers
- Node.js 20 for frontend
- JDK 21 (Temurin) for backend

## Local Runbook

### Install Pre-commit Hooks

```bash
python3 -m pip install --user pre-commit
pre-commit install
```

If blocked by externally-managed Python environment, use a virtual environment or `pipx`.

### Run Pre-commit Manually

```bash
pre-commit run --all-files
```

### Backend Deep Scan (SpotBugs + FindSecBugs)

```bash
cd src/backend
javac -version  # Confirm JDK 21 compiler
./mvnw -Psecurity-sast -DskipTests verify
```

### Frontend Security Checks

```bash
cd src/frontend
npm install
npm run lint              # ESLint with security rules
npm audit --omit=dev --audit-level=high
```

### Run Trivy Locally

```bash
# Filesystem scan (dependencies & misconfigurations)
docker run --rm -v "$PWD:/repo" -w /repo \
  aquasec/trivy:latest fs --config trivy.yaml .

# Backend dependency scan only
docker run --rm -v "$PWD:/repo" -w /repo \
  aquasec/trivy:latest fs --scanners vuln src/backend
```

## Performance Targets

| Stage | Target Time | Typical Tools |
| --- | --- | --- |
| Pre-commit | < 30 seconds | Gitleaks, Semgrep (diff only) |
| PR Checks | < 5 minutes | All PR tools in parallel |
| Nightly | 20-40 minutes | All comprehensive scans |

## SAST Limitations

SAST cannot reliably detect:

- **Business logic flaws** (authorization bypasses, improper workflows)
- **Runtime behavior** (race conditions, memory issues in production)
- **Authentication/session issues** (weak session management)
- **Infrastructure misconfigurations** (cloud IAM, network policies)
- **API abuse** (rate limiting, input validation edge cases)

**Complement with:**
- DAST (dynamic testing)
- Manual security review
- Threat modeling
- Penetration testing

## Success Metrics

Track these metrics to measure SAST effectiveness:

- **PR failure rate** - Should be low (<10%) once baseline is clean
- **Time to remediate** - How fast findings are fixed
- **False positive rate** - Track and suppress to maintain developer trust
- **Coverage** - % of code scanned by at least one tool
- **Drift detection** - New vulnerabilities caught in nightly scans

## Branch Protection

To enforce SAST checks, configure branch protection rules on the `master` branch:

**Git Workflow:**
- Development happens on `develop` branch
- PRs are created FROM `develop` TO `master`
- SAST checks run on all PRs targeting `master`

**Required Status Checks** (6 checks must pass):
1. Gitleaks
2. Semgrep (Fast)
3. SonarQube Analysis
4. ESLint Security
5. Frontend Dependency Audit (npm audit)
6. Trivy Security Scan

**Additional Protections**:
- Require PR approval before merging
- Require conversation resolution
- Prevent direct pushes to master

**Complete Setup Guide**: [docs/branch-protection.md](branch-protection.md)

This ensures that **no code can be merged to production (master) without passing all security gates**.

## Maintenance

### Updating Tool Versions

- **Pre-commit hooks**: Automatic via `pre-commit autoupdate`
- **GitHub Actions**: Update version tags in workflow YAML files
- **Maven plugins**: Update in `pom.xml` properties section
- **npm packages**: Update in `package.json` devDependencies

### Handling New Findings

When a tool update introduces new findings:

1. Review each finding for legitimacy
2. Fix true positives promptly
3. Suppress false positives narrowly with documentation
4. Consider if the finding indicates a pattern requiring broader fixes

## SonarQube Setup

**SonarQube runs automatically in GitHub Actions PRs** as an ephemeral Docker service container. No external setup or secrets required!

### How It Works

Each PR automatically:
1. Spins up a fresh SonarQube container
2. Auto-configures the project
3. Scans the code
4. Blocks on quality gate failures
5. Destroys the container after the run

### Benefits

- ✅ **Zero setup** - Works immediately out of the box
- ✅ **No infrastructure** - No servers to maintain
- ✅ **No secrets required** - Everything is self-contained
- ✅ **Fresh per PR** - No state pollution between runs

### Advanced Setup (Optional)

For historical trend tracking and persistent metrics, see the comprehensive setup guide:

**[docs/sonarqube-setup.md](sonarqube-setup.md)**

The guide covers:
- Local Docker setup for development
- Persistent SonarQube deployment
- GitHub Actions integration with persistent instance
- Quality gate customization
- Troubleshooting

## References

- [SonarQube Documentation](https://docs.sonarqube.org/latest/)
- [Semgrep Rules Registry](https://semgrep.dev/r)
- [CodeQL Query Documentation](https://codeql.github.com/docs/)
- [Trivy Documentation](https://aquasecurity.github.io/trivy/)
- [Gitleaks Documentation](https://github.com/gitleaks/gitleaks)
- [FindSecBugs Rules](https://find-sec-bugs.github.io/bugs.htm)
- [ESLint Security Plugin](https://github.com/eslint-community/eslint-plugin-security)
