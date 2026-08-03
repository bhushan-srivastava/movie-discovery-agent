# Movie Discovery Agent — Requirements

**Version:** 1.3  
**Date:** July 2026  
**Status:** Approved Database-First Scope Definition  

---

## 1. Problem Statement

Movie discovery is a common user need. Users often navigate multiple interfaces or perform repetitive manual searches to find movies that match their interests. This project builds a lightweight **Movie Discovery Agent** that accepts natural-language movie requests, uses a Generative AI model to select approved MCP tools, and returns conversational responses through a backend API and a minimal web chat interface.

The application is an intentionally shared demonstration experience. Conversations are visible to all users of the app, conversation history is persisted, and movie data is stored in Neon PostgreSQL as the existing source of truth for runtime lookup through the backend acting as an MCP client to an existing Neon MCP service over Streamable HTTP.

**Core Value:** One conversational interface → natural-language query → intelligent tool selection → movie results → shared demonstration experience.

---

## 2. Target User

**Primary User Personas:**

1. **Casual Movie Explorer**
   - Wants to discover movies through simple conversational prompts
   - Expects a minimal web chat interface
   - Values quick, understandable results

2. **Application Developer**
    - Evaluates the backend API, Spring AI behavior, and MCP integration
   - Needs documented request/response behavior
   - Values clear contracts and predictable runtime behavior

3. **Demonstration Audience**
   - Evaluates Spring AI, MCP, streaming chat, and shared conversation behavior
   - Understands that conversations are intentionally shared
   - Values a working reference implementation

---

## 3. Functional Requirements

### FR-1: Accept Natural-Language Movie Requests

The system SHALL accept natural-language movie requests through backend APIs used by the web UI.

Requests SHALL be associated with a selected conversation.

Responses SHALL return one completed JSON object and conversational output.

---

### FR-2: Provide a Minimal Web Chat UI

The system SHALL provide a minimal React-based chat UI using Ant Design.

The UI SHALL include:
- a conversation sidebar
- a new-conversation action
- a main chat transcript panel
- a message input area
- completed assistant output
- a visible loading indicator while the request is pending
- a visible warning that conversations are shared and sensitive information must not be entered

---

### FR-3: Support Shared Conversations Without Authentication

The system SHALL operate without authentication or user accounts.

All conversations SHALL be intentionally shared within the demonstration environment.

The UI SHALL prominently display a warning indicating:
- conversations are shared
- sensitive information must not be entered

---

### FR-4: Persist Conversations and Messages

The main backend SHALL persist `Conversation` and `Message` entities using Spring Data JPA with Neon PostgreSQL.

Persistence SHALL support:
- conversation creation
- conversation listing
- message history retrieval
- shared access across all users of the demonstration

Assistant messages SHALL be persisted only after successful completion of a response.

An assistant message SHALL NOT be persisted when a response fails before successful completion.

---

### FR-5: Use Persisted Movie Data Through Neon MCP

Neon PostgreSQL SHALL remain the existing source of truth for persisted `Movie` records.

Movie discovery and recommendation logic SHALL query persisted movie records through the existing Neon MCP service rather than a runtime `movies.json` catalogue.

Runtime movie data access SHALL support:
- search
- details lookup
- recommendation filtering

---

### FR-6: Import and Normalize Movie Data

The system SHALL include a standalone Node.js ETL script that reads a prepared local JSON dataset and upserts normalized `Movie` records into Neon PostgreSQL.

The ETL process SHALL:
- run outside the web applications
- not use Express
- not use Python
- not call live TMDB APIs
- normalize incoming movie data into the approved `Movie` schema
- support repeatable import behavior for development and demonstration setup

---

### FR-7: Return Completed JSON Responses

The system SHALL expose `POST /api/conversations/{conversationId}/chat` with a JSON request body containing required `message` and a JSON response containing the completed assistant `message`.

The endpoint SHALL return clear JSON error responses for validation, missing-conversation, model, and tool failures. Streaming, NDJSON framing, and streamed tool-status events are not required.

---

### FR-8: Maintain Bounded Conversation Context

For each chat request, the backend SHALL build model context from the latest 10 messages of the selected conversation.

The context SHALL be bounded to avoid unbounded prompt growth.

---

### FR-9: Use Existing Neon MCP via Streamable HTTP

The backend SHALL act as an MCP client and connect to the existing Neon MCP service through Streamable HTTP.

Neon MCP configuration SHALL be:
- read-only
- scoped to the approved Neon project
- restricted to query and schema-inspection tools where possible

The runtime SHALL use approved movie-discovery tool behavior (`search_movies`, `get_movie_details`, `recommend_movies`) through the configured Neon MCP capability set.

