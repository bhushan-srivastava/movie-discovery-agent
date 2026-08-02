# Movie Discovery Agent — Technical Design

**Version:** 1.4  
**Date:** July 2026  
**Status:** Approved Database-First Design Baseline

---

## 1. Purpose and Scope

This document defines the implementation-ready V1 technical design aligned to `docs/01-requirements.md`.

### V1 In Scope

- Java 21, Spring Boot 4.1, Spring AI 2.0, Spring AI MCP
- Gemini API used through Spring AI `ChatClient`
- Backend acts as MCP client to the existing Neon MCP service over Streamable HTTP
- Neon MCP configured read-only, project-scoped, and restricted to query/schema-inspection tools where possible
- Neon PostgreSQL as the existing source of truth
- Backend JPA persistence only for `Conversation` and `Message`
- Java schema validation mode: `spring.jpa.hibernate.ddl-auto=validate`
- Java never creates, updates, or migrates schema objects
- React + Ant Design UI using `VITE_API_BASE_URL` only
- Streaming over HTTP `POST` with NDJSON via `fetch()` + `ReadableStream`
- Standalone Node.js ETL that reads prepared local JSON and upserts movie data

### V1 Out of Scope

- Custom Java movie MCP server implementation
- Authentication and user entity
- RAG/embeddings/vector database
- Redis
- Docker/Kubernetes
- Live TMDB integration
- Cloud deployment
- Python ETL
- Express-based ETL service
- Flyway-based Java schema migration workflows

---

## 2. Architecture Overview

### Runtime Components

1. **Main Backend (`backend/`)**
   - REST APIs and NDJSON streaming endpoint
   - Spring AI `ChatClient` orchestration with Gemini
   - Spring AI MCP client over Streamable HTTP
   - Conversation/message persistence
   - Prompt policy and response redaction rules

2. **Existing Neon MCP Service (external dependency)**
   - Provides read-only query/schema tools for the scoped Neon project
   - Executes movie-table data access for discovery operations

3. **React UI (`frontend/`)**
   - Ant Design chat interface
   - Conversation sidebar + new conversation action
   - Shared-conversation warning banner
   - NDJSON stream consumption and tool-status rendering

4. **Node.js ETL (`etl/`)**
   - Reads prepared local JSON dataset
   - Normalizes and upserts movie records into Neon

### High-Level Flow

UI → Backend REST/stream controller → Spring AI `ChatClient` (Gemini) → Spring AI MCP client (Streamable HTTP) → Existing Neon MCP service → Neon `movie` table → backend NDJSON stream → UI.

---

## 3. Ownership and Boundaries

### Runtime Data Ownership

- **Backend (Java/JPA):** `Conversation`, `Message`
- **External Neon MCP + Neon DB:** movie-table query access used by runtime tool invocation

### Schema Ownership

- Neon PostgreSQL is the source of truth.
- Database tables are created and managed outside Java.
- Java never creates, updates, or migrates schema objects.

### Validation Boundary

- Backend uses `spring.jpa.hibernate.ddl-auto=validate`.
- Backend validates only `Conversation` and `Message` mappings at startup.
- Any movie-table shape mismatch is handled at the MCP/query contract boundary (not Java `Movie` JPA ownership).

### MCP Boundary

- Backend is the MCP client.
- Neon MCP is external and read-only for runtime use.
- Runtime MCP usage is limited to query/schema-inspection tools where possible.

---

## 4. Package and Component Structure

### Backend

```text
com.group.moviediscoveryagent
├─ controller
│  ├─ ConversationController
│  └─ ChatStreamController
├─ service
│  ├─ ConversationService
│  ├─ MessageService
│  ├─ AgentService
│  └─ StreamEventService
├─ persistence
│  ├─ entity
│  │  ├─ ConversationEntity
│  │  └─ MessageEntity
│  └─ repository
│     ├─ ConversationRepository
│     └─ MessageRepository
├─ ai
│  ├─ ChatClientConfig
│  ├─ PromptPolicyConfig
│  └─ ModelTimeoutConfig
├─ mcp
│  └─ client
│     └─ NeonMcpClientConfig
├─ exception
└─ model
   ├─ api
   └─ stream
```

