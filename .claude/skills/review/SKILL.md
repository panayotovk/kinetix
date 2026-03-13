---
name: review
description: Run a structured code review against project conventions, checking architecture, test coverage, security, and code quality. Invoke with /review optionally followed by a file path, directory, or description of what to review.
user-invocable: true
allowed-tools: Read, Glob, Grep, Bash, Task, WebFetch, WebSearch
---

# Code Review

You are performing a structured code review of the changes or files specified. If no specific target is given, review the uncommitted changes (staged + unstaged).

## Step 1 — Identify what to review

If the user specified a file or directory, review that. Otherwise:

```bash
git diff --name-only HEAD
git diff --cached --name-only
```

Read the changed files to understand the full context of the changes.

## Step 2 — Review against project conventions (CLAUDE.md)

Check each changed file against these project standards:

### Architecture & Organisation
- [ ] **Single responsibility** — each class/function has one reason to change
- [ ] **One type per file** — data classes, enums, sealed classes, interfaces in their own files
- [ ] **DTOs in `dtos` sub-package** — one `@Serializable` data class per file
- [ ] **Implementation files focused on behaviour** — no type definitions mixed in

### Code Quality
- [ ] **Depend on abstractions** — interfaces at module boundaries
- [ ] **Small, composable units** — prefer focused classes over large ones
- [ ] **Clear naming** — names describe what they do, no abbreviations without context
- [ ] **No unnecessary complexity** — simplest solution that meets the requirement

### Test Coverage
- [ ] **Tests exist for new behaviour** — unit tests for logic, integration for boundaries
- [ ] **Test names are behavioural specifications** — "rejects a trade when..." not "testTrade"
- [ ] **Tests are independent and fast** — no shared state, no execution order dependency
- [ ] **UI features have Playwright E2E tests** — if applicable

### Security (quick scan)
- [ ] **No hardcoded secrets** — no API keys, passwords, tokens in code or config
- [ ] **Input validation at boundaries** — HTTP endpoints, Kafka consumers, gRPC services validate input
- [ ] **No SQL injection vectors** — parameterised queries, no string interpolation
- [ ] **Auth checks on endpoints** — server-side authorisation enforced

### Kotlin-specific (if applicable)
- [ ] **Kotest FunSpec** with `shouldBe` / `shouldThrow` matchers
- [ ] **MockK** for mocking, not the class under test
- [ ] **Exposed ORM** patterns followed correctly

### Python-specific (if applicable)
- [ ] **pytest** conventions followed
- [ ] **NumPy vectorisation** preferred over loops for numerical code

### UI-specific (if applicable)
- [ ] **Vitest** for unit tests
- [ ] **Playwright** for E2E tests in `ui/e2e/`
- [ ] **Tailwind** class conventions followed
- [ ] **Accessibility** — ARIA labels, keyboard navigation

## Step 3 — Summarise findings

Structure your review as:

### What looks good
List the things that are well-done — good patterns, clean code, proper testing.

### Issues found
For each issue, provide:
- **Severity**: CRITICAL / HIGH / MEDIUM / LOW
- **File and line**: exact location
- **Issue**: what is wrong
- **Fix**: specific recommendation

Order by severity (critical first).

### Suggestions
Optional improvements that are not blockers but would improve the code.

## Reminders

- Be specific — cite exact files and lines, not vague guidance
- Focus on what matters — a security issue is more important than a naming nit
- Respect existing patterns — do not suggest wholesale refactors unless asked
- Check test coverage — if new behaviour is added, tests should exist