The runtime SHALL NOT use write, mutation, or admin-style MCP tools.

---

### FR-10: Validate Java Entities Against the Existing Neon Schema

The backend application SHALL treat Neon PostgreSQL as the existing source of truth.

The database tables are created and managed outside Java.

Java SHALL never create, update, or migrate the schema.

The backend application SHALL use `spring.jpa.hibernate.ddl-auto=validate`.

The `Conversation` and `Message` entities SHALL map to and validate against the existing Neon schema at startup.

Java JPA SHALL remain limited to `Conversation` and `Message` persistence.

---

### FR-12: Enforce MCP Query and Response Guardrails

The system prompt and runtime policy SHALL enforce all of the following:

- SELECT-style query intent only
- query only the `movie` table
- maximum 5 rows returned for any movie-query tool invocation
- never query `conversation` or `message` tables
- case-insensitive movie searches
- use genres, ratings, popularity, language, runtime, director, and year for discovery and recommendations
- never reveal SQL text, schema metadata, credentials, or internal errors to end users

---

### FR-11: Use Externalized Configuration and Secret Management

The backend application SHALL use:
- `application.properties` for common configuration
- `application-development.properties` for development-specific configuration with environment-variable placeholders

The backend application SHALL use environment-variable-backed Neon configuration.

Real database and GenAI credentials SHALL remain outside Git.

The React UI SHALL use `VITE_API_BASE_URL` only and SHALL NOT contain database or model credentials.

---

## 4. Non-Functional Requirements

### NFR-1: Performance

- Database-backed movie queries should be optimized for interactive chat usage
- Model requests SHALL use a configurable timeout
- The UI SHALL show a loading indicator while waiting for the completed response
- External model latency is not guaranteed and is outside system control

---

### NFR-2: Reliability

- All REST API inputs SHALL be validated before processing
- Invalid REST inputs SHALL trigger exception handlers that map to HTTP 400 responses with clear error messages
- Model and tool failures SHALL return valid JSON error responses
- Assistant output SHALL NOT be persisted after a failed response
- The application SHALL fail clearly when Neon configuration is invalid
- MCP tool input validation failures SHALL be logged and reported to the GenAI model for recovery
- No uncaught exceptions SHALL leak to the REST API caller; all non-streaming errors SHALL return valid JSON responses

---

### NFR-3: Maintainability

- All code SHALL follow the project's coding rules:
  - constructor injection
  - thin REST controllers
    - centralized service-layer orchestration for chat and persistence
- The main backend SHALL own `Conversation` and `Message` persistence
- Movie data access SHALL occur through the configured Neon MCP client boundary
- Java SHALL not create or migrate schema objects
- JPA runtime mapping SHALL be validated with `ddl-auto=validate`
- Every new behavior SHALL have corresponding JUnit 5 tests
- Code SHALL compile successfully via `backend\mvnw.cmd clean package`
- Tests SHALL run via `backend\mvnw.cmd test`

---

### NFR-4: Configuration and Secret Management

- Common non-secret configuration SHALL live in `application.properties`
- Development overrides SHALL live in `application-development.properties`
- Real database and model credentials SHALL be provided through environment variables or equivalent external secret sources
- No database or model credentials SHALL be committed to Git
- The React UI SHALL expose only `VITE_API_BASE_URL`

---

## 5. User Stories

### US-1: User Chats Through Shared Web UI

**As a** demonstration user  
**I want to** chat through a simple web interface  
**So that** I can discover movies conversationally without calling the API directly  

**Acceptance Criteria:**
- I can open the React + Ant Design chat UI
- I can type a message and submit it
- I can see the completed assistant output in the chat transcript
- I can see final movie results when returned

---

### US-2: User Navigates Shared Conversations

**As a** demonstration user  
**I want to** select an existing conversation or start a new one  
**So that** I can continue or begin a shared chat session  

**Acceptance Criteria:**
- I can view a conversation sidebar
- I can create a new conversation
- I can switch between conversations
- the selected conversation loads its prior persisted messages

---

### US-3: User Sees Shared-Data Warning

**As a** demonstration user  
**I want to** be warned that conversations are shared  
**So that** I avoid entering sensitive information  

**Acceptance Criteria:**
- the UI shows a visible warning banner or notice
- the notice clearly states that conversations are shared
- the notice clearly states that sensitive information must not be entered

---

### US-4: User Receives Completed Assistant Output

**As a** user  
**I want to** see progress while the assistant is working  
**So that** the app feels responsive and transparent  

**Acceptance Criteria:**
- I see a loading indicator while the request is pending
- I see one completed assistant response after the request succeeds
- failures are shown as a clear error state without partial assistant text

---

