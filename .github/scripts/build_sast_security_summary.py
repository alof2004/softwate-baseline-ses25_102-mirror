#!/usr/bin/env python3

import json
import os
import sys
from pathlib import Path


def format_status(result: str) -> str:
    return {
        "success": "Passed",
        "failure": "**Failed**",
        "skipped": "Skipped",
        "cancelled": "Cancelled",
    }.get(result, "Unknown")


def load_sarif_results(path: Path):
    try:
        data = json.loads(path.read_text())
    except (OSError, json.JSONDecodeError):
        return []

    runs = data.get("runs", [])
    results = []
    for run in runs:
        results.extend(run.get("results", []))
    return results


def safe_text(value, default: str) -> str:
    if value in (None, ""):
        return default
    return str(value).replace("\n", " ").strip()


def append_gitleaks(lines, report_path: Path):
    results = load_sarif_results(report_path)
    lines.extend(["### Gitleaks Findings", ""])

    if results:
        lines.extend([f"Found **{len(results)}** secret(s):", ""])
        for result in results[:20]:
            location = (
                result.get("locations", [{}])[0]
                .get("physicalLocation", {})
            )
            uri = safe_text(location.get("artifactLocation", {}).get("uri"), "unknown")
            line = safe_text(location.get("region", {}).get("startLine"), "0")
            rule_id = safe_text(result.get("ruleId"), "unknown")
            lines.append(f"- **{rule_id}** in `{uri}:{line}`")
        if len(results) > 20:
            lines.extend(["", "_Showing first 20 findings. See artifacts for complete report._"])
    else:
        lines.append("No findings details available.")
    lines.append("")


def append_semgrep(lines, report_path: Path):
    results = load_sarif_results(report_path)
    lines.extend(["### Semgrep Findings", ""])

    if results:
        lines.extend([f"Found **{len(results)}** issue(s):", ""])
        for result in results[:20]:
            location = (
                result.get("locations", [{}])[0]
                .get("physicalLocation", {})
            )
            uri = safe_text(location.get("artifactLocation", {}).get("uri"), "unknown")
            line = safe_text(location.get("region", {}).get("startLine"), "0")
            rule_id = safe_text(result.get("ruleId"), "unknown")
            message = safe_text(result.get("message", {}).get("text"), "no description")
            lines.append(f"- **{rule_id}**: {message} in `{uri}:{line}`")
        if len(results) > 20:
            lines.extend(["", "_Showing first 20 findings. See artifacts for complete report._"])
    else:
        lines.append("No findings details available.")
    lines.append("")


def append_trivy(lines, report_paths):
    fs_results = load_sarif_results(report_paths[0]) if report_paths[0].is_file() else []
    backend_results = load_sarif_results(report_paths[1]) if report_paths[1].is_file() else []
    total = len(fs_results) + len(backend_results)

    lines.extend(["### Trivy Findings", ""])
    if total > 0:
        lines.extend([f"Found **{total}** vulnerability/vulnerabilities:", ""])
        for result in fs_results[:10] + backend_results[:10]:
            rule_id = safe_text(result.get("ruleId"), "unknown")
            message = safe_text(result.get("message", {}).get("text"), "no description")
            level = safe_text(result.get("level"), "unknown")
            lines.append(f"- **{rule_id}**: {message} ({level})")
        if total > 20:
            lines.extend(["", "_Showing first 20 findings. See artifacts for complete report._"])
    else:
        lines.append("Trivy scan failed but no findings details available.")
    lines.append("")


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: build_sast_security_summary.py <output_file>", file=sys.stderr)
        return 1

    output_path = Path(sys.argv[1])
    reports_dir = Path(os.environ.get("SAST_REPORTS_DIR", "reports"))

    gitleaks_result = os.environ["GITLEAKS_RESULT"]
    semgrep_result = os.environ["SEMGREP_RESULT"]
    eslint_result = os.environ["ESLINT_RESULT"]
    trivy_result = os.environ["TRIVY_RESULT"]

    lines = [
        "<!-- sast-security-summary -->",
        "## SAST Security Summary",
        "",
        "| Tool | Status |",
        "| --- | --- |",
        f"| Gitleaks | {format_status(gitleaks_result)} |",
        f"| Semgrep | {format_status(semgrep_result)} |",
        f"| ESLint Security | {format_status(eslint_result)} |",
        f"| Trivy | {format_status(trivy_result)} |",
        "",
    ]

    if gitleaks_result == "failure":
        append_gitleaks(lines, reports_dir / "gitleaks-pr.sarif")

    if semgrep_result == "failure":
        append_semgrep(lines, reports_dir / "semgrep-pr.sarif")

    if trivy_result == "failure":
        append_trivy(
            lines,
            [
                reports_dir / "trivy-fs-results.sarif",
                reports_dir / "trivy-backend-results.sarif",
            ],
        )

    lines.extend(["---", "", "_This summary is automatically updated on each commit._"])
    output_path.write_text("\n".join(lines) + "\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
