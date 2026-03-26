# SAST Strategy

## Purpose

This repository must include SAST integrated into multiple stages of the development lifecycle. The chosen strategy follows the assignment requirement to secure the provided baseline without adding new application features.

The design goal is simple:

- catch high-signal security issues before they leave a developer machine
- block risky changes in pull requests
- run a deeper recurring scan that does not slow down everyday development

## Tooling Decisions

### Why this stack

- `Gitleaks` handles hardcoded secrets well and is fast enough for pre-commit and PR use.
- `Semgrep` gives fast local source-code feedback across the Java backend and the React frontend.
- `CodeQL` is the GitHub-native code scanner used in CI for Java and JavaScript analysis.
- `npm audit` covers third-party dependency risk in the frontend.
- `SpotBugs` with `FindSecBugs` adds deeper Java analysis than a fast diff-oriented rule engine.
- `OWASP Dependency-Check` covers backend dependency CVEs and supports a clear fail threshold through CVSS.

### Why not rely on one scanner

No single scanner covers secrets, dependency CVEs, and source-level code issues equally well. The stack is intentionally layered so each tool focuses on a narrow job with better signal.

### Why the blocking thresholds are strict but narrow

The course slides explicitly warn about alert fatigue. This repository therefore blocks only on high-confidence findings:

- any `Gitleaks` hit blocks immediately
- local `Semgrep` blocks only on `ERROR` severity findings
- CodeQL blocks through GitHub code-scanning alerts in CI
- `npm audit` blocks only on `high` and `critical` runtime vulnerabilities
- backend dependency scanning blocks only when `failBuildOnCVSS >= 7`

Lower-confidence or lower-severity findings still appear in reports, especially in the nightly workflow, but they do not stop every developer action.

## Lifecycle Placement

| Stage | Tools | Purpose | Blocking |
| --- | --- | --- | --- |
| Pre-commit | `Gitleaks`, `Semgrep` | Fast local feedback on staged changes | Yes |
| PR / push to `develop` | `Gitleaks`, `CodeQL`, `npm audit`, OWASP Dependency-Check | Protect the integration branch with centralized code and dependency scanning | Yes |
| Nightly / manual dispatch | `CodeQL`, full-history `Gitleaks`, `npm audit`, `SpotBugs` + `FindSecBugs`, OWASP Dependency-Check | Deeper recurring assessment and dependency drift detection | Yes, for high-confidence findings |

## Repo Layout

- `.pre-commit-config.yaml`: versioned local hooks
- `.gitleaks.toml`: project-specific allowlists on top of the default Gitleaks rules
- `.semgrep.yml`: project-specific Semgrep rules for local hooks
- `.semgrepignore`: local Semgrep exclusions for generated and third-party paths
- `.github/workflows/sast.yml`: PR and push SAST workflow
- `.github/workflows/sast-nightly.yml`: nightly and manual deep SAST workflow
- `src/backend/pom.xml`: backend `security-sast` Maven profile

## Triage Policy

Every finding must be classified into one of these buckets:

- `True positive`: real issue, fix it
- `False positive`: scanner is wrong in this code context, suppress narrowly and document why
- `Accepted risk`: issue is real, but the risk is intentionally tolerated and recorded

Suppressions must stay narrow:

- use `.gitleaks.toml` allowlists only for reviewed placeholders or intentional test material
- use scanner-specific suppression files or configuration only for specific reviewed findings
- do not weaken global thresholds just to make pipelines green

## Environment Requirements

### Local hooks

- Python 3 for `pre-commit`
- Git

`pre-commit` will manage the hook environments, including the pinned `Gitleaks` and `Semgrep` hook runtimes.

### GitHub code scanning

CodeQL runs in GitHub Actions for both PR/push and nightly execution. It covers:

- `java-kotlin` with a manual Maven build step for the backend
- `javascript-typescript` without a build step for the frontend

This split keeps local feedback fast while moving the authoritative CI code scan into GitHub's code-scanning platform.

### Backend build-based scans

The backend deep scan requires a real JDK 21 compiler, not only a Java 21 runtime.

During implementation, local inspection showed:

- `java -version` reported Java 21
- `javac -version` reported Java 17
- `/usr/lib/jvm/java-21-openjdk-amd64/bin/javac` was not present

That mismatch breaks Maven compilation for this project. For backend security scans, developers must make sure `JAVA_HOME` and `PATH` point to a JDK 21 installation.

## Local Runbook

Install the local hooks:

```bash
python3 -m pip install --user pre-commit
pre-commit install
```

If `pip install --user` is blocked by an externally-managed Python environment, use a virtual environment or `pipx` for the `pre-commit` installation instead.

Run the local hook set across the repository:

```bash
pre-commit run --all-files
```

Run the backend deep scan:

```bash
cd src/backend
javac -version
./mvnw -Psecurity-sast -DskipTests verify
```

Run only the backend dependency audit used by the PR workflow:

```bash
cd src/backend
javac -version
./mvnw -Psecurity-sast -DskipTests -Dsecurity.sast.skipSpotbugs=true verify
```

## SAST Limits

SAST is not a complete security assessment. It will not reliably find:

- business logic flaws
- runtime authentication/session problems
- deployment and infrastructure misconfigurations
- browser/runtime behavior that only appears during execution

That is why DAST remains necessary in the overall project.
