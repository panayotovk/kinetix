---
name: dep-audit
description: Audit dependencies across all modules for known vulnerabilities, outdated versions, and security risks. Invoke with /dep-audit optionally followed by a specific module (e.g. "ui", "risk-engine", "position-service").
user-invocable: true
allowed-tools: Read, Glob, Grep, Bash, Task, WebFetch, WebSearch
---

# Dependency Audit

You are performing a dependency audit across Kinetix modules. Check for known vulnerabilities, outdated packages, and security risks.

## Step 1 — Identify modules to audit

If the user specified a module, audit only that. Otherwise audit all:

- **Gradle (Kotlin services)**: `build.gradle.kts` files across all modules
- **Python (risk-engine)**: `risk-engine/pyproject.toml` and/or `risk-engine/uv.lock`
- **npm (UI)**: `ui/package.json` and `ui/package-lock.json`

## Step 2 — Audit each ecosystem

### Gradle dependencies
```bash
./gradlew dependencies --configuration runtimeClasspath 2>/dev/null | head -100
# Or check for outdated:
./gradlew dependencyUpdates 2>/dev/null || echo "ben-manes versions plugin not configured — checking manually"
```

Read the `build.gradle.kts` files and check key dependency versions:
- Ktor, Exposed, Kotlin, kotlinx-serialization
- Kafka client, Lettuce (Redis), gRPC
- Kotest, MockK (test dependencies)
- Logback, Micrometer (observability)

### Python dependencies
```bash
cd risk-engine && uv pip list 2>/dev/null || cat pyproject.toml
# Check for vulnerabilities:
cd risk-engine && uv run pip-audit 2>/dev/null || echo "pip-audit not available — checking manually"
```

Key Python packages to check:
- numpy, scipy (numerical computing)
- grpcio, protobuf (gRPC)
- pytest (testing)

### npm dependencies
```bash
cd ui && npm audit 2>/dev/null
cd ui && npm outdated 2>/dev/null
```

Key npm packages to check:
- React, React DOM
- Vite, Vitest
- Playwright
- Tailwind CSS
- Recharts or chart libraries
- WebSocket libraries

## Step 3 — Cross-reference with known CVEs

For each ecosystem, check:
- Are there any dependencies with **known CVEs** (Critical or High severity)?
- Are there dependencies **more than 2 major versions behind**?
- Are there dependencies that are **unmaintained** (no release in 12+ months)?
- Are there dependencies with **license concerns** (GPL in a commercial project)?

## Step 4 — Report findings

Structure the report as:

### Critical (fix immediately)
- Dependencies with known exploitable CVEs
- Dependencies with critical security advisories

### High (fix soon)
- Dependencies multiple major versions behind
- Dependencies with high-severity CVEs

### Medium (plan to update)
- Dependencies 1+ minor versions behind with security fixes
- Dependencies approaching end-of-life

### Low (track)
- Dependencies with available updates (no security impact)
- Dependencies with maintenance concerns

For each finding, provide:
- **Package**: name and current version
- **Module**: which Kinetix module uses it
- **Issue**: what is wrong (CVE number, version gap, etc.)
- **Recommendation**: specific version to upgrade to, or alternative package

## Step 5 — Summary

Provide a one-paragraph executive summary:
- Total dependencies audited
- Critical/High/Medium/Low counts
- Top recommendation

## Reminders

- Focus on runtime dependencies first, then test dependencies
- Check transitive dependencies too — a vulnerable transitive dep is still a vulnerability
- Note if any security scanning tools are missing from the project (suggest adding them to CI)
- Be specific about upgrade paths — "upgrade X from 1.2 to 1.5" not "update X"
