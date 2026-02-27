# Plan Document Conventions

This directory contains design documents, implementation plans, reviews, and
playtest notes for Moo of Doom. Follow these conventions so documents stay
consistent and easy to scan — both for humans and for AI assistants working
in the codebase.

---

## File Naming

```
YYYY-MM-DD-short-description.md
```

- Date is the creation date.
- Use lowercase kebab-case for the description.
- Examples: `2026-02-23-moo-of-doom-design.md`, `2026-02-26-security-review.md`

---

## Document Types

### Design Document

High-level feature design. Describes *what* and *why*.

```markdown
# Feature Name — Design Document

**Date:** YYYY-MM-DD
**Minecraft Version:** x.xx.xx
**Mod Loader:** NeoForge | Fabric | Both
**Mod ID:** `mooofdoom`

## Overview
One paragraph summarizing the feature.

## Sections
Use H2 for major sections. Tables for structured comparisons.
```

### Implementation Plan

Step-by-step build instructions. Describes *how*.

```markdown
# Feature Name — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** One sentence.
**Architecture:** One sentence describing the technical approach.
**Tech Stack:** Comma-separated list of tools and versions.

---

## Key Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| ...      | ...    | ... |

---

### Task N: Short Title

**Files:**
- Create/Modify: `path/to/file`

**Step 1: Description**
Prose explanation, then a code block if applicable.
```

### Review / Audit

Analysis of existing code or systems.

```markdown
# Topic — Review

**Date:** YYYY-MM-DD
**Scope:** What was reviewed.

---

## Summary
Brief findings overview.

## N. Category Name
### N.M — Finding Title
**Severity:** High | Medium | Low
**File:** `path/to/file`
Description and action items.
```

### Playtest Notes

Structured test scenarios and observations.

```markdown
# Playtest N — Title

**Date:** YYYY-MM-DD

## Scenarios
Numbered checklist of things to test.

## Observations
What happened, what broke, what to fix.
```

---

## Formatting Rules

- **H1** (`#`) for the document title only — one per file.
- **H2** (`##`) for major sections.
- **H3** (`###`) for tasks or subsections.
- **Horizontal rules** (`---`) between major sections.
- **Tables** for structured comparisons (decisions, audit findings, config).
- **Code blocks** with language tags (`java`, `groovy`, `bash`, `json`, etc.).
- **Bold** for field labels in metadata blocks (`**Goal:**`, `**Date:**`).
- Keep lines under ~100 chars where practical.
- No emoji unless the document is a playtest log and you're having fun.