### US-5: Context Uses Latest 10 Messages

**As a** user  
**I want to** continue a conversation with recent context preserved  
**So that** follow-up questions make sense  

**Acceptance Criteria:**
- the backend uses only the latest 10 messages from the selected conversation as model context
- older messages may remain persisted but are excluded from model context once outside the 10-message bound

---

### US-6: Maintainer Uses Persisted Movie Records

**As a** maintainer  
**I want to** store normalized movie data in Neon PostgreSQL  
**So that** movie search, details lookup, and recommendation run on persisted data  

**Acceptance Criteria:**
- movie records are already stored in Neon
- movie search and recommendation use persisted records
- the runtime app does not depend on file-backed movie lookup

---

### US-7: Maintainer Imports Movie Data with Node.js ETL

**As a** maintainer  
**I want to** import and normalize movie data using a standalone Node.js ETL script  
**So that** the PostgreSQL movie catalogue can be prepared before application runtime  

**Acceptance Criteria:**
- the ETL runs as a standalone Node.js script
- the ETL reads a prepared local JSON dataset
- the ETL does not use Express
- the ETL does not use Python
- the ETL does not use live TMDB
- the ETL normalizes movie data before upsert

---

### US-8: Backend Uses Existing Neon MCP

**As a** system maintainer  
**I want to** use the existing Neon MCP service through a restricted backend MCP client boundary  
**So that** movie discovery queries are controlled, read-only, and aligned with project scope  

**Acceptance Criteria:**
- the backend connects to the existing Neon MCP service over Streamable HTTP
- Neon MCP access is configured as read-only and project-scoped
- MCP usage is limited to query/schema tools where possible
- runtime policy enforces movie-table-only query behavior

---

## 6. Testable Acceptance Criteria

### Criterion 1: Shared Conversations Are Persisted

**Given** an existing conversation  
**When** the application restarts  
**Then** the conversation and completed messages remain available from Neon PostgreSQL

---

### Criterion 2: Movies Are Persisted in PostgreSQL

**Given** imported movie data  
**When** the application handles movie tool requests  
**Then** movie search, details, and recommendation queries operate on persisted `Movie` records

---

### Criterion 3: ETL Imports Normalized Movie Data

**Given** a prepared local JSON movie dataset  
**When** the standalone Node.js ETL script is executed  
**Then** normalized movie records are upserted into Neon PostgreSQL

---

### Criterion 4: Shared Warning Is Visible

**Given** a user opens the UI  
**When** the chat page loads  
**Then** the page displays a visible warning that conversations are shared  
**And** sensitive information must not be entered

---

### Criterion 5: Completed JSON Responses Are Delivered

**Given** a valid chat request for a selected conversation  
**When** the backend processes the request  
**Then** the client receives one completed JSON response containing the assistant message
**And** the UI shows loading while processing

---

### Criterion 6: Latest 10 Messages Bound Model Context

**Given** a conversation with more than 10 persisted messages  
**When** a new chat request is processed  
**Then** only the latest 10 messages are used as model context

---

### Criterion 7: Assistant Message Persistence Occurs Only on Successful Completion

**Given** a completed JSON assistant response
**When** the response completes successfully  
**Then** the final assistant message is persisted  
**And when** the response fails before successful completion  
**Then** partial assistant output is not persisted

---

### Criterion 8: Model or Tool Failures Return JSON Errors

**Given** a chat request
**When** model or tool processing fails
**Then** the client receives a valid JSON error response

---

### Criterion 10: Existing Neon MCP Is Used Through a Restricted Client Boundary

**Given** the backend handles a movie-related request  
**When** the model invokes a movie-query tool  
**Then** the backend communicates with the existing Neon MCP service over Streamable HTTP  
**And** the MCP configuration remains read-only, project-scoped, and restricted to query/schema tools where possible

---

### Criterion 11: React Uses Only Backend API Base URL Configuration

**Given** the React UI runtime configuration  
**When** the UI is built and run  
**Then** it uses `VITE_API_BASE_URL` only  
**And** it does not include database or model credentials

---

## 7. Assumptions

1. **GenAI Model Availability:** One GenAI model provider will be available with credentials supplied via environment variables. No hardcoded API keys will be committed.

2. **Spring AI and MCP Compatibility:** Spring Boot 4.1, Spring AI 2.0, and Spring AI MCP compatibility must be verified during build setup.

3. **Neon PostgreSQL Availability:** Neon PostgreSQL is available for `Movie`, `Conversation`, and `Message` persistence.

4. **Boundary Ownership:** The main backend owns `Conversation` and `Message` persistence access. Movie data is accessed through the existing Neon MCP service as an external dependency.

