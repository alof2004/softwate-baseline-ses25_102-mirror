#!/usr/bin/env python3

import json
import sys
from pathlib import Path


LEVEL_ORDER = {
    "error": 2,
    "warning": 1,
    "note": 0,
    "none": 0,
}

LEVEL_LABELS = {
    "error": "Error",
    "warning": "Warning",
    "note": "Note",
    "none": "Note",
}


def safe_text(value, default: str) -> str:
    if value in (None, ""):
        return default
    return str(value).replace("\n", " ").strip()


def load_results(report_path: Path):
    findings = []
    data = json.loads(report_path.read_text())

    for run in data.get("runs", []):
        rules = {
            rule.get("id"): rule
            for rule in run.get("tool", {}).get("driver", {}).get("rules", [])
            if rule.get("id")
        }

        for result in run.get("results", []):
            rule_id = safe_text(result.get("ruleId"), "unknown")
            level = safe_text(result.get("level"), "",).lower() or safe_text(
                rules.get(rule_id, {}).get("defaultConfiguration", {}).get("level"),
                "note",
            ).lower()
            location = result.get("locations", [{}])[0].get("physicalLocation", {})
            findings.append(
                {
                    "rule_id": rule_id,
                    "level": level,
                    "message": safe_text(
                        result.get("message", {}).get("text"), "no description"
                    ),
                    "file": safe_text(
                        location.get("artifactLocation", {}).get("uri"), "unknown"
                    ),
                    "line": safe_text(location.get("region", {}).get("startLine"), "0"),
                }
            )

    findings.sort(
        key=lambda finding: (
            -LEVEL_ORDER.get(finding["level"], -1),
            finding["rule_id"],
            finding["file"],
        )
    )
    return findings


def grouped_findings(findings):
    groups = {}
    for finding in findings:
        key = (finding["rule_id"], finding["level"], finding["message"])
        group = groups.setdefault(
            key,
            {
                "rule_id": finding["rule_id"],
                "level": finding["level"],
                "message": finding["message"],
                "locations": [],
            },
        )
        group["locations"].append(f"`{finding['file']}:{finding['line']}`")

    ordered = sorted(
        groups.values(),
        key=lambda finding: (
            -LEVEL_ORDER.get(finding["level"], -1),
            finding["rule_id"],
            finding["locations"][0],
        ),
    )
    return ordered


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: render_semgrep_summary.py <title> <sarif_report>", file=sys.stderr)
        return 1

    title = sys.argv[1]
    report_path = Path(sys.argv[2])
    findings = load_results(report_path)
    grouped = grouped_findings(findings)

    counts = {"error": 0, "warning": 0, "note": 0}
    for finding in findings:
        if finding["level"] == "error":
            counts["error"] += 1
        elif finding["level"] == "warning":
            counts["warning"] += 1
        else:
            counts["note"] += 1

    status = "Passed" if not findings else "Findings Reported"

    print(f"### {title}\n")
    print(f"**Status:** {status}\n")
    print("| Severity | Count |")
    print("| --- | ---: |")
    print(f"| Error | {counts['error']} |")
    print(f"| Warning | {counts['warning']} |")
    print(f"| Note | {counts['note']} |")
    print(f"| Total | {len(findings)} |")

    if findings:
        print("\n**Top Findings**\n")
        for finding in grouped[:10]:
            level = LEVEL_LABELS.get(finding["level"], finding["level"].title())
            print(
                f"- **{level}** `{finding['rule_id']}`: {finding['message']}"
            )
            print(f"  Locations: {', '.join(finding['locations'][:3])}")
            if len(finding["locations"]) > 3:
                remaining = len(finding["locations"]) - 3
                print(f"  Additional matches: {remaining}")

        if len(grouped) > 10:
            print(
                f"\n_Showing the first 10 grouped findings. See the SARIF artifact for the complete result set._"
            )
    else:
        print("\nNo Semgrep findings were reported.")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
