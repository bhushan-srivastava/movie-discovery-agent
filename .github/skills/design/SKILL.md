---
name: design
description: Produce implementation-ready technical design for approved requirements only.
---

# Design Skill

## Stage
Technical design only.

## Authority
- Follow `AGENTS.md` and `.github/copilot-instructions.md` for shared working rules.

## Inputs
- Approved requirements from `docs/01-requirements.md`.
- Current architecture and constraints.

## Workflow
1. Map each acceptance criterion to design elements.
2. Define component interactions, data flow, and validation approach.
3. Identify risks, trade-offs, and test strategy hooks.
4. Write/update `docs/02-design.md`.

## Prohibited Actions
- Do not implement the app.
- Do not change Java source files.
- Do not change `pom.xml`.
- Do not change runtime configuration files (including `application.properties`).
- Do not install dependencies.

## Output
- `docs/02-design.md` updated with stage-specific design decisions only.

