#!/usr/bin/env python3

import base64
import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

from render_sonarqube_conditions import render_conditions


def fetch_json(url: str, token: str):
    auth = base64.b64encode(f"{token}:".encode()).decode()
    request = urllib.request.Request(
        url,
        headers={
            "Authorization": f"Basic {auth}",
            "Accept": "application/json",
        },
    )
    try:
        with urllib.request.urlopen(request) as response:
            return json.loads(response.read().decode())
    except (urllib.error.URLError, urllib.error.HTTPError, json.JSONDecodeError):
        return None


def metric_value(metrics_data, metric: str, default: str) -> str:
    measures = (metrics_data or {}).get("component", {}).get("measures", [])
    for measure in measures:
        if measure.get("metric") == metric:
            period = measure.get("period", {})
            return str(period.get("value", measure.get("value", default)))
    return default


def format_percent(value: str) -> str:
    return "N/A" if value == "N/A" else f"{value}%"


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: build_sonarqube_pr_comment.py <output_file>", file=sys.stderr)
        return 1

    output_path = Path(sys.argv[1])
    sonar_host_url = os.environ["SONAR_HOST_URL"]
    sonar_token = os.environ["SONAR_TOKEN"]
    scan_outcome = os.environ["SONAR_SCAN_OUTCOME"]
    gate_outcome = os.environ["SONAR_GATE_OUTCOME"]
    project_key = os.environ.get("SONAR_PROJECT_KEY", "clinic-management-app")

    quality_status = "UNKNOWN"
    qg_data = None
    metrics_data = None

    if scan_outcome == "success":
        qg_data = fetch_json(
            f"{sonar_host_url}/api/qualitygates/project_status?projectKey={project_key}",
            sonar_token,
        )
        if qg_data:
            quality_status = qg_data.get("projectStatus", {}).get("status", "UNKNOWN")

        metrics_data = fetch_json(
            f"{sonar_host_url}/api/measures/component?component={project_key}"
            "&metricKeys=bugs,vulnerabilities,code_smells,security_hotspots,coverage,"
            "duplicated_lines_density,ncloc,new_bugs,new_vulnerabilities,new_code_smells,"
            "new_security_hotspots,new_coverage,new_duplicated_lines_density,new_lines",
            sonar_token,
        )

    lines = [
        "<!-- sonarqube-pr-comment -->",
        "## SonarQube Analysis Report",
        "",
    ]

    if quality_status == "OK":
        lines.append("### Quality Gate: **Passed**")
    elif quality_status == "ERROR":
        lines.append("### Quality Gate: **Failed**")
    else:
        lines.append(f"### Quality Gate: **{quality_status}**")
    lines.append("")

    lines.extend(
        [
            "| Check | Status |",
            "| --- | --- |",
            f"| Scan execution | `{scan_outcome}` |",
            f"| Quality gate check | `{gate_outcome}` |",
            "",
        ]
    )

    if metrics_data:
        new_bugs = metric_value(metrics_data, "new_bugs", "0")
        new_vulnerabilities = metric_value(metrics_data, "new_vulnerabilities", "0")
        new_code_smells = metric_value(metrics_data, "new_code_smells", "0")
        new_hotspots = metric_value(metrics_data, "new_security_hotspots", "0")
        new_coverage = metric_value(metrics_data, "new_coverage", "N/A")
        new_duplications = metric_value(metrics_data, "new_duplicated_lines_density", "0.0")
        new_lines = metric_value(metrics_data, "new_lines", "0")

        total_bugs = metric_value(metrics_data, "bugs", "0")
        total_vulnerabilities = metric_value(metrics_data, "vulnerabilities", "0")
        total_code_smells = metric_value(metrics_data, "code_smells", "0")
        total_hotspots = metric_value(metrics_data, "security_hotspots", "0")
        total_coverage = metric_value(metrics_data, "coverage", "N/A")
        total_duplications = metric_value(metrics_data, "duplicated_lines_density", "0.0")
        total_ncloc = metric_value(metrics_data, "ncloc", "0")

        lines.extend(
            [
                "### Code Metrics",
                "",
                "#### New Code (This PR)",
                "| Metric | Value |",
                "| --- | --- |",
                f"| Bugs | **{new_bugs}** |",
                f"| Vulnerabilities | **{new_vulnerabilities}** |",
                f"| Code Smells | **{new_code_smells}** |",
                f"| Security Hotspots | **{new_hotspots}** |",
                f"| Coverage | **{format_percent(new_coverage)}** |",
                f"| Duplications | **{format_percent(new_duplications)}** |",
                f"| Lines of Code | **{new_lines}** |",
                "",
                "#### Overall Project",
                "| Metric | Value |",
                "| --- | --- |",
                f"| Bugs | {total_bugs} |",
                f"| Vulnerabilities | {total_vulnerabilities} |",
                f"| Code Smells | {total_code_smells} |",
                f"| Security Hotspots | {total_hotspots} |",
                f"| Coverage | {format_percent(total_coverage)} |",
                f"| Duplications | {format_percent(total_duplications)} |",
                f"| Lines of Code | {total_ncloc} |",
                "",
            ]
        )

    if qg_data:
        conditions = qg_data.get("projectStatus", {}).get("conditions", [])
        lines.extend(["### Quality Gate Requirements", ""])
        if conditions:
            lines.extend(render_conditions(conditions))
        else:
            lines.extend(
                [
                    "Your PR will be checked for:",
                    "",
                    "- No bugs in new code",
                    "- No vulnerabilities in new code",
                    "- All security hotspots reviewed",
                    "- No code smells in new code",
                    "- Less than 3% code duplication",
                    "",
                    "_These conditions will be evaluated when you add new code to this PR._",
                ]
            )
        lines.append("")

    output_path.write_text("\n".join(lines) + "\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