### Frontend

```text
frontend/src
├─ components
│  ├─ ConversationSidebar
│  ├─ SharedWarningBanner
│  ├─ ChatTranscript
│  ├─ ChatInput
│  └─ ToolStatusPanel
├─ api
│  └─ chatApi
├─ hooks
│  └─ useChatStream
└─ pages
   └─ ChatPage
```

### ETL

```text
etl/
├─ package.json
├─ src/
│  ├─ index.js
│  ├─ normalize.js
│  ├─ upsertMovies.js
│  └─ db.js
└─ data/
   └─ movies.source.json
```

---

## 5. Persistence Model and Query Rules

### 5.1 Backend JPA Entities

- `conversation.id`: UUID, database-generated
- `message.id`: UUID, database-generated
- `message.conversation_id` foreign key references `conversation.id`
- Ordering index target: `message(conversation_id, created_at)`

### 5.2 Movie Data Access

- Movie data is queried through Neon MCP runtime tools against Neon tables.
- Backend does not own a Java `Movie` entity lifecycle for schema management.
- Runtime query results are treated as external tool output contract data.

### 5.3 Movie Query Constraints (Runtime Policy)

- SELECT-only query intent
- `movie` table only
- Max 5 rows returned
- Never query `conversation` or `message` tables
- Case-insensitive search behavior
- Discovery/recommendation dimensions: genres, ratings, popularity, language, runtime, director, year

---

## 6. Configuration Strategy

### Backend Configuration Files

- `application.properties`: common/shared settings
- `application-development.properties`: development settings using environment-variable placeholders

### Required Runtime Configuration

- Gemini API key and model selection
- Neon DB connection settings for backend JPA (`Conversation`/`Message` only)
- Neon MCP endpoint and project scope settings
- Neon MCP read-only and query/schema tool restrictions
- Model timeout configuration

### Frontend Configuration

- React uses only `VITE_API_BASE_URL`
- No DB credentials and no model credentials in UI

---

## 7. API and Streaming Contract

### 7.1 Conversation APIs

- `GET /api/conversations`
- `POST /api/conversations`
- `GET /api/conversations/{conversationId}/messages`

### 7.2 Streaming Chat API

- `POST /api/conversations/{conversationId}/chat/stream`
- Request body includes `message` (required)

### 7.3 NDJSON Framing (Normative)

- `Content-Type: application/x-ndjson`
- UTF-8 encoding
- Exactly one valid JSON object per line
- Every object line ends with `\n`

Client behavior is normative:

- Use `fetch()` + `ReadableStream`
- Buffer partial chunks until a newline-terminated line is complete
- Parse each complete line as one JSON object event

### 7.4 Event Types

- `tool-start`
- `tool-result`
- `text-delta`
- `completion`
- `error`

### 7.5 Timeout Behavior

- Timeout before first stream line: HTTP `504`
- Timeout after stream starts: emit `error` event and close stream

---

## 8. MCP Client Contract and Guardrails

Neon MCP runtime use must satisfy all of the following:

1. Read-only MCP operation
2. Scoped to the approved Neon project
3. Query and schema-inspection tools only where possible
4. Never use mutation/admin tools
5. System prompt + runtime policy enforce:
   - SELECT-only intent
   - `movie` table only
   - max 5 rows
   - never query `conversation`/`message`
   - case-insensitive search semantics
   - discovery fields: genres, ratings, popularity, language, runtime, director, year
6. User-facing responses must never reveal:
   - SQL text
   - schema metadata
   - credentials
   - internal exception details

---

## 9. Node.js ETL Design

### Input

- Prepared local JSON dataset (no live TMDB access)

