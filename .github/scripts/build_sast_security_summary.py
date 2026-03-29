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
        return [], {}

    runs = data.get("runs", [])
    results = []
    rules = {}
    for run in runs:
        driver = run.get("tool", {}).get("driver", {})
        for rule in driver.get("rules", []):
            rule_id = rule.get("id")
            if rule_id:
                rules[rule_id] = rule
        results.extend(run.get("results", []))
    return results, rules


def load_json(path: Path, default):
    try:
        return json.loads(path.read_text())
    except (OSError, json.JSONDecodeError):
        return default


def safe_text(value, default: str) -> str:
    if value in (None, ""):
        return default
    return str(value).replace("\n", " ").strip()


def advisory_text(*parts: str) -> str:
    filtered = [part for part in parts if part]
    return ", ".join(filtered) if filtered else "-"


def sarif_result_level(result: dict, rules: dict[str, dict]) -> str:
    level = safe_text(result.get("level"), "").lower()
    if level:
        return level

    rule = rules.get(result.get("ruleId"), {})
    return safe_text(rule.get("defaultConfiguration", {}).get("level"), "note").lower()


def semgrep_advisory_severity(result: dict, rules: dict[str, dict]) -> str:
    level = sarif_result_level(result, rules)
    if level == "warning":
        return "MEDIUM"
    if level in {"note", "none"}:
        return "LOW"
    return "UNKNOWN"


def trivy_rule_severity(result: dict, rules: dict[str, dict]) -> str:
    rule = rules.get(result.get("ruleId"), {})
    tags = {str(tag).upper() for tag in rule.get("properties", {}).get("tags", [])}
    for severity in ("CRITICAL", "HIGH", "MEDIUM", "LOW"):
        if severity in tags:
            return severity

    message = safe_text(result.get("message", {}).get("text"), "")
    for severity in ("CRITICAL", "HIGH", "MEDIUM", "LOW"):
        if f"Severity: {severity}" in message:
            return severity
    return "UNKNOWN"


def load_eslint_messages(report_path: Path):
    report = load_json(report_path, [])
    messages = []
    for entry in report:
        uri = safe_text(entry.get("filePath"), "unknown")
        for message in entry.get("messages", []):
            messages.append(
                {
                    "severity": "error" if message.get("severity") == 2 else "warning",
                    "ruleId": safe_text(message.get("ruleId"), "unknown"),
                    "message": safe_text(message.get("message"), "no description"),
                    "line": safe_text(message.get("line"), "0"),
                    "filePath": uri,
                }
            )
    return messages


def append_gitleaks(lines, report_path: Path):
    results, _ = load_sarif_results(report_path)
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
    results, _ = load_sarif_results(report_path)
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


def append_semgrep_advisory(lines, report_path: Path):
    results, rules = load_sarif_results(report_path)
    if not results:
        return

    medium = 0
    low = 0
    for result in results:
        severity = semgrep_advisory_severity(result, rules)
        if severity == "MEDIUM":
            medium += 1
        elif severity == "LOW":
            low += 1

    lines.extend(
        [
            "### Semgrep Advisory Findings",
            "",
            f"Non-blocking findings: **{len(results)}** ({advisory_text(f'{medium} medium' if medium else '', f'{low} low' if low else '')})",
            "",
        ]
    )

    for result in results[:10]:
        location = result.get("locations", [{}])[0].get("physicalLocation", {})
        uri = safe_text(location.get("artifactLocation", {}).get("uri"), "unknown")
        line = safe_text(location.get("region", {}).get("startLine"), "0")
        rule_id = safe_text(result.get("ruleId"), "unknown")
        message = safe_text(result.get("message", {}).get("text"), "no description")
        severity = semgrep_advisory_severity(result, rules).title()
        lines.append(f"- **{severity}** `[{rule_id}]` {message} in `{uri}:{line}`")

    if len(results) > 10:
        lines.extend(["", "_Showing first 10 findings. See artifacts for the full advisory report._"])
    lines.append("")


def append_trivy(lines, report_paths):
    fs_results, _ = load_sarif_results(report_paths[0]) if report_paths[0].is_file() else ([], {})
    backend_results, _ = load_sarif_results(report_paths[1]) if report_paths[1].is_file() else ([], {})
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


