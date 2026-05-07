-- Run this once to set up your PostgreSQL database
-- Requires PostgreSQL 14+ and pgvector extension

-- Create database
CREATE DATABASE knowledge_base;

-- Connect to knowledge_base, then run:

-- Enable pgvector extension (optional but recommended for production)
-- This enables true semantic vector similarity search
CREATE EXTENSION IF NOT EXISTS vector;

-- The main entries table
-- JPA will auto-create this, but here for reference / manual setup
CREATE TABLE IF NOT EXISTS knowledge_entries (
    id          BIGSERIAL PRIMARY KEY,
    title       TEXT NOT NULL,
    content     TEXT NOT NULL,
    source_type VARCHAR(20),
    source_url  TEXT,
    ai_summary  TEXT,
    embedding   TEXT,           -- comma-separated floats (basic) or use vector(1536) with pgvector
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Tags table
CREATE TABLE IF NOT EXISTS entry_tags (
    entry_id BIGINT REFERENCES knowledge_entries(id) ON DELETE CASCADE,
    tag      TEXT NOT NULL
);

-- Index for faster tag lookups
CREATE INDEX IF NOT EXISTS idx_entry_tags_tag ON entry_tags(tag);

-- Full text search index
CREATE INDEX IF NOT EXISTS idx_entries_fts
    ON knowledge_entries
    USING gin(to_tsvector('english', title || ' ' || content));

-- If using pgvector, add this index for fast similarity search:
-- CREATE INDEX IF NOT EXISTS idx_entries_embedding
--     ON knowledge_entries
--     USING ivfflat (embedding vector_cosine_ops)
--     WITH (lists = 100);