### Responsibilities

- Parse source JSON
- Normalize records
- Upsert into Neon movie table

### Constraints

- Standalone Node.js script
- No Express
- No Python
- Repeatable execution for demo refresh

---

## 10. Error Handling and Logging

### Backend

- Validation failures: HTTP 400
- Timeout before stream start: HTTP 504
- Timeout after stream start: NDJSON `error` event
- Persist assistant message only on successful completion
- Never persist partial assistant output after failure

### Logging Requirements

- Include request ID and conversation ID where available
- Log tool start/result and timeout boundaries
- Never log credentials
- Do not expose SQL/schema/internal exceptions in user-facing responses

---

## 11. Testing Strategy

### Backend

- Repository/service tests for `Conversation` and `Message`
- Latest-10 context selection tests
- NDJSON framing/event-sequencing tests
- Timeout behavior tests (pre-stream 504, post-stream error event)
- Assistant persistence rule tests
- MCP client integration tests with Neon MCP
- Guardrail compliance tests (table allowlist, row limit, read-only behavior)
- Redaction tests for SQL/schema/credential/internal-error suppression

### ETL

- Normalization tests
- Upsert idempotency tests
- Invalid-input handling tests
- Repeat-run behavior tests

### UI

- NDJSON parser tests (`fetch()` + `ReadableStream`)
- Incomplete chunk buffering tests
- Sidebar/new-conversation behavior
- Shared warning visibility tests
- Tool-status and error rendering tests

---

## 12. Risks and Mitigations

1. **External Neon MCP capability mismatch**
   - Mitigation: early contract validation of read-only scope and tool allowlist

2. **Prompt or policy drift from safety constraints**
   - Mitigation: explicit prompt template tests and runtime guardrail checks

3. **NDJSON client parser edge cases**
   - Mitigation: strict line-buffering tests and malformed-chunk handling tests

4. **ETL data quality variability**
   - Mitigation: normalization validation plus import summary/reporting

---

## 13. Frontend Technology Update

### 13.1 TypeScript to JavaScript Conversion

- All frontend components converted from TypeScript (.tsx/.ts) to JavaScript (.jsx/.js)
- Removed TypeScript type annotations and interfaces
- Removed all TypeScript-only dependencies (`typescript`, `@types/*` packages)
- Build script changed from `"tsc -b && vite build"` to `"vite build"` (no TypeScript compilation)
- Entry point updated in `index.html`: from `/src/main.tsx` to `/src/main.jsx`

### 13.2 New Conversation Draft Mode (Client-Side)

Conversation creation is now a two-phase process:

1. **Local Draft Phase (no HTTP call)**
   - User clicks "New" button
   - ChatPage state: `isDraft=true`, `selectedId=null`, `messages=[]`
   - No `POST /api/conversations` call is made
   - Draft does not appear in the persisted sidebar list
   - User types message into the composer
   - Input text preserved if submission fails

2. **Conversation Creation Phase (on first valid message)**
   - User submits first message
   - Message is trimmed of whitespace
   - Blank messages are rejected
   - Title created from first prompt: trimmed and truncated to 255 characters
   - `POST /api/conversations` called with title
   - Returned conversation ID used for subsequent streaming
   - `ChatPage.isDraft` set to `false`
   - Sidebar refreshed to show new persisted conversation

3. **Subsequent Messages (no new creation)**
   - Messages sent to existing conversation ID
   - No `POST /api/conversations` call
   - Title never overwritten on existing conversation

**Failure Behavior:**
- Conversation creation failure: remain in draft mode, preserve typed message, show error
- Streaming failure after creation: keep created conversation, refresh sidebar and messages

### 13.3 Markdown Response Format

System prompt and rendering updated from HTML to Markdown:

