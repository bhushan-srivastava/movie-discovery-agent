---
name: plan
description: Create an execution plan that sequences approved work without coding.
---

# Plan Skill

## Stage
Implementation planning only.

## Authority
- Follow `AGENTS.md` and `.github/copilot-instructions.md` for shared working rules.

## Inputs
- Approved requirements/design (`docs/01-requirements.md`, `docs/02-design.md`).

## Workflow
1. Break work into ordered tasks tied to acceptance criteria.
2. Define file-level impact expectations and verification steps.
3. Identify dependencies, risks, and rollback considerations.
4. Write/update `docs/03-plan.md`.

## Prohibited Actions
- Do not implement the app.
- Do not change Java source files.
- Do not change `pom.xml`.
- Do not change runtime configuration files (including `application.properties`).
- Do not install dependencies.

## Output
- `docs/03-plan.md` updated with executable task sequencing for one approved scope.

