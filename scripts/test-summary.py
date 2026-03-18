#!/usr/bin/env python3
"""Aggregate JUnit XML test results into a summary table.

Usage:
    python3 scripts/test-summary.py [ROOT_DIR] [--format text|markdown]

Discovers JUnit XML files produced by Gradle, pytest, and Vitest, then prints
a per-component breakdown by test type (unit / integration / e2e).
"""
from __future__ import annotations

import argparse
import json
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

# Maps CI job names to the (component, test_type) entries they produce.
JOB_COMPONENT_MAP: dict[str, list[tuple[str, str]]] = {
    "kotlin-unit-tests": [
        ("common", "unit"), ("gateway", "unit"), ("notification-service", "unit"),
        ("position-service", "unit"), ("price-service", "unit"), ("rates-service", "unit"),
        ("risk-orchestrator", "unit"), ("regulatory-service", "unit"), ("audit-service", "unit"),
        ("reference-data-service", "unit"), ("volatility-service", "unit"),
        ("correlation-service", "unit"), ("schema-tests", "unit"),
    ],
    "kotlin-acceptance-tests": [
        ("gateway", "acceptance"), ("notification-service", "acceptance"),
        ("risk-orchestrator", "acceptance"), ("regulatory-service", "acceptance"),
        ("position-service", "acceptance"), ("audit-service", "acceptance"),
        ("price-service", "acceptance"), ("rates-service", "acceptance"),
        ("reference-data-service", "acceptance"), ("volatility-service", "acceptance"),
        ("correlation-service", "acceptance"),
    ],
    "kotlin-integration": [
        ("position-service", "integration"), ("price-service", "integration"),
        ("risk-orchestrator", "integration"), ("audit-service", "integration"),
        ("correlation-service", "integration"), ("rates-service", "integration"),
        ("reference-data-service", "integration"), ("volatility-service", "integration"),
        ("regulatory-service", "integration"),
    ],
    "end-to-end-tests": [("end2end-tests", "e2e")],
    "python-unit-tests": [("risk-engine", "unit")],
    "python-integration-tests": [("risk-engine", "integration")],
    "ui-build": [("ui", "unit")],
    "ui-e2e-tests": [("ui", "e2e")],
    "smoke-tests": [("smoke-tests", "e2e")],
}

# Pattern → (component extraction strategy, test type)
#   component is derived from the path segment before /build/ for Gradle,
#   or a fixed name for pytest/vitest.
PATTERNS = [
    # Gradle / local layout
    ("**/build/test-results/test/**/*.xml", "unit"),
    ("**/build/test-results/acceptanceTest/**/*.xml", "acceptance"),
    ("**/build/test-results/integrationTest/**/*.xml", "integration"),
    ("**/build/test-results/end2EndTest/**/*.xml", "e2e"),
    ("**/risk-engine/**/unit.xml", "unit"),
    ("**/risk-engine/**/integration.xml", "integration"),
    ("**/risk-engine/**/pytest.xml", "unit"),
    ("**/ui/test-results/e2e/junit.xml", "e2e"),
    ("**/ui/**/junit.xml", "unit"),
    # CI artifact layout (download-artifact@v4 preserves directory structure)
    ("**/unit-test-xml-*/**/*.xml", "unit"),
    ("**/acceptance-test-xml-*/**/*.xml", "acceptance"),
    ("**/integration-test-xml-*/**/*.xml", "integration"),
    ("**/e2e-test-xml/**/*.xml", "e2e"),
    ("**/python-unit-test-xml/unit.xml", "unit"),
    ("**/python-integration-test-xml/integration.xml", "integration"),
    ("**/ui-test-xml/junit.xml", "unit"),
    ("**/playwright-e2e-test-xml-*/junit.xml", "e2e"),
]

TEST_TYPES = ["unit", "acceptance", "integration", "e2e"]
COLUMN_WIDTH = 14


def discover_xml_files(root: Path) -> list[tuple[str, str, Path]]:
    """Return a list of (component, test_type, xml_path) tuples."""
    results = []
    seen: set[Path] = set()
    for pattern, test_type in PATTERNS:
        for xml_path in sorted(root.glob(pattern)):
            if xml_path in seen:
                continue
            seen.add(xml_path)
            component = _extract_component(xml_path, root, test_type)
            if component:
                results.append((component, test_type, xml_path))
    return results


