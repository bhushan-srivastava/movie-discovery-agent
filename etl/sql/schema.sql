CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS movie (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_source_id VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    original_title VARCHAR(255),
    release_year INTEGER,
    genres TEXT[] NOT NULL,
    director VARCHAR(255),
    synopsis TEXT,
    runtime_minutes INTEGER,
    original_language VARCHAR(20),
    vote_average NUMERIC(3,1),
    vote_count INTEGER,
    popularity NUMERIC(12,4),
    poster_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT movie_runtime_positive CHECK (runtime_minutes IS NULL OR runtime_minutes > 0),
    CONSTRAINT movie_vote_average_valid CHECK (vote_average IS NULL OR vote_average BETWEEN 0 AND 10),
    CONSTRAINT movie_release_year_valid CHECK (release_year IS NULL OR release_year BETWEEN 1800 AND 2200)
);

CREATE INDEX IF NOT EXISTS idx_movie_title_lower ON movie (LOWER(title));
CREATE INDEX IF NOT EXISTS idx_movie_release_year ON movie (release_year);
CREATE INDEX IF NOT EXISTS idx_movie_genres ON movie USING GIN (genres);
CREATE INDEX IF NOT EXISTS idx_movie_original_language ON movie (original_language);
CREATE INDEX IF NOT EXISTS idx_movie_vote_average ON movie (vote_average DESC);

CREATE TABLE IF NOT EXISTS conversation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL DEFAULT 'New conversation',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS message (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    tool_name VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT message_role_valid CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM'))
);

CREATE INDEX IF NOT EXISTS idx_message_conversation_created
    ON message (conversation_id, created_at, id);
