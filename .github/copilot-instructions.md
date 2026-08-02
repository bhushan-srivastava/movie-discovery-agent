# Repository Instructions

## Project Objective

This repository contains a lightweight Movie Discovery Agent.

The application accepts natural-language movie requests, allows a
Generative AI model to select approved runtime MCP tools, and returns
conversational responses.

## Technology

- Java 21
- Spring Boot 4.1
- Spring AI 2.0
- Spring AI MCP
- Gemini API
- Maven Wrapper
- JUnit 5
- Neon PostgreSQL (existing source of truth)
- Existing Neon MCP server over Streamable HTTP
- NDJSON streaming over HTTP POST
- React + Ant Design frontend
- Node.js ETL for movie import

## Target Architecture

User request
→ REST controller
→ agent service
→ Spring AI ChatClient (Gemini)
→ Spring AI MCP client
→ existing Neon MCP (read-only, project-scoped, query/schema tools)
→ Neon PostgreSQL movie table

## Initial MCP Tools

- search_movies
- get_movie_details
- recommend_movies

## Coding Rules

- Use constructor injection.
- Keep controllers thin.
- Validate REST and MCP inputs.
- Return no more than five movies per tool call.
- Never commit API keys, passwords, tokens, or credentials.
- Read runtime credentials from environment variables.
- Keep Neon MCP configuration read-only and scoped to the approved Neon project.
- Restrict MCP usage to query and schema-inspection tools where possible.
- System prompt guardrails must enforce:
  - SELECT queries only
  - query only the `movie` table
  - maximum 5 rows
  - never query `conversation` or `message` tables
  - case-insensitive movie searches
  - use genres, ratings, popularity, language, runtime, director, and year for discovery/recommendations
  - never reveal SQL, schema metadata, credentials, or internal errors
- Use JPA only for `Conversation` and `Message` persistence in the backend.
- Java must never create, alter, or migrate database tables.
- Implement one approved story at a time.
- Add tests for every new behavior.
- Run relevant tests before claiming completion.

## Scope Restrictions

Do not add unless explicitly approved:

- Authentication
- RAG or embeddings
- Vector databases
- Redis
- Multiple agents
- Multiple model providers
- Live TMDB integration
- Docker
- Cloud deployment
- Reviews, ratings, favorites, or watchlists

## Commands

Build:

`backend\mvnw.cmd clean package`

Test:

`backend\mvnw.cmd test`

Run:

`backend\mvnw.cmd spring-boot:run`

## Definition of Done

A story is complete only when:

- Its acceptance criteria are satisfied.
- The project compiles.
- Relevant tests pass.
- No unrelated changes appear in the diff.
- Required documentation is updated.
- Known limitations are reported.

## Repository Layout Note

- The Spring Boot application lives in `backend/`.
- Use `backend/pom.xml`, `backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/`, and `backend/src/` when working on the backend application.
- The React client lives in `frontend/`.
- The Node.js ETL lives in `etl/`.
- `movie-mcp-server/` is not part of the required runtime architecture.