5. **ETL Availability:** A standalone Node.js ETL process is available to prepare movie data before runtime use.

6. **No Schema Creation by Java:** Java applications validate existing Neon schema only and do not create, update, or migrate schema objects.

7. **Environment-Based Configuration:** Backend runtime configuration is injected through `application.properties`, `application-development.properties`, and environment variables.

8. **Frontend Configuration Isolation:** The React UI uses only `VITE_API_BASE_URL` and does not receive database or model credentials.

---

## 8. Constraints

### Technical Constraints

| Constraint | Rationale | Impact |
|---|---|---|
| **Neon PostgreSQL is the only database** | Single approved persistence store | `Movie`, `Conversation`, and `Message` persist in Neon only |
| **Main backend owns `Conversation` and `Message`** | Clear service ownership | Conversation/message persistence stays in the main backend |
| **Neon MCP read-only movie access** | Clear MCP boundary with managed external service | Movie lookup remains external to Java persistence logic |
| **Max 5 movies per tool** | Control token usage and response size | `search_movies` and `recommend_movies` must enforce limits |
| **Latest 10 messages only** | Bounded model context | Older persisted messages are excluded from prompt context |
| **No Java schema writes** | Preserve existing Neon schema as source of truth | Java only validates mappings and connectivity |
| **JPA validate mode** | Prevent accidental schema drift | Runtime uses `spring.jpa.hibernate.ddl-auto=validate` |
| **No secrets in Git** | Security hygiene | Credentials must come from external configuration |

### Scope Constraints

- NO authentication or user entity in V1
- NO Redis
- NO RAG, embeddings, or vector databases
- NO Express-based ETL
- NO Python-based ETL
- NO live TMDB integration
- NO Docker
- NO cloud deployment
- NO multiple GenAI model providers

---

## 9. Explicit Out-of-Scope Items

The following features are explicitly out of scope for V1:

### Authentication and Identity
- Spring Security integration
- user login / logout
- role-based access control
- user entity
- API key or token-based end-user authentication

### Advanced AI Features
- RAG
- vector embeddings
- semantic search
- fine-tuning of GenAI models
- multiple agents
- multiple model providers

### Data and Integration
- Redis
- live TMDB integration
- real-time movie database synchronization
- external review or ratings services

### ETL Alternatives
- Python-based ETL
- Express-based ETL services

### Infrastructure and Deployment
- Docker containerization
- Kubernetes orchestration
- cloud deployment
- load balancing or horizontal scaling

### End-User Features
- reviews
- ratings
- favorites
- watchlists
- personalization based on user identity

---

## 10. Known Open Questions

| Question | Impact | Resolution Path |
|---|---|---|
| **Which Gemini model variant will be used in V1?** | Affects latency, cost, and prompt tuning | Team decision in Story 1 with compatibility verification |
| **What is the exact normalized `Movie` schema?** | Affects ETL and query behavior | Finalize during design and persistence modeling |
| **What is the exact source JSON dataset shape for ETL?** | Affects import normalization | Define prepared local dataset contract |
| **What model timeout values are appropriate?** | Affects request latency and UX | Tune after provider selection and integration testing |
| **How will schema compatibility be validated against Neon?** | Affects startup checks and entity mapping confidence | Validate via JPA `ddl-auto=validate` and runtime connectivity checks |
| **What timeout values are appropriate for the chosen model provider?** | Affects runtime behavior and UX | Tune after provider selection and early integration testing |

---

## 11. Definition of Done

A story is complete only when:

1. Its acceptance criteria are satisfied.
2. The project compiles with `backend\mvnw.cmd clean package`.
3. Relevant tests pass with `backend\mvnw.cmd test`.
4. No unrelated changes appear in the diff.
5. Required documentation is updated.
6. Known limitations are reported.

---

## 12. Success Metrics

- Shared conversations and completed messages persist correctly in Neon PostgreSQL
- Movie tools operate on persisted `Movie` records
- Completed JSON chat responses are visible in the UI
- A loading indicator is visible while requests are pending
- Only the latest 10 messages are used as model context
- React uses only `VITE_API_BASE_URL`
- All JUnit 5 tests pass in build

---

## Appendix: Revision History

| Version | Date | Author | Change |
|---|---|---|---|
| 1.3 | July 2026 | Project Team | Revised architecture to use backend MCP client with existing read-only Neon MCP and runtime query guardrails |
| 1.2 | July 2026 | Project Team | Revised for database-first Neon source of truth and validation-only Java schema mapping |
| 1.1 | July 2026 | Project Team | Revised scope for Neon PostgreSQL, Flyway, shared persistence, Node.js ETL, and separate MCP ownership |
| 1.0 | July 2026 | Project Team | Initial requirements capture |
