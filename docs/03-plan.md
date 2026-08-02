# Movie Discovery Agent — Implementation Plan

**Version:** 1.2  
**Date:** July 2026  
**Status:** Approved Six-Story Delivery Plan  
**Target Duration:** One intensive week (approximately 45-55 hours)

---

## 1. Planning Intent

This plan sequences the implementation into exactly six stories with explicit dependency order, file impact, test strategy, and completion evidence.

Scope includes:
- main backend (`backend/`) with Gemini + Spring AI + Spring AI MCP client
- React + Ant Design UI (`frontend/`)
- full integration testing and hardening
- required-tool evidence capture (RTK, Graphify), documentation, and demo preparation

Completed setup evidence:
- `etl/` already contains the completed Node.js movie import utility
- movie data is already imported into Neon and is not a new implementation story in this plan

Runtime MCP integration is mandatory and uses the backend MCP client path to the existing read-only Neon MCP service.

---

## 2. Prerequisites (Before Story 1 Starts)

1. Confirm Neon project is provisioned and reachable.
2. Confirm secure credential handling approach (no credentials in Git).
3. Finalize environment-variable names for:
   - backend Neon connection (`Conversation`/`Message` persistence)
   - backend Neon MCP endpoint and project scope
   - Gemini credentials/model configuration
   - UI `VITE_API_BASE_URL`
4. Confirm Gemini model selection and Spring AI compatibility target.
5. Confirm Neon MCP runtime configuration is read-only, project-scoped, and restricted to query/schema-inspection tools where possible.
6. Confirm ETL completion evidence (movie data already imported into Neon).

---

## 3. Execution Calendar

- **Day 1:** Story 1 — Backend foundation, Neon validation, Gemini and Neon MCP client
- **Day 2:** Story 2 — Conversation/message persistence and APIs
- **Days 3-4:** Story 3 — Spring AI orchestration, latest-10 context, and NDJSON streaming
- **Day 5:** Story 4 — React + Ant Design UI
- **Day 6:** Story 5 — Integration testing and hardening
- **Day 7:** Story 6 — RTK, Graphify, documentation, and demo preparation

---

## 4. Story 1 — Backend Foundation, Neon Validation, Gemini, and Neon MCP Client (Day 1)

### Goal

Establish backend runtime foundations for Neon validation-only persistence, Gemini model configuration, and Spring AI MCP client connectivity to existing Neon MCP without creating or changing database tables.

### Must Establish on Day 1

- Backend runtime foundation in `backend/`
- Neon schema validation mode (`spring.jpa.hibernate.ddl-auto=validate`)
- JPA scope limited to `Conversation` and `Message`
- Gemini selection/configuration baseline for Spring AI `ChatClient`
- Neon MCP client baseline over Streamable HTTP
- MCP client policy baseline:
  - read-only
  - project-scoped
  - query/schema-inspection tool restrictions where possible

### Acceptance Criteria Mapping

- FR-9, FR-10, FR-11, FR-12
- NFR-2, NFR-3, NFR-4
- Criterion 10

### Expected Files

