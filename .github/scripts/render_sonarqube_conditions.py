#!/usr/bin/env python3

import json
import sys
from pathlib import Path


METRIC_DESCRIPTIONS = {
    "new_reliability_rating": ("No bugs in new code", "Bugs found"),
    "new_security_rating": ("No vulnerabilities in new code", "Vulnerabilities found"),
    "new_security_hotspots_reviewed": ("All security hotspots reviewed", "Security hotspots need review"),
    "new_maintainability_rating": ("No code smells in new code", "Code smells found"),
    "new_duplicated_lines_density": ("Less than 3% code duplication", "Too much duplicated code"),
    "new_software_quality_reliability_rating": ("No bugs in new code", "Bugs found"),
    "new_software_quality_security_rating": ("No vulnerabilities in new code", "Vulnerabilities found"),
    "new_software_quality_maintainability_rating": ("No code smells in new code", "Code smells found"),
}

RATING_MAP = {
    "1": "A (0 issues)",
    "2": "B (minor)",
    "3": "C (major)",
    "4": "D (critical)",
    "5": "E (blocker)",
}


def render_conditions(conditions):
    lines = []

    for condition in conditions:
        metric = condition.get("metricKey", "unknown")
        status = condition.get("status", "UNKNOWN")
        actual = condition.get("actualValue", "-")

        if metric in METRIC_DESCRIPTIONS:
            passed_text, failed_text = METRIC_DESCRIPTIONS[metric]
            if status == "OK":
                lines.append(f"- {passed_text}")
                continue

            lines.append(f"- **FAILED**: {failed_text}")
            if metric == "new_duplicated_lines_density":
                lines.append(f"  - Duplication: {actual}%")
            elif "rating" in metric:
                lines.append(f"  - Rating: {RATING_MAP.get(actual, actual)}")
            elif metric == "new_security_hotspots_reviewed":
                lines.append(f"  - Reviewed: {actual}%")
            continue

        status_text = "Passed" if status == "OK" else "**FAILED**"
        lines.append(f"- {metric}: {status_text}")

    return lines


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: render_sonarqube_conditions.py <quality_gate_json>", file=sys.stderr)
        return 1

    qg_data = json.loads(Path(sys.argv[1]).read_text())
    conditions = qg_data.get("projectStatus", {}).get("conditions", [])

    for line in render_conditions(conditions):
        print(line)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