def _extract_component(xml_path: Path, root: Path, test_type: str) -> str | None:
    rel = xml_path.relative_to(root)
    parts = rel.parts

    # Gradle: <component>/build/test-results/<task>/...
    if "build" in parts:
        idx = parts.index("build")
        if idx > 0:
            return parts[idx - 1]
        return None

    # CI artifact: <type>-test-xml-<module>/...
    for part in parts:
        if part.startswith("unit-test-xml-"):
            return part.removeprefix("unit-test-xml-")
        if part.startswith("acceptance-test-xml-"):
            return part.removeprefix("acceptance-test-xml-")
        if part.startswith("integration-test-xml-"):
            return part.removeprefix("integration-test-xml-")

    # Playwright E2E CI artifact: playwright-e2e-test-xml-<spec>/junit.xml
    for part in parts:
        if part.startswith("playwright-e2e-test-xml-"):
            return "ui"

    # CI artifact: e2e-test-xml/...
    if "e2e-test-xml" in parts:
        return "end2end-tests"

    # pytest: risk-engine/**/pytest.xml
    if xml_path.name == "pytest.xml":
        if "risk-engine" in parts:
            return "risk-engine"

    # pytest CI artifacts: python-unit-test-xml/ or python-integration-test-xml/
    for part in parts:
        if part.startswith("python-") and part.endswith("-test-xml"):
            return "risk-engine"

    # vitest: ui/**/junit.xml or ui-test-xml/junit.xml
    if xml_path.name == "junit.xml":
        if "ui" in parts or "ui-test-xml" in parts:
            return "ui"

    return None


def parse_results(
    discovered: list[tuple[str, str, Path]],
) -> dict[tuple[str, str], dict[str, int]]:
    """Parse XML files and aggregate counts by (component, test_type)."""
    summary: dict[tuple[str, str], dict[str, int]] = defaultdict(
        lambda: {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    )

    for component, test_type, xml_path in discovered:
        key = (component, test_type)
        try:
            tree = ET.parse(xml_path)
        except ET.ParseError:
            continue
        root = tree.getroot()
        for ts in _iter_testsuites(root):
            summary[key]["tests"] += int(ts.get("tests", 0))
            summary[key]["failures"] += int(ts.get("failures", 0))
            summary[key]["errors"] += int(ts.get("errors", 0))
            summary[key]["skipped"] += int(ts.get("skipped", 0))

    return dict(summary)


def _iter_testsuites(root):
    """Yield <testsuite> elements regardless of whether the root is <testsuite> or <testsuites>."""
    if root.tag == "testsuite":
        yield root
    elif root.tag == "testsuites":
        yield from root.iter("testsuite")


def inject_skipped_entries(
    summary: dict[tuple[str, str], dict[str, int]],
    job_results: dict[str, str],
) -> None:
    """For jobs that were skipped (or didn't run), inject placeholder entries
    so they still appear in the summary table."""
    for job_name, result in job_results.items():
        if result in ("success", "failure"):
            continue
        entries = JOB_COMPONENT_MAP.get(job_name, [])
        for component, test_type in entries:
            key = (component, test_type)
            if key not in summary:
                summary[key] = {
                    "tests": 0, "failures": 0, "errors": 0, "skipped": 0,
                    "job_skipped": True,
                }


def _format_cell(entry: dict[str, int] | None) -> str:
    """Render a single summary cell.

    - ``None`` (absent) → ``"-"``
    - ``job_skipped`` → ``"⊘"``
    - failures/errors > 0 → ``"N (M✗)"``
    - otherwise → ``"N"``
    """
    if entry is None:
        return "-"
    if entry.get("job_skipped"):
        return "⊘"
    count = entry["tests"]
    fail_count = entry["failures"] + entry["errors"]
    if fail_count > 0:
        return f"{count} ({fail_count}✗)"
    return str(count)


def _row_status(summary: dict[tuple[str, str], dict[str, int]], component: str) -> str:
    """Determine the status icon for a component row.

    - ``"✗"`` if any cell has failures or errors
    - ``"⊘"`` if every cell is absent or job_skipped
    - ``"✓"`` otherwise
    """
    entries = [(tt, summary.get((component, tt))) for tt in TEST_TYPES]
    has_failure = any(
        e is not None and not e.get("job_skipped") and (e["failures"] + e["errors"]) > 0
        for _, e in entries
    )
    if has_failure:
        return "✗"
    all_absent_or_skipped = all(
        e is None or e.get("job_skipped")
        for _, e in entries
    )
    if all_absent_or_skipped:
        return "⊘"
    return "✓"


def _components_sorted(summary):
    return sorted({comp for comp, _ in summary})


def format_text(summary: dict[tuple[str, str], dict[str, int]]) -> str:
    if not summary:
        return "No test results found."

    components = _components_sorted(summary)
    col_w = COLUMN_WIDTH
    status_w = 8
    name_w = max(len(c) for c in components + ["Component"]) + 2
    line_w = status_w + name_w + col_w * len(TEST_TYPES) + col_w  # +col_w for Total

    lines = []
    lines.append("Test Summary")
    lines.append("\u2550" * line_w)
    header = (
        f"{'Status':<{status_w}}"
        + f"{'Component':<{name_w}}"
        + "".join(f"{t.title():>{col_w}}" for t in TEST_TYPES)
        + f"{'Total':>{col_w}}"
    )
    lines.append(header)
    lines.append("\u2500" * line_w)

    grand = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}

    for comp in components:
        row_total = 0
        row_failures = 0
        cells = []
        for tt in TEST_TYPES:
            entry = summary.get((comp, tt))
            cell_text = _format_cell(entry)
            cells.append(f"{cell_text:>{col_w}}")
            if entry and not entry.get("job_skipped"):
                row_total += entry["tests"]
                row_failures += entry["failures"] + entry["errors"]

        total_cell = _format_cell({"tests": row_total, "failures": row_failures, "errors": 0, "skipped": 0}) if row_total else str(row_total)
        cells.append(f"{total_cell:>{col_w}}")
        status = _row_status(summary, comp)
        lines.append(f"{status:<{status_w}}" + f"{comp:<{name_w}}" + "".join(cells))

        for tt in TEST_TYPES:
            key = (comp, tt)
            if key in summary and not summary[key].get("job_skipped"):
                for k in grand:
                    grand[k] += summary[key][k]

    lines.append("\u2500" * line_w)

    total_cells = []
    for tt in TEST_TYPES:
        tt_counts = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
        for c in components:
            entry = summary.get((c, tt))
            if entry and not entry.get("job_skipped"):
                for k in tt_counts:
                    tt_counts[k] += entry[k]
        if tt_counts["tests"]:
            total_cells.append(f"{_format_cell(tt_counts):>{col_w}}")
        else:
            total_cells.append(f"{'-':>{col_w}}")
    grand_cell = _format_cell(grand)
    total_cells.append(f"{grand_cell:>{col_w}}")
    overall_status = "✗" if (grand["failures"] + grand["errors"]) > 0 else "✓"
    lines.append(f"{overall_status:<{status_w}}" + f"{'Total':<{name_w}}" + "".join(total_cells))

    passed = grand["tests"] - grand["failures"] - grand["errors"] - grand["skipped"]
    failed = grand["failures"] + grand["errors"]
    skipped = grand["skipped"]

    lines.append("")
    if failed:
        lines.append(f"\u2717 {passed} passed, {failed} failed, {skipped} skipped")
    else:
        lines.append(f"\u2713 {passed} passed, {failed} failed, {skipped} skipped")

    return "\n".join(lines)


