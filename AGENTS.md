# Agent Working Agreement

## Think Before Coding

- Read the relevant requirements, design, plan, and existing code first.
- State assumptions when requirements are ambiguous.
- Surface conflicting requirements instead of silently selecting an interpretation.
- Explain meaningful trade-offs briefly.
- Ask before making a decision that expands the approved scope.

## Simplicity First

- Implement the smallest solution satisfying the current acceptance criteria.
- Do not add speculative features.
- Do not introduce abstractions with only one current use.
- Prefer Java and Spring capabilities over unnecessary dependencies.
- If a solution is significantly larger than necessary, simplify it.

## Surgical Changes

- Modify only files required by the current story.
- Do not refactor, rename, reorganize, or reformat unrelated code.
- Preserve the existing project style.
- Remove only unused code introduced by the current change.
- Every changed line must be traceable to the current requirement.

## Verifiable Execution

Before implementation:

1. Identify the acceptance criteria being addressed.
2. State assumptions.
3. List files expected to change.
4. Specify the tests that will prove completion.
5. Present a brief implementation plan.

After implementation:

1. Compile the project.
2. Run focused tests.
3. Run the relevant broader test suite.
4. Review the diff for unrelated changes.
5. Report modified files, test results, and known limitations.

Do not claim completion without successful verification.

## Repository Layout Note

- The Spring Boot application now lives in `backend/`.
- Use `backend/pom.xml`, `backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/`, and `backend/src/` when working on the application.