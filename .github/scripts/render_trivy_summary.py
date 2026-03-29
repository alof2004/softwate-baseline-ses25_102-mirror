#!/usr/bin/env python3

import json
import sys
from pathlib import Path


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: render_trivy_summary.py <title> <findings_json>", file=sys.stderr)
        return 1

    title = sys.argv[1]
    findings_path = Path(sys.argv[2])
    findings = json.loads(findings_path.read_text())

    print(f"### {title}\n")
    print("| Package | Vulnerability | Severity | Installed | Fixed |")
    print("| --- | --- | --- | --- | --- |")

    count = 0
    for result in findings.get("Results", []):
        for vuln in result.get("Vulnerabilities", []):
            if vuln.get("Severity") not in {"HIGH", "CRITICAL"}:
                continue

            pkg = vuln.get("PkgName", "unknown")
            vuln_id = vuln.get("VulnerabilityID", "unknown")
            severity = vuln.get("Severity", "unknown")
            installed = vuln.get("InstalledVersion", "unknown")
            fixed = vuln.get("FixedVersion", "not available")
            print(f"| {pkg} | {vuln_id} | {severity} | {installed} | {fixed} |")
            count += 1
            if count >= 20:
                break
        if count >= 20:
            break

    if count == 0:
        print("| No HIGH/CRITICAL vulnerabilities found | - | - | - | - |")
    elif count >= 20:
        print("\n_Showing top 20 vulnerabilities. See full report in artifacts._\n")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
