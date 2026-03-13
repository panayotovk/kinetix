---
name: release
description: Prepare a release — generate changelog from git history, bump version, update Helm chart versions, and create a git tag. Invoke with /release optionally followed by a version number.
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Glob, Grep, Task
---

# Release Workflow

You are preparing a release for Kinetix. Follow these steps in order.

## Step 1 — Determine the version

If the user provided a version number, use that. Otherwise:

```bash
git tag --sort=-v:refname | head -5
```

Suggest the next version based on semantic versioning:
- **MAJOR** — breaking API or schema changes
- **MINOR** — new features, backward-compatible
- **PATCH** — bug fixes only

Ask the user to confirm the version before proceeding.

## Step 2 — Generate changelog

Generate a changelog from git history since the last tag:

```bash
LAST_TAG=$(git tag --sort=-v:refname | head -1)
git log ${LAST_TAG}..HEAD --oneline --no-merges
```

Categorise commits into:
- **Features** — new functionality
- **Fixes** — bug fixes
- **Improvements** — refactoring, performance, test coverage
- **Infrastructure** — CI/CD, deployment, dependencies

Format as markdown and present to the user for review.

## Step 3 — Update Helm chart versions

Find and update all Helm chart versions:

```bash
find deploy/helm -name "Chart.yaml" -type f
```

Update `version` and `appVersion` in each Chart.yaml to match the release version.

## Step 4 — Create the release commit and tag

```bash
git add -A
git commit -m "Release v${VERSION}"
git tag -a "v${VERSION}" -m "Release v${VERSION}"
```

## Step 5 — Summary

Present:
- The version number
- The changelog
- The files changed
- The tag created
- Remind the user to `git push --follow-tags` when ready

## Reminders

- Do NOT push automatically — let the user decide when to push
- Do NOT proceed past version confirmation without user approval
- Keep the changelog concise — group similar commits
- Verify that tests pass before creating the release tag
