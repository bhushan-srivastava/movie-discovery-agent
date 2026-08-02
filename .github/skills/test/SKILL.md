---
name: test
description: Verify approved acceptance criteria with documented evidence and strict production-code protection.
---

# Test Skill

## Stage
Validation and test reporting only.

## Authority
- Follow `AGENTS.md` and `.github/copilot-instructions.md` for shared working rules.

## Scope and Edit Rules
- Validate approved AC coverage using normal, boundary, and failure cases.
- You may create/modify test source files when needed to verify approved AC.
- Do not modify production Java source files.
- Do not modify `pom.xml`.
- Do not modify runtime configuration files (including `application.properties`).
- If a defect is found, record failure evidence and return the issue to the build stage; do not fix production code in this stage.

## Workflow
1. Identify story ID and AC under test.
2. Define test matrix: normal, boundary, failure.
3. Add/update test-only files if required.
4. Run focused tests.
5. Run `.\mvnw.cmd test`.
6. Run `.\mvnw.cmd clean package`.
7. Record commands and actual results in `docs/05-test-report.md`.

## Completion Rule
- Never claim success without actual test output captured in `docs/05-test-report.md`.

## Output
- `docs/05-test-report.md` updated with command evidence and AC-level test results.