- Backend system prompt (`MovieAssistantPrompt.SYSTEM_PROMPT`) now requires clean Markdown output
- Removed prohibitions on Markdown syntax
- Removed HTML-only response contract
- Model requests: short paragraphs, numbered/unordered lists, bold and italic text
- Model prohibited from: raw HTML, scripts, styles, iframes, forms, images, executable content

### 13.4 Markdown Rendering with react-markdown

Frontend rendering strategy:

- Added dependencies: `react-markdown` (^8.0.7), `remark-gfm` (^3.0.1)
- Removed dependencies: `dompurify`, `@types/dompurify`
- Removed: `dangerouslySetInnerHTML`, HTML sanitization helpers

Rendering behavior:

- **Assistant messages**: Rendered with `ReactMarkdown` using `remark-gfm` plugin
- **User messages**: Rendered as plain text (no Markdown parsing)
- **Links**: Custom renderer disables clickable anchors; only visible text is rendered
- **Images**: Custom renderer returns null (images not rendered)
- **Streaming**: Markdown accumulated progressively from text-delta events
- **Incomplete Markdown**: Rendering robust to incomplete or malformed Markdown
- **HTML from model**: Raw HTML is not interpreted as DOM; HTML tags appear as text

### 13.5 Chat Message DOM Structure

Messages refactored for proper alignment:

**Assistant Message Row:**
```jsx
<div className="message-row assistant-row">
  <Avatar>AI</Avatar>
  <div className="message-content">
    <div className="message-role">Assistant</div>
    <div className="message-bubble assistant-bubble">
      <ReactMarkdown>...</ReactMarkdown>
    </div>
  </div>
</div>
```

**User Message Row:**
```jsx
<div className="message-row user-row">
  <div className="message-content">
    <div className="message-role">You</div>
    <div className="message-bubble user-bubble">
      plain text
    </div>
  </div>
  <Avatar>You</Avatar>
</div>
```

Alignment and spacing:

- Assistant messages: left-aligned, avatar left of content
- User messages: right-aligned, avatar right of content
- Avatar size: 32–36 pixels
- Avatar-to-content gap: 8–12 pixels
- Avatar aligns to top of multi-line content
- Role label sits directly above bubble
- Message bubbles: width based on content, max ~75% of transcript width
- Compact padding and rounded corners
- No full-width Ant Design Cards

### 13.6 Transcript Scrolling

- Single transcript scroll container (no nested scrollbars)
- Auto-scroll to bottom when message submitted or user already near bottom
- Preserves scroll position when user intentionally scrolls upward
- Bottom padding prevents composer overlap
- Streaming deltas remain visible when user is near bottom

### 13.7 Composer and Responsive Layout

**Composer:**
- Compact Ant Design `Input.TextArea`
- Send button vertically aligned
- Enter-to-send, Shift+Enter for newline
- Disabled while streaming, disabled when blank
- Placeholder changes for draft vs. normal mode

**Responsive (< 768px width):**
- Sidebar collapsible or drawer-based
- Message bubbles fit inside viewport
- Composer remains usable
- No horizontal page scrollbar

### 13.8 Theme Variables

Centralized design tokens:

```css
--page-bg
--sidebar-bg
--main-panel-bg
--user-bubble-bg
--assistant-bubble-bg
--primary-color
--text-color
--muted-text
--border-color
--standard-spacing (8px)
--border-radius
--subtle-shadow
```

---

## 14. Implementation Readiness

Design is implementation-ready with these locked decisions:

- Backend is the only Java runtime application in scope
- Runtime model path: Gemini API via Spring AI `ChatClient`
- Runtime tool path: Spring AI MCP client to existing Neon MCP over Streamable HTTP
- Neon MCP is read-only, project-scoped, and restricted to query/schema tools where possible
- JPA in backend is only for `Conversation` and `Message`
- Neon remains source of truth; Java performs no schema creation/migration
- NDJSON streaming over POST consumed by `fetch()` + `ReadableStream`
- Shared conversation UX, latest-10 context, and MCP tool-status streaming remain mandatory