def append_trivy_advisory(lines, report_paths):
    findings = []
    severity_counts = {"MEDIUM": 0, "LOW": 0}

    for report_path in report_paths:
        if not report_path.is_file():
            continue
        results, rules = load_sarif_results(report_path)
        for result in results:
            severity = trivy_rule_severity(result, rules)
            if severity in severity_counts:
                severity_counts[severity] += 1
            findings.append((severity, result))

    if not findings:
        return

    advisory_summary = advisory_text(
        f"{severity_counts['MEDIUM']} medium" if severity_counts["MEDIUM"] else "",
        f"{severity_counts['LOW']} low" if severity_counts["LOW"] else "",
    )

    lines.extend(
        [
            "### Trivy Advisory Findings",
            "",
            f"Non-blocking findings: **{len(findings)}** ({advisory_summary})",
            "",
        ]
    )

    for severity, result in findings[:10]:
        lines.append(
            f"- **{severity.title()}** `{safe_text(result.get('ruleId'), 'unknown')}`: {safe_text(result.get('message', {}).get('text'), 'no description')}"
        )

    if len(findings) > 10:
        lines.extend(["", "_Showing first 10 findings. See artifacts for the full advisory report._"])
    lines.append("")


def append_eslint(lines, report_path: Path, eslint_result: str):
    messages = load_eslint_messages(report_path)
    if not messages:
        return

    warning_count = sum(1 for message in messages if message["severity"] == "warning")
    error_count = sum(1 for message in messages if message["severity"] == "error")
    title = "### ESLint Findings" if eslint_result == "failure" else "### ESLint Advisory Findings"

    lines.extend(
        [
            title,
            "",
            f"Findings: **{len(messages)}** ({advisory_text(f'{error_count} error' if error_count else '', f'{warning_count} warning' if warning_count else '')})",
            "",
        ]
    )

    for message in messages[:10]:
        severity = message["severity"].title()
        lines.append(
            f"- **{severity}** `[{message['ruleId']}]` {message['message']} in `{message['filePath']}:{message['line']}`"
        )

    if len(messages) > 10:
        lines.extend(["", "_Showing first 10 findings. See artifacts for the full ESLint report._"])
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

    semgrep_advisory_results, semgrep_advisory_rules = load_sarif_results(
        reports_dir / "semgrep-pr-advisory.sarif"
    )
    semgrep_advisory_medium = sum(
        1
        for result in semgrep_advisory_results
        if semgrep_advisory_severity(result, semgrep_advisory_rules) == "MEDIUM"
    )
    semgrep_advisory_low = sum(
        1
        for result in semgrep_advisory_results
        if semgrep_advisory_severity(result, semgrep_advisory_rules) == "LOW"
    )

    eslint_messages = load_eslint_messages(reports_dir / "eslint-security.json")
    eslint_warning_count = sum(
        1 for message in eslint_messages if message["severity"] == "warning"
    )

    trivy_advisory_counts = {"MEDIUM": 0, "LOW": 0}
    for advisory_report in (
        reports_dir / "trivy-fs-advisory.sarif",
        reports_dir / "trivy-backend-advisory.sarif",
    ):
        results, rules = load_sarif_results(advisory_report)
        for result in results:
            severity = trivy_rule_severity(result, rules)
            if severity in trivy_advisory_counts:
                trivy_advisory_counts[severity] += 1

    semgrep_advisory_summary = advisory_text(
        f"{semgrep_advisory_medium} medium" if semgrep_advisory_medium else "",
        f"{semgrep_advisory_low} low" if semgrep_advisory_low else "",
    )
    eslint_advisory_summary = advisory_text(
        f"{eslint_warning_count} warning" if eslint_warning_count else ""
    )
    trivy_advisory_summary = advisory_text(
        f"{trivy_advisory_counts['MEDIUM']} medium" if trivy_advisory_counts["MEDIUM"] else "",
        f"{trivy_advisory_counts['LOW']} low" if trivy_advisory_counts["LOW"] else "",
    )

    lines = [
        "<!-- sast-security-summary -->",
        "## SAST Security Summary",
        "",
        "| Tool | Gate Status | Advisory Findings |",
        "| --- | --- | --- |",
        f"| Gitleaks | {format_status(gitleaks_result)} | - |",
        f"| Semgrep | {format_status(semgrep_result)} | {semgrep_advisory_summary} |",
        f"| ESLint Security | {format_status(eslint_result)} | {eslint_advisory_summary} |",
        f"| Trivy | {format_status(trivy_result)} | {trivy_advisory_summary} |",
        "",
        "_Advisory findings do not block the merge gate; they are surfaced for triage._",
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

    append_semgrep_advisory(lines, reports_dir / "semgrep-pr-advisory.sarif")
    append_eslint(lines, reports_dir / "eslint-security.json", eslint_result)
    append_trivy_advisory(
        lines,
        [
            reports_dir / "trivy-fs-advisory.sarif",
            reports_dir / "trivy-backend-advisory.sarif",
        ],
    )

    lines.extend(["---", "", "_This summary is automatically updated on each commit._"])
    output_path.write_text("\n".join(lines) + "\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
