#!/usr/bin/env python3

import argparse
import json
from pathlib import Path


SEVERITY_ORDER = {
    "Informational": 0,
    "Low": 1,
    "Medium": 2,
    "High": 3,
    "Critical": 4,
}


def parse_severity(alert: dict) -> str:
    riskdesc = alert.get("riskdesc") or alert.get("risk") or ""
    severity = riskdesc.split(" ", 1)[0] if riskdesc else "Informational"
    if severity == "Info":
        return "Informational"
    if severity in SEVERITY_ORDER:
        return severity
    return "Informational"


def summarize(report: dict):
    counts = {severity: 0 for severity in SEVERITY_ORDER}
    alert_types = {}

    for site in report.get("site", []):
        for alert in site.get("alerts", []):
            severity = parse_severity(alert)
            key = (alert.get("pluginid"), alert.get("name"), severity)
            if key not in alert_types:
                alert_types[key] = {
                    "name": alert.get("name", "Unknown alert"),
                    "severity": severity,
                    "instances": 0,
                }
                counts[severity] += 1

            alert_types[key]["instances"] += len(alert.get("instances", []))

    ordered_alerts = sorted(
        alert_types.values(),
        key=lambda item: (
            -SEVERITY_ORDER.get(item["severity"], -1),
            -item["instances"],
            item["name"],
        ),
    )

    return counts, ordered_alerts


def render_summary(counts: dict, alerts: list[dict], target_ref: str) -> list[str]:
    total = sum(counts.values())
    lines = [
        "## DAST Scan Summary",
        "",
        f"- Target ref: `{target_ref}`",
        "",
        "| Severity | Count |",
        "| --- | ---: |",
        f"| High | {counts['High']} |",
        f"| Medium | {counts['Medium']} |",
        f"| Low | {counts['Low']} |",
        f"| Informational | {counts['Informational']} |",
        f"| Total | {total} |",
    ]

    if alerts:
        lines.extend(
            [
                "",
                "### Alert Types",
                "",
                "| Alert | Severity | Instances |",
                "| --- | --- | ---: |",
            ]
        )
        for alert in alerts:
            lines.append(
                f"| {alert['name']} | {alert['severity']} | {alert['instances']} |"
            )

    return lines


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", required=True)
    parser.add_argument("--summary-file", required=True)
    parser.add_argument("--target-ref", required=True)
    parser.add_argument("--fail-on", default="High")
    args = parser.parse_args()

    report = json.loads(Path(args.report).read_text())
    summary_path = Path(args.summary_file)
    counts, alerts = summarize(report)
    lines = render_summary(counts, alerts, args.target_ref)

    fail_threshold = SEVERITY_ORDER.get(args.fail_on, SEVERITY_ORDER["High"])
    blocking = [
        severity
        for severity, count in counts.items()
        if count > 0 and SEVERITY_ORDER[severity] >= fail_threshold
    ]

    if blocking:
        lines.extend(
            [
                "",
                "---",
                "",
                f"**Gate Status:** Failed. Blocking severities found: {', '.join(sorted(blocking, key=SEVERITY_ORDER.get, reverse=True))}.",
            ]
        )
        summary_path.write_text("\n".join(lines) + "\n")
        return 1

    lines.extend(
        [
            "",
            "**Gate Status:** Passed. No blocking ZAP findings detected.",
        ]
    )
    summary_path.write_text("\n".join(lines) + "\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
