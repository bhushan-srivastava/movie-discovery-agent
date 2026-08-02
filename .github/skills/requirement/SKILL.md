---
name: requirement
description: Capture approved story requirements and acceptance criteria without implementation changes.
---

# Requirement Skill

## Stage
Requirements definition only.

## Authority
- Follow `AGENTS.md` and `.github/copilot-instructions.md` for shared working rules.

## Inputs
- Approved story/epic context.
- Existing docs and constraints.

## Workflow
1. Capture problem statement, scope, assumptions, and constraints.
2. Define clear, testable acceptance criteria.
3. Record out-of-scope items.
4. Write/update `docs/01-requirements.md`.

## Prohibited Actions
- Do not implement the app.
- Do not change Java source files.
- Do not change `pom.xml`.
- Do not change runtime configuration files (including `application.properties`).
- Do not install dependencies.

## Output
- `docs/01-requirements.md` updated with requirement decisions for the approved scope only.