- `backend/pom.xml` (dependency/config updates as required by Story 1)
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/application-development.properties`
- `backend/src/main/java/.../ai/ChatClientConfig.java`
- `backend/src/main/java/.../mcp/client/NeonMcpClientConfig.java`
- `backend/src/test/java/.../*`

### Tests

- Backend startup/context tests for Story 1 configuration.
- Neon connectivity and JPA validation tests for `Conversation`/`Message` only.
- MCP client configuration tests for Neon MCP endpoint/project scope policy.
- Provider compatibility smoke checks for selected Gemini model integration.

### Completion Evidence

- Backend starts with `ddl-auto=validate`.
- Backend connects to Neon for `Conversation`/`Message` mapping validation.
- Backend MCP client connects to Neon MCP using configured read-only/project-scoped settings.
- No Java table creation, alteration, or migration occurs.

### Dependencies

- Prerequisites complete.

---

## 5. Story 2 — Conversation/Message Persistence and APIs (Day 2)

### Goal

Implement conversation and message persistence plus REST APIs required for shared-conversation chat flows.

### Acceptance Criteria Mapping

- FR-1, FR-3, FR-4, FR-10, FR-11
- US-1, US-2
- Criterion 1

### Expected Files

- `backend/src/main/java/.../persistence/entity/ConversationEntity.java`
- `backend/src/main/java/.../persistence/entity/MessageEntity.java`
- `backend/src/main/java/.../persistence/repository/ConversationRepository.java`
- `backend/src/main/java/.../persistence/repository/MessageRepository.java`
- `backend/src/main/java/.../service/ConversationService.java`
- `backend/src/main/java/.../service/MessageService.java`
- `backend/src/main/java/.../controller/ConversationController.java`
- `backend/src/main/java/.../model/api/*`
- `backend/src/main/java/.../exception/*`
- `backend/src/test/java/.../*`

### Tests

- Repository tests for conversation/message persistence.
- Service tests for create/list/history behavior.
- API tests for conversation create/list/messages endpoints.
- Validation/error mapping tests for bad inputs (HTTP 400 behavior).

### Completion Evidence

- Shared conversations persist and reload from Neon.
- Conversation/message API contract works for create/list/history.
- JPA remains limited to `Conversation` and `Message`.

### Dependencies

- Story 1 complete.

---

## 6. Story 3 — Spring AI Orchestration, Latest-10 Context, and NDJSON Streaming (Days 3-4)

### Goal

Deliver backend chat orchestration with Gemini + Neon MCP, latest-10 context management, and NDJSON streaming responses including tool-status events.

### Acceptance Criteria Mapping

- FR-1, FR-5, FR-7, FR-8, FR-9, FR-12
- US-4, US-5, US-8
- Criterion 2, Criterion 5, Criterion 6, Criterion 7, Criterion 8, Criterion 9, Criterion 10

### Expected Files

- `backend/src/main/java/.../controller/ChatStreamController.java`
- `backend/src/main/java/.../service/AgentService.java`
- `backend/src/main/java/.../service/StreamEventService.java`
- `backend/src/main/java/.../ai/PromptPolicyConfig.java`
- `backend/src/main/java/.../ai/ModelTimeoutConfig.java`
- `backend/src/main/java/.../model/stream/*`
- `backend/src/main/java/.../mcp/client/NeonMcpClientConfig.java`
- `backend/src/test/java/.../*`

### Tests

- Latest-10 message context selection tests.
- Prompt/policy guardrail tests:
  - SELECT-only intent
  - `movie` table only
  - never `conversation`/`message` table queries
  - max 5 rows behavior constraints
- NDJSON contract tests:
  - `Content-Type: application/x-ndjson`
  - one JSON object per line
  - newline-terminated event objects
  - chunk-buffering compatibility
- Timeout behavior tests:
  - pre-stream timeout returns HTTP 504
  - post-stream timeout emits `error` event
- Assistant persistence tests (persist on completion only; no partial persistence after failure).

### Completion Evidence

- Runtime path is active: backend -> Spring AI `ChatClient` -> Spring AI MCP client -> existing Neon MCP.
- Streaming endpoint returns NDJSON events and tool-status updates.
- Latest-10 context is enforced.
- Guardrails are enforced and user responses do not expose SQL/schema metadata/credentials/internal errors.

### Dependencies

- Stories 1-2 complete.

---

## 7. Story 4 — React + Ant Design UI (Day 5)

### Goal

Deliver the minimal shared-conversation UI integrated with backend APIs and NDJSON stream handling.

### Acceptance Criteria Mapping

- FR-2, FR-3, FR-7, FR-11
- US-1, US-2, US-3, US-4
- Criterion 4, Criterion 5, Criterion 11

### Expected Files

- `frontend/package.json`
- `frontend/src/main.*`
- `frontend/src/pages/ChatPage.*`
- `frontend/src/components/ConversationSidebar.*`
- `frontend/src/components/SharedWarningBanner.*`
- `frontend/src/components/ChatTranscript.*`
- `frontend/src/components/ChatInput.*`
- `frontend/src/components/ToolStatusPanel.*`
- `frontend/src/api/chatApi.*`
- `frontend/src/hooks/useChatStream.*`
- `frontend/.env.example`

### Tests

- UI tests for sidebar/new-conversation behavior.
- Shared warning visibility tests.
- NDJSON parser tests using `fetch()` + `ReadableStream`.
- Incomplete-chunk buffering tests.
- Tool status and stream error rendering tests.

### Completion Evidence

- UI supports shared conversation navigation and chat submission.
- UI shows shared-data warning.
- UI renders streamed assistant output and tool-status events.
- UI uses only `VITE_API_BASE_URL`.

### Dependencies

- Story 3 complete.

---

## 8. Story 5 — Integration Testing and Hardening (Day 6)

### Goal

Execute integration coverage and quality hardening across backend and UI for the approved runtime architecture.

### Acceptance Criteria Mapping

- Cross-check all implemented criteria from Stories 1-4.

### Expected Files

- `backend/src/test/java/.../*`
- `frontend/src/**/*.test.*`
- `docs/05-test-report.md`
- `docs/04-build-log.md`

### Tests

- End-to-end path validation:
  - UI -> backend -> Spring AI `ChatClient` -> MCP client -> Neon MCP -> response stream
- Regression tests for persistence, context window, guardrails, and NDJSON streaming.
- Failure-path tests for timeout and redaction behavior.

### Completion Evidence

- Critical functional and failure paths pass in integrated runs.
- Blocking defects are fixed and regression-tested.
- Updated build/test evidence captured.

### Dependencies

- Stories 1-4 complete.

---

## 9. Story 6 — RTK, Graphify, Documentation, and Demo Preparation (Day 7)

### Goal

Finalize required evidence, documentation completeness, and demo readiness.

### Acceptance Criteria Mapping

- Definition of Done from `docs/01-requirements.md` is fully satisfied.

### Expected Files

- `docs/04-build-log.md`
- `docs/05-test-report.md`
- `docs/tool-evidence/*` (including RTK and Graphify outputs)
- `README.md` (run order, env setup, ETL evidence, backend, and UI)
- optional demo runbook updates under `docs/`

### Tests

- Final backend test suite and package verification.
- Final UI test/build verification.
- Final smoke validation of runtime path and shared-conversation UX.

### Completion Evidence

- RTK evidence captured.
- Graphify evidence captured.
- Documentation updated and consistent with implemented runtime architecture.
- Demo script rehearsed and validated.

### Dependencies

- Stories 1-5 complete.

---

## 10. Global Dependency Order and Effort

### Fixed Order

1. Story 1 (Day 1)
2. Story 2 (Day 2)
3. Story 3 (Days 3-4)
4. Story 4 (Day 5)
5. Story 5 (Day 6)
6. Story 6 (Day 7)

### Time Budget

- Approximate total effort: **45-55 hours**
- One intensive week delivery window

---

## 11. Story-to-Criterion Traceability Matrix

| Criterion | Covered In Story |
|---|---|
| Criterion 1: Shared conversations persisted | Story 2 |
| Criterion 2: Movie records queried from Neon | Story 3 |
| Criterion 3: ETL imports normalized data | Completed setup evidence (pre-plan) |
| Criterion 4: Shared warning visible | Story 4 |
| Criterion 5: Streaming responses delivered | Story 3, Story 4 |
| Criterion 6: Latest-10 context bound | Story 3 |
| Criterion 7: Assistant persists only on completion | Story 3 |
| Criterion 8: Timeout before stream returns 504 | Story 3 |
| Criterion 9: Timeout after stream emits error event | Story 3 |
| Criterion 10: Existing Neon MCP used via restricted client boundary | Story 1, Story 3 |
| Criterion 11: UI only uses `VITE_API_BASE_URL` | Story 4 |

---

## 12. Verification Principles

- Prefer unit and mock-driven tests for isolated logic.
- Use Neon integration tests only where true DB/MCP integration must be proven.
- Do not replace mandatory runtime MCP path to existing Neon MCP with a stub for production-path validation.
- Keep each story scoped to one focused delivery window with explicit completion evidence.