def format_markdown(summary: dict[tuple[str, str], dict[str, int]]) -> str:
    if not summary:
        return "No test results found."

    components = _components_sorted(summary)

    lines = []
    lines.append("## Test Summary")
    lines.append("")
    lines.append("| Status | Component | Unit | Acceptance | Integration | E2E | Total |")
    lines.append("|:------:|-----------|-----:|-----------:|------------:|----:|------:|")

    grand = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}

    for comp in components:
        row_total = 0
        row_failures = 0
        cells = []
        for tt in TEST_TYPES:
            entry = summary.get((comp, tt))
            cells.append(_format_cell(entry))
            if entry and not entry.get("job_skipped"):
                row_total += entry["tests"]
                row_failures += entry["failures"] + entry["errors"]

        total_cell = _format_cell({"tests": row_total, "failures": row_failures, "errors": 0, "skipped": 0}) if row_total else str(row_total)
        status = _row_status(summary, comp)
        lines.append(f"| {status} | {comp} | {' | '.join(cells)} | {total_cell} |")

        for tt in TEST_TYPES:
            if (comp, tt) in summary and not summary[(comp, tt)].get("job_skipped"):
                for k in grand:
                    grand[k] += summary[(comp, tt)][k]

    total_cells = []
    for tt in TEST_TYPES:
        tt_counts = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
        for c in components:
            entry = summary.get((c, tt))
            if entry and not entry.get("job_skipped"):
                for k in tt_counts:
                    tt_counts[k] += entry[k]
        total_cells.append(f"**{_format_cell(tt_counts)}**" if tt_counts["tests"] else "-")
    grand_cell = _format_cell(grand)
    overall_status = "✗" if (grand["failures"] + grand["errors"]) > 0 else "✓"
    lines.append(f"| {overall_status} | **Total** | {' | '.join(total_cells)} | **{grand_cell}** |")

    passed = grand["tests"] - grand["failures"] - grand["errors"] - grand["skipped"]
    failed = grand["failures"] + grand["errors"]
    skipped = grand["skipped"]

    lines.append("")
    if failed:
        lines.append(f"\u2717 **{passed} passed, {failed} failed, {skipped} skipped**")
    else:
        lines.append(f"\u2713 **{passed} passed, {failed} failed, {skipped} skipped**")

    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description="Aggregate JUnit XML test results.")
    parser.add_argument("root", nargs="?", default=".", help="Root directory to scan (default: .)")
    parser.add_argument("--format", choices=["text", "markdown"], default="text", dest="fmt")
    parser.add_argument(
        "--job-results",
        default=None,
        help="JSON mapping CI job names to their result (success/failure/skipped/cancelled)",
    )
    args = parser.parse_args()

    root = Path(args.root).resolve()
    discovered = discover_xml_files(root)
    summary = parse_results(discovered)

    if args.job_results:
        job_results = json.loads(args.job_results)
        inject_skipped_entries(summary, job_results)

    if args.fmt == "markdown":
        print(format_markdown(summary))
    else:
        print(format_text(summary))


if __name__ == "__main__":
    main()
