# 🧠 Personal Knowledge Base

A RAG-based personal knowledge assistant built with Spring Boot and Anthropic's Claude API.
Ingest notes, articles, and PDFs — then ask questions about them in natural language.

## Features

- **Ask questions** — query all your saved content using natural language (RAG pattern)
- **Ingest notes** — paste book highlights, personal thoughts, anything
- **Scrape URLs** — paste an article link, it extracts and saves the content
- **Parse PDFs** — upload PDFs (research papers, books, documents)
- **AI summaries** — every entry gets an auto-generated summary on save
- **Weekly reflection** — Claude reads your week's entries and surfaces themes + insights

## Tech Stack

- **Java 17** + **Spring Boot 3.2**
- **PostgreSQL** with full-text search (optionally pgvector for semantic search)
- **Anthropic Claude API** (claude-sonnet-4)
- **jsoup** for web scraping
- **Apache PDFBox** for PDF parsing

## Architecture (RAG Pattern)

```
User Question
     │
     ▼
Full-text search on knowledge_entries
     │
     ▼
Top 5 relevant entries as context
     │
     ▼
Claude API (system prompt + context + question)
     │
     ▼
Answer grounded in YOUR knowledge base
```

## Setup

### 1. Prerequisites
- Java 17+
- PostgreSQL 14+
- Maven
- Anthropic API key → https://console.anthropic.com

### 2. Database

```sql
CREATE DATABASE knowledge_base;
```

Then run `src/main/resources/schema.sql` in your database.

### 3. Configuration

```bash
export ANTHROPIC_API_KEY=sk-ant-your-key-here
```

Update `src/main/resources/application.properties` with your PostgreSQL credentials.

### 4. Run

```bash
mvn spring-boot:run
```

Open `http://localhost:8080` in your browser.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/entries/note` | Add a text note |
| POST | `/api/entries/url` | Scrape and save a URL |
| POST | `/api/entries/pdf` | Upload and parse a PDF |
| POST | `/api/query` | Ask a question (RAG) |
| GET | `/api/reflect/weekly` | Get weekly AI reflection |
| GET | `/api/entries` | List all entries |
| DELETE | `/api/entries/{id}` | Delete an entry |

## Upgrading to Semantic Search (pgvector)

For proper vector similarity search instead of full-text search:

1. Install pgvector: `CREATE EXTENSION vector;`
2. Add embedding generation using OpenAI's `text-embedding-3-small` or a free model
3. Store embeddings as `vector(1536)` column
4. Use cosine similarity query (commented in `KnowledgeEntryRepository.java`)

